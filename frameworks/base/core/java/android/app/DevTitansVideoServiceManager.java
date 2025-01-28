package android.app;

import android.annotation.SystemService;
import android.content.Context;
import android.os.IDevTitansVideoService;

@SystemService(Context.DEVTITANS_VIDEO_SERVICE)
public class DevTitansVideoServiceManager {
    private static final String TAG = "DevTitansVideoServiceManager";
    private IDevTitansVideoService mService;

    public DevTitansVideoServiceManager(Context context, IDevTitansVideoService service) {
        mService = service;
    }

    public boolean isVideoPlaying() {
        return mService.isVideoPlaying();
    }

    public void setVideoPlaying(boolean isVideoPlaying) {
        mService.setVideoPlaying(isVideoPlaying);
    }
}