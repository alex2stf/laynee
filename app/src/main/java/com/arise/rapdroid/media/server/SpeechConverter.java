package com.arise.rapdroid.media.server;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.arise.core.tools.Mole;
import com.arise.core.tools.models.CompleteHandler;
import com.arise.weland.dto.ContentInfo;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechConverter {
    SpeechRecognizer speechRecognizer;
    final Intent speechRecognizerIntent;


    CompleteHandler<ArrayList<String>> dataFound;


    private static final Mole log = Mole.getInstance(SpeechConverter.class);

    public SpeechConverter setDataFoundListener(CompleteHandler<ArrayList<String>> onComplete) {
        this.dataFound = onComplete;
        return this;
    }

    public SpeechConverter(Context context){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);




        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                System.out.println("on begin");
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                System.out.println("received buffer");
            }

            @Override
            public void onEndOfSpeech() {
                System.out.println("end of speech");
            }

            @Override
            public void onError(int i) {


                String message;
                switch (i) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "Client side error";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "Insufficient permissions";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "No match";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "RecognitionService busy";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "error from server";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "No speech input";
                        break;
                    default:
                        message = "Didn't understand, please try again.";
                        break;
                }

                log.error("speech converter err " + message);
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                dataFound.onComplete(data);
//                for (String s: data){
//                    ContentInfo contentInfo =(AppUtil.contentInfoProvider.searchByKeyword())
//                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                System.out.println(bundle);
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000);

    }

    public void destroy(){
        speechRecognizer.destroy();
    }

    boolean listening = false;
//    boolean manuallyStarted = false;

    public void startListen(){
        speechRecognizer.startListening(speechRecognizerIntent);
        listening = true;
//        manuallyStarted = true;
    }


    public void stopListen(){
        speechRecognizer.stopListening();
        listening = false;
//        manuallyStarted = false;
    }

    public boolean isListening() {
        return listening;
    }
}
