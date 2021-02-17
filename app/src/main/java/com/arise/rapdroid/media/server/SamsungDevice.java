package com.arise.rapdroid.media.server;

import android.content.Context;

import com.arise.core.tools.Mole;
import com.samsung.multiscreen.Device;
import com.samsung.multiscreen.Error;
import com.samsung.multiscreen.Result;
import com.samsung.multiscreen.Search;
import com.samsung.multiscreen.Service;

public class SamsungDevice {
    private final Context context;
    Mole log = Mole.getInstance(SamsungDevice.class);

    public SamsungDevice(Context context){

        this.context = context;
    }
    public void discover(){
        try {
            Search search = Service.search(context);
            search.setOnServiceFoundListener(new Search.OnServiceFoundListener() {
                @Override
                public void onFound(Service service) {
                    log.info("FOUND SERVIC.E" + service.getName());
//                    ApplicationInfo applicationInfo = service.get

                    service.getDeviceInfo(new Result<Device>() {
                        @Override
                        public void onSuccess(Device device) {
                            System.out.println(device.getPlatform());

//                            device.getApp


                        }

                        @Override
                        public void onError(Error error) {

                        }
                    });
                    service.getUri(); // root api uri
//                    service.
                }
            });

            search.setOnStopListener(new Search.OnStopListener() {
                @Override
                public void onStop() {
                    log.info("SEARCH STOP");
                }
            });

            search.start(true);

        }catch (Throwable t){
            log.error(t);
        }
    }
}
