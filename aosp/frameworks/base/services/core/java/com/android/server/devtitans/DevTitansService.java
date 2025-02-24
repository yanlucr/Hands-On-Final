package com.android.server.devtitans;

import android.app.devtitans.IDevTitansServiceManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.content.Context;
import com.android.server.SystemService;

public class DevTitansService extends IDevTitansServiceManager.Stub {
    private final static String LOG_TAG = "DevTitansService";
    private final Context mContext;

    private boolean isVideoPlaying = false;

    public DevTitansService(Context context) {
        mContext = context;
    }

    public boolean isVideoPlaying() {
        return this.isVideoPlaying;
    }
    public void setVideoPlaying(boolean isVideoPlaying) {
        this.isVideoPlaying = isVideoPlaying;
    }
}