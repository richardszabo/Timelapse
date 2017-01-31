package hu.rics.timelapse;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
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

import static hu.rics.timelapse.R.id.actualFrameSecEditText;
import static hu.rics.timelapse.R.id.mediaFileNameEditText;


/**
 * Based on https://developer.android.com/guide/topics/media/camera.html
 *
 * @author rics
 */
public class CameraActivity extends Activity {

    static final String TAG = "CameraActivity";
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;
    Button captureButton;
    final public static double DEFAULT_FRAME_RATE = 1;
    private double frameRate;
    File outputMediaFile;
    TextView maxFrameSecTextView;
    EditText actualFrameSecEditText;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(CameraActivity.TAG,"oncreate---------------------");
        setContentView(R.layout.activity_camera);
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
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

        mCamera = Camera.open(); // attempt to get a Camera instance
        mPreview.setCamera(mCamera);
        mPreview.myStartPreview();
        Log.d(CameraActivity.TAG,"onresume---------------------" + mCamera);
        outputMediaFile = getOutputMediaFile();
        final EditText mediaFileNameEditText = (EditText) findViewById(R.id.mediaFileNameEditText);
        mediaFileNameEditText.setText(outputMediaFile.toString());

        captureButton.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if (isRecording) {
                            // stop recording and release camera
                            mMediaRecorder.stop();  // stop the recording
                            releaseMediaRecorder(); // release the MediaRecorder object
                            mCamera.lock();         // take camera access back from MediaRecorder

                            // inform the user that recording has stopped
                            captureButton.setText("Start");
                            outputMediaFile = getOutputMediaFile();
                            mediaFileNameEditText.setText(outputMediaFile.toString());
                            isRecording = false;
                        } else {
                            try {
                                frameRate = Double.valueOf(actualFrameSecEditText.getText().toString());
                            } catch(NumberFormatException e) {
                                Log.e(TAG, "Invalid frame/sec.");
                                Toast toast = Toast.makeText(getApplicationContext(), "Invalid frame/sec.", Toast.LENGTH_SHORT);
                                toast.show();
                                return;
                            }
                            // initialize video camera
                            if (prepareVideoRecorder(frameRate)) {
                                // Camera is available and unlocked, MediaRecorder is prepared,
                                // now you can start recording
                                mMediaRecorder.start();

                                // inform the user that recording has started
                                captureButton.setText("Stop");
                                isRecording = true;
                            } else {
                                // prepare didn't work, release the camera
                                releaseMediaRecorder();
                                // inform user
                            }
                        }
                    }
                });

    }

    private File getOutputMediaFile() {

        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
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

    private boolean prepareVideoRecorder(double frameRate) {

        mMediaRecorder = new MediaRecorder();

        Log.d(TAG, "prep1:" + mCamera +":");
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_720P );
        //http://stackoverflow.com/a/16543157/21047
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(profile.videoFrameWidth,profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        
        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        Log.d(TAG, "prep2");
        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        // Step 4: Set output file
        mMediaRecorder.setOutputFile(outputMediaFile.toString());
        //mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        Log.d(TAG, "prep5");
        
        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        int fpsRange[] = new int[2];

        parameters.getPreviewFpsRange(fpsRange);
        for(int i = 0; i < fpsRange.length; ++i) {
            Log.d(TAG, "getPreviewFpsRange(" + i + "):" + fpsRange[i]);
        }

        mMediaRecorder.setCaptureRate(frameRate);

        Log.d(TAG, "prep6");
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        Log.d(TAG, "prep7");
        return true;
    }

    protected String profileToString(CamcorderProfile cp) {
        return "audioBitRate:" + cp.audioBitRate + ":" +
                "audioChannels:" +cp.audioChannels + ":" +
                "audioCodec:" + cp.audioCodec + ":" +
                "audioSampleRate:" + cp.audioSampleRate + ":" +
                "duration:" + cp.duration + ":" +
                "fileFormat:" + cp.fileFormat + ":" +
                "quality:" + cp.quality + ":" +
                "videoBitRate:" + cp.videoBitRate + ":" +
                "videoCodec:" + cp.videoCodec + ":" +
                "videoFrameHeight:" + cp.videoFrameHeight + ":" +
                "videoFrameRate:" + cp.videoFrameRate + ":" +
                "videoFrameWidth:" + cp.videoFrameWidth +":";
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.myStopPreview();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }
}
