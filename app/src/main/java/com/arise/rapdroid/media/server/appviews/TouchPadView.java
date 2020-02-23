package com.arise.rapdroid.media.server.appviews;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.arise.astox.net.clients.HttpClient;
import com.arise.astox.net.models.http.HttpRequest;
import com.arise.core.tools.CollectionUtil;
import com.arise.core.tools.Mole;
import com.arise.core.tools.ThreadUtil;
import com.arise.core.tools.Util;
import com.arise.rapdroid.media.server.R;
import com.arise.rapdroid.media.server.WelandClient;
import com.arise.weland.dto.RemoteConnection;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.Random;

public class TouchPadView extends FrameLayout {

    private static final Mole log = Mole.getInstance(TouchPadView.class);
    ImageButton lockBtn;
    RemoteConnection connection;
    LinearLayout top;
    DrawingView drawingView;

    private String [][] colorsHex = new String[][]{
            //reds "ff0000",
            {"ff3700", "ff8000", "ffae00", "9b84c4", "cf80a1", "ba2362", "e09b46"},

            //yellows "ffff00",
            { "ffd900", "91ff00", "00ff99", "cf80cf", "a3ba23", "d8e65e", "a8e046"},

            //greens "00ff00",
            { "6f8771", "b5cfa9", "52b027", "5ee672", "5c9675", "1b5075", "73d98c"},
    };

    Random random = new Random();
    int start = random.nextInt(colorsHex.length - 1);

    private int [][] colors;

    private int nextColor(){
        if (start > (colors.length - 1)){
            start = 0;
        }
        int val = colors[start][ random.nextInt(colors[start].length - 1) ];
        start++;
        return val;
    }


    public TouchPadView(@NonNull Context context) {
        super(context);

        colors = new int[colorsHex.length][colorsHex[0].length];
        for (int i = 0; i < colorsHex.length; i++){
            for (int j = 0; j < colorsHex[i].length; j++){
                colors[i][j] = Color.parseColor("#" + colorsHex[i][j]);
            }
        }

        top = new LinearLayout(getContext());
        top.setOrientation(LinearLayout.HORIZONTAL);

        drawingView = new DrawingView(getContext());
        lockBtn = new ImageButton(getContext());
        lockBtn.setImageResource(R.drawable.ic_lock);
        lockBtn.setBackgroundResource(R.drawable.button_style_blue);
        top.addView(lockBtn);
        drawingView.setPaintColor(nextColor());

        ImageButton clickBtn = new ImageButton(getContext());
        clickBtn.setImageResource(R.drawable.ic_cursor_click);
        clickBtn.setBackgroundResource(R.drawable.button_style_blue);
        clickBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (communicator != null){
                    communicator.sendClick();
                }
            }
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 20;
        top.addView(clickBtn, params);

        addView(drawingView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(top, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }




    public TouchPadView onButtonClick(OnClickListener onClickListener){
        lockBtn.setOnClickListener(onClickListener);
        return this;
    }

    NetworkCommunicator communicator;
    public void setConnection(RemoteConnection connection) {
        this.connection = connection;
        communicator = new NetworkCommunicator(connection.getPayload());
        drawingView.setHandler(communicator);
    }

    public void unLockedIcon() {
        if (lockBtn != null){
            lockBtn.setImageResource(R.drawable.ic_unlock);
        }
    }


    class NetworkCommunicator implements  DrawingHandler, Runnable {

        private final Object payload;
        BluetoothSocket bluetoothSocket;
        Socket httpSocket;
        Thread asyncThread = null;
        String state = "unset";
        float px, py;
        float startX = 0;
        float startY = 0;

        public NetworkCommunicator(Object payload) {
            this.payload = payload;
        }

        void startThread(){
            if (asyncThread == null){
                asyncThread = new Thread(this);
                asyncThread.start();
            }
        }

        void stopThread(){
            write("EOF".getBytes());
            if (httpSocket != null){
                Util.close(httpSocket);
                httpSocket = null;
            }
            asyncThread.interrupt();
            asyncThread = null;
        }

        @Override
        public void onActionDown(DrawingView drawingView, MotionEvent event) {
            startThread();
            state = "action_down";
            startX =  event.getX();
            startY =  event.getY();
        }

        @Override
        public void onMove(DrawingView drawingView, MotionEvent event) {
            if (!"clicked".equals(state)){
                px = startX - event.getX();
                py = startY - event.getY();
                state = "move";
            }
        }

        @Override
        public void onActionUp(DrawingView drawingView, MotionEvent event) {
            state = "action_up";
            startX = 0;
            startY = 0;
            drawingView.setPaintColor(nextColor());
        }

        @Override
        public void onConstruct() {
            startThread();
        }

        private void send(float dx, float dy) {
            write((">x=" + dx + ";y=" + dy + ";").getBytes());
        }

        private void write(byte [] bytes) {
            if (bluetoothSocket != null){
                try {
                    bluetoothSocket.getOutputStream().write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    Util.close(bluetoothSocket);
                }
            }
            if (httpSocket != null){
                try {
                    httpSocket.getOutputStream().write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void connect() {
            HttpRequest request = new HttpRequest().setMethod("GET").setUri(">w=0");
            if (payload instanceof BluetoothDevice && bluetoothSocket == null){
                try {
                    bluetoothSocket = (BluetoothSocket) WelandClient.getWorker(connection).getConnection(request);
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO treat error
                }

            }
            else if (payload instanceof URI){
                URI uri = (URI) payload;
                HttpClient client = new HttpClient().setHost(uri.getHost()).setPort(uri.getPort());
                try {
                    httpSocket = client.getConnection(request);
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO treat error
                }
            }
        }

        @Override
        public void run() {
            connect(); //connect async
            while (true){
                if ("move".equals(state) ){
                    send(px, py);
                }
                else if ("action_up".equals(state)){
                    stopThread();
                    break;
                }
            }
        }

        public void sendClick() {
            ThreadUtil.fireAndForget(new Runnable() {
                @Override
                public void run() {
                    connect();
                    write((">K=1;").getBytes());
                    write("EOF".getBytes());
                }
            });
        }
    }





}
