package hu.rics.timelapse;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;


/**
 *
 * @author rics
 */
/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Context context;
    private SurfaceHolder mHolder;
    static final int CAMERA_ID = 0;
    private Camera mCamera;
    private static final String TAG = "CameraPreview";
    private boolean previewIsRunning;


    public CameraPreview(Context context) {
        super(context);
        this.context = context;

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
            setCameraDisplayOrientation(context,CAMERA_ID,mCamera);
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

    // correct displayorientation for Ace 3 and SMT800 tab in all four direction
    // taken from here: http://stackoverflow.com/a/10218309/21047
    public static void setCameraDisplayOrientation(Context context,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        Log.i(CameraActivity.TAG,"degrees:" + degrees + ":");
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}
