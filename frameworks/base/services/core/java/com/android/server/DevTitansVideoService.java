package com.android.server;

import android.util.Slog;
import android.os.RemoteException;
import android.os.IDevTitansVideoService;

public class DevTitansVideoService extends IDevTitansVideoService.Stub {
	private static final String TAG = "DevTitansVideoService";
	private boolean isVideoPlaying = false;

	public DevTitansVideoService() {
		Slog.d(TAG, "DevTitansVideoService starting.");
	}

	public boolean isVideoPlaying() throws RemoteException {
		return this.isVideoPlaying();
	}

	public void setVideoPlaying(boolean isVideoPlaying) throws RemoteException {
		this.isVideoPlaying = isVideoPlaying;
	}
}
