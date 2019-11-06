package heliox.com.perido;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidadvance.topsnackbar.TSnackbar;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class HomeActivity extends AppCompatActivity implements SensorEventListener {

    public static final String PREFS = "PeridoPrefs";
    private static final String TAG = "Perido";
    private static final int SENSOR_SENSITIVITY = 4;
    private static final String DEFAULT_DIRECTORY = "Perido";
    private static final int BACK_PRESS_INTERVAL = 2000;

    private static File imageRoot;
    private static Camera globalCamera;

    private ImageView mImageView;
    private TextView mOutput;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private long mBackPressed;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mOutput = findViewById(R.id.output);
        mImageView = findViewById(R.id.image_view);
        sharedPrefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        checkAndSetSensor();
        registerSettingsBtnListener();
    }

    private void registerSettingsBtnListener() {
        ImageView mSettingsBtn = findViewById(R.id.settingsBtn);
        mSettingsBtn.setOnClickListener(l -> {
            Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            HomeActivity.this.overridePendingTransition(R.anim.animation_enter, R.anim.animation_leave);
            startActivity(settingsIntent);
        });
    }

    private void checkAndSetSensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor != null) {
            mProximity = proximitySensor;
        }
    }

    private void checkPrefs() {
        boolean isTesting = sharedPrefs.getBoolean(getString(R.string.settings_testing), false);

        String modeText = (isTesting) ? getString(R.string.ui_testing) : getString(R.string.ui_prod);
        mOutput.setText(modeText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPrefs();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                try {
                    playStartSFX();
                    initCamera();

                    globalCamera.takePicture(null, null, jpegCallback);
                    playEndSFX();
                    Log.i(TAG, "Picture taken");

                } catch (Exception e) {
                    Log.i(TAG, "Error on Start: " + e.getMessage());
                    showErrorSnackbar();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void initCamera() {
        try {
            Log.i(TAG, "Camera opened");
            try {
                globalCamera = Camera.open();

                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                globalCamera.setPreviewTexture(surfaceTexture);
                globalCamera.setDisplayOrientation(90);
                globalCamera.startPreview();

                Log.i(TAG, "Picture preview");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                savePicture(data);
                Log.i(TAG, "Picture saved");
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
            } finally {
                globalCamera.stopPreview();
            }
        }
    };

    private void savePicture(byte[] data) {
        Log.d(TAG, "Saving picture...");

        String fileName = "Testing_" + UUID.randomUUID().toString().substring(0, 5) + ".jpg";
        checkDirectory();

        File pictureFile = new File(imageRoot, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();

            showTopSnackbar("New Image saved: " + fileName, Color.WHITE, Color.BLACK, TSnackbar.LENGTH_SHORT);
            loadIntoView(pictureFile);

        } catch (Exception error) {
            Log.d("File not saved: ", error.getMessage());
            showErrorSnackbar();
        }
    }

    public void checkDirectory() {

        boolean isDefaultFolder = sharedPrefs.getBoolean(getString(R.string.settings_folder_default), true);

        if (isDefaultFolder) {
            imageRoot = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), DEFAULT_DIRECTORY);
        } else {
            imageRoot = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES).toString());
        }

        try {
            if (!imageRoot.exists()) {
                imageRoot.mkdirs();
                Log.i(TAG, "Dir created");
            } else {
                Log.i(TAG, "Dir already exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadIntoView(File pictureFile) {
        Glide.with(this)
                .load(pictureFile)
                .apply(new RequestOptions()
                        .override(1920, 1080)
                        .fitCenter()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .transform(new RotateTransformation(this, 90f)))
                .into(mImageView);

        Log.i(TAG, "File: " + pictureFile.getAbsolutePath());
    }

    private void playStartSFX() {
        MediaPlayer sfx = MediaPlayer.create(HomeActivity.this, R.raw.recording_start);
        sfx.start();
    }

    private void playEndSFX() {
        MediaPlayer sfx = MediaPlayer.create(HomeActivity.this, R.raw.recording_end);
        sfx.start();
    }

    public void showErrorSnackbar() {
        showTopSnackbar("Please try again.", Color.WHITE, Color.RED, TSnackbar.LENGTH_LONG);
    }

    public void showTopSnackbar(String text, int text_color, int bg_color, Integer duration) {
        TSnackbar snackbar = TSnackbar.make(findViewById(android.R.id.content), text, duration);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(bg_color);
        TextView textView = snackbarView.findViewById(com.androidadvance.topsnackbar.R.id.snackbar_text);
        textView.setTextColor(text_color);
        snackbar.show();
    }

    @Override
    public void onBackPressed() {
        if (mBackPressed + BACK_PRESS_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), "Tap again to exit", Toast.LENGTH_SHORT).show();
        }

        mBackPressed = System.currentTimeMillis();
    }
}
