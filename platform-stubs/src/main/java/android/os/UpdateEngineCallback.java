package android.os;

/** Compile-only stub of the {@code @SystemApi} class of the same name. */
public abstract class UpdateEngineCallback {
    public abstract void onStatusUpdate(int status, float percent);

    public abstract void onPayloadApplicationComplete(int errorCode);
}
