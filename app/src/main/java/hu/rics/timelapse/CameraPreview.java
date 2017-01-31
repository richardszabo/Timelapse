package hu.rics.timelapse;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


/**
 *
 * @author rics
 */
/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final String TAG = "CameraPreview";
    private boolean previewIsRunning;


    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera c) {
        mCamera = c;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            Log.d(CameraActivity.TAG,"surfacecreated---------------------");
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(CameraActivity.TAG,"surfacedestroyed---------------------");
        myStopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(CameraActivity.TAG,"surfacechanged---------------------");
        myStartPreview();
    }

    // safe call to start the preview
    // if this is called in onResume, the surface might not have been created yet
    // so check that the camera has been set up too.
    public void myStartPreview() {
        if (!previewIsRunning && (mCamera != null)) {
            mCamera.startPreview();
            previewIsRunning = true;
        }
    }

    // same for stopping the preview
    public void myStopPreview() {
        if (previewIsRunning && (mCamera != null)) {
            mCamera.stopPreview();
            previewIsRunning = false;
        }
    }
}
