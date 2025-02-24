package android.app.devtitans;

import android.os.RemoteException;
import android.annotation.SystemService;
import android.content.Context;
import android.util.Log;
import android.annotation.Nullable;

/** @hide */
@SystemService(Context.DEVTITANS_SERVICE)
public final class DevTitansServiceManager {

    private static final String TAG = "DevTitansServiceManager";
    private final IDevTitansServiceManager mService;
    private Context mContext;

    /** @hide */
    public DevTitansServiceManager(Context context, IDevTitansServiceManager service) {
        mContext = context;
        mService = service;
    }

    /** @hide */
    public boolean isVideoPlaying() {
        try {
            return mService.isVideoPlaying();
        } catch (RemoteException ex) {
            Log.e(TAG, "Unable to get video status math service");
            return false;

        }
    }

    /** @hide */
    public void setVideoPlaying(boolean isVideoPlaying) {
        try {
            mService.setVideoPlaying(isVideoPlaying);
        } catch (RemoteException ex) {
            Log.e(TAG, "Unable to set video status math service");
        }
    }
}