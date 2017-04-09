package hu.rics.timelapse;

import android.Manifest;
import android.app.Activity;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import hu.rics.camera1util.CameraPreview;
import hu.rics.camera1util.MediaRecorderWrapper;
import hu.rics.permissionhandler.PermissionHandler;

import static android.os.Environment.getExternalStoragePublicDirectory;


/**
 * Based on https://developer.android.com/guide/topics/media/camera.html
 *
 * @author rics
 */
public class CameraActivity extends Activity {

    static final String TAG = "CameraActivity";
    private MediaRecorderWrapper mediaRecorderWrapper;
    Button captureButton;
    final public static double DEFAULT_FRAME_RATE = 1;
    private double frameRate;
    File outputMediaFile;
    TextView maxFrameSecTextView;
    EditText actualFrameSecEditText;
    PermissionHandler permissionHandler;
    String permissions[] = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_camera);

        permissionHandler = new PermissionHandler(this);
        permissionHandler.requestPermission(permissions);

        // Add a listener to the Capture button
        captureButton = (Button) findViewById(R.id.button_capture);
        maxFrameSecTextView = (TextView) findViewById(R.id.max2FrameSecTextView);
        //maxFrameSecTextView.setText(Integer.toString(getMaxCaptureRate(mCamera)));
        actualFrameSecEditText = (EditText) findViewById(R.id.actualFrameSecEditText);
        actualFrameSecEditText.setText(Double.toString(DEFAULT_FRAME_RATE));
    }

    @Override
    public void onResume() {
        super.onResume();

        if( permissionHandler.hasRights() ) {
            initMediaRecorderWrapperIfNull();
            mediaRecorderWrapper.startPreview();
        }
        outputMediaFile = getOutputMediaFile();
        final EditText mediaFileNameEditText = (EditText) findViewById(R.id.mediaFileNameEditText);
        mediaFileNameEditText.setText(outputMediaFile.toString());

        captureButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if( permissionHandler.hasRights() ) {

                            if (mediaRecorderWrapper.isRecording()) {
                                mediaRecorderWrapper.stopRecording();  // stop the recording

                                // inform the user that recording has stopped
                                captureButton.setText("Start");
                                outputMediaFile = getOutputMediaFile();
                                mediaFileNameEditText.setText(outputMediaFile.toString());
                            } else {
                                try {
                                    frameRate = Double.valueOf(actualFrameSecEditText.getText().toString());
                                    mediaRecorderWrapper.setFrameRateIfPossible(frameRate);
                                    mediaRecorderWrapper.startRecording(mediaFileNameEditText.getText().toString());
                                    if (mediaRecorderWrapper.isRecording()) {
                                        captureButton.setText("Stop");
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Invalid frame/sec.");
                                    Toast toast = Toast.makeText(getApplicationContext(), "Invalid frame/sec.", Toast.LENGTH_SHORT);
                                    toast.show();
                                    return;
                                }
                            }
                        }
                    }
                });

    }

    private File getOutputMediaFile() {

        File mediaStorageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");

        return mediaFile;
    }

    private int getMaxCaptureRate(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int fpsRange[] = new int[2];

        parameters.getPreviewFpsRange(fpsRange);
        return fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]/1000;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if( permissionHandler.hasRights() && mediaRecorderWrapper != null ) {
            mediaRecorderWrapper.stopPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        permissionHandler.onRequestPermissionsResult( requestCode, permissions, grantResults);
        Log.i(TAG,"permissionHandler.hasRights():" + permissionHandler.hasRights());

        if( permissionHandler.hasRights() ) {
            initMediaRecorderWrapperIfNull();
        }
    }
    private void initMediaRecorderWrapperIfNull() {
        if( mediaRecorderWrapper == null ) {
            mediaRecorderWrapper = new MediaRecorderWrapper(this, R.id.camera_preview);
            mediaRecorderWrapper.setTimelapse(true);
        }
    }
}
