package android.app.devtitans;
/**
 * System private API for communicating with the DevTitans Service.
 * {@hide}
 */
interface IDevTitansServiceManager {
    boolean isVideoPlaying();
    void setVideoPlaying(boolean isVideoPlaying);
}