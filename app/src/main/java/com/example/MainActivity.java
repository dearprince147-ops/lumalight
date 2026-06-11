package com.example;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LumaLight";
    private static final String PREF_COLOR = "pref_color";
    private static final int PERMISSION_REQUEST_CAMERA = 1001;

    // UI
    private View colorSwatch;
    private LinearLayout presetContainer;
    private SeekBar hueSlider;
    private RadioGroup modeGroup;
    private LinearLayout strobeContainer;
    private SeekBar strobeSlider;
    private SwitchMaterial ledToggle;
    private Button activateButton;

    private FrameLayout activeOverlay;
    private View activeFlashlightColor;
    private TextView activeHintText;

    // State
    private int currentColor = Color.WHITE;
    private int currentModeId = R.id.modeSolid;
    private boolean isActive = false;
    private int strobeFrequency = 10; // 1 to 20 Hz
    private boolean useCameraLed = false;

    // Hardware
    private PowerManager.WakeLock wakeLock;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean hasCameraFlash;
    private boolean isTorchOn = false;

    // Timing & Animation
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ValueAnimator pulseAnimator;
    private boolean lightState = false; // For strobe/sos toggle

    // SOS Timing arrays (in milliseconds)
    // 1 time unit = 150ms for brisk SOS
    // Dot = 1 unit, Dash = 3 units, Pause between elements = 1 unit
    // Pause between letters = 3 units, Pause between SOS loops = 7 units
    // Total pattern elements: S (dot, pause, dot, pause, dot, letter_pause) ->
    // O (dash, pause, dash, pause, dash, letter_pause) ->
    // S (dot, pause, dot, pause, dot, word_pause)
    private static final int SOS_UNIT = 200;
    private final int[] sosPattern = {
            SOS_UNIT, SOS_UNIT, SOS_UNIT, SOS_UNIT, SOS_UNIT, 3 * SOS_UNIT, // S
            3 * SOS_UNIT, SOS_UNIT, 3 * SOS_UNIT, SOS_UNIT, 3 * SOS_UNIT, 3 * SOS_UNIT, // O
            SOS_UNIT, SOS_UNIT, SOS_UNIT, SOS_UNIT, SOS_UNIT, 7 * SOS_UNIT // S
    };
    // True/False representing light on/off for each duration in the sosPattern array.
    // The pattern represents durations of ON and OFF alternately starting with ON.
    private int sosIndex = 0;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        currentColor = prefs.getInt(PREF_COLOR, Color.WHITE);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        }

        initCamera();
        bindViews();
        setupListeners();
        setupPresets();
        updateColorState(currentColor);
    }

    private void initCamera() {
        hasCameraFlash = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (hasCameraFlash) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String[] cameraIds = cameraManager.getCameraIdList();
                for (String id : cameraIds) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (flashAvailable != null && flashAvailable && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id;
                        break;
                    }
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "Camera init failed", e);
            }
        }
    }

    private void bindViews() {
        colorSwatch = findViewById(R.id.colorSwatch);
        presetContainer = findViewById(R.id.presetContainer);
        hueSlider = findViewById(R.id.hueSlider);
        modeGroup = findViewById(R.id.modeGroup);
        strobeContainer = findViewById(R.id.strobeContainer);
        strobeSlider = findViewById(R.id.strobeSlider);
        ledToggle = findViewById(R.id.ledToggle);
        activateButton = findViewById(R.id.activateButton);

        activeOverlay = findViewById(R.id.activeOverlay);
        activeFlashlightColor = findViewById(R.id.activeFlashlightColor);
        activeHintText = findViewById(R.id.activeHintText);

        if (!hasCameraFlash || cameraId == null) {
            ledToggle.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        hueSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float[] hsv = {progress, 1.0f, 1.0f};
                    updateColorState(Color.HSVToColor(hsv));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            currentModeId = checkedId;
            strobeContainer.setVisibility(checkedId == R.id.modeStrobe ? View.VISIBLE : View.GONE);
        });

        strobeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                strobeFrequency = progress + 1; // 1 to 20
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ledToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ledToggle.setChecked(false);
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else {
                useCameraLed = isChecked;
            }
        });

        activateButton.setOnClickListener(v -> toggleFlashlight());

        activeOverlay.setOnClickListener(v -> {
            if (isActive) toggleFlashlight();
        });
    }

    private void setupPresets() {
        int[] presets = {
                Color.WHITE,
                Color.parseColor("#FFF4E5"), // Warm White
                Color.RED,
                Color.GREEN,
                Color.BLUE,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA,
                Color.parseColor("#FFA500")  // Orange
        };

        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (48 * density);
        int marginPx = (int) (12 * density);

        for (int color : presets) {
            View presetView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(0, 0, marginPx, 0);
            presetView.setLayoutParams(params);
            presetView.setBackgroundColor(color);
            presetView.setElevation(4f * density);
            presetView.setOnClickListener(v -> updateColorState(color));
            presetContainer.addView(presetView);
        }
    }

    private void updateColorState(int color) {
        currentColor = color;
        colorSwatch.setBackgroundColor(color);
        activeFlashlightColor.setBackgroundColor(color);

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hueSlider.setProgress((int) hsv[0]);

        getPreferences(MODE_PRIVATE).edit().putInt(PREF_COLOR, currentColor).apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ledToggle.setChecked(true);
                useCameraLed = true;
            } else {
                useCameraLed = false;
                Toast.makeText(this, "Camera permission required for LED torch", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleFlashlight() {
        isActive = !isActive;
        if (isActive) {
            startFlashlight();
        } else {
            stopFlashlight();
        }
    }

    private void startFlashlight() {
        activeOverlay.setVisibility(View.VISIBLE);
        activeFlashlightColor.setAlpha(1.0f);
        activeHintText.setVisibility(View.VISIBLE);
        activeHintText.setAlpha(1.0f);
        
        // Fade out hint after 2 seconds
        handler.postDelayed(() -> {
            activeHintText.animate().alpha(0.0f).setDuration(500).withEndAction(() -> activeHintText.setVisibility(View.GONE)).start();
        }, 2000);

        // Hide System UI
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Max screen brightness
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(layoutParams);

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        // Apply mode
        sosIndex = 0;
        lightState = true;
        
        if (currentModeId == R.id.modeSolid) {
            setBothLights(true);
        } else if (currentModeId == R.id.modeStrobe) {
            handler.post(strobeRunnable);
        } else if (currentModeId == R.id.modeSos) {
            handler.post(sosRunnable);
        } else if (currentModeId == R.id.modePulse) {
            setLedTorch(useCameraLed); // LED stays on for pulse
            pulseAnimator = ValueAnimator.ofFloat(1.0f, 0.1f);
            pulseAnimator.setDuration(1000);
            pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnimator.addUpdateListener(animation -> {
                float alpha = (float) animation.getAnimatedValue();
                activeFlashlightColor.setAlpha(alpha);
            });
            pulseAnimator.start();
        }
    }

    private void stopFlashlight() {
        activeOverlay.setVisibility(View.GONE);
        
        // Restore System UI
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        // Restore screen brightness
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(layoutParams);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        handler.removeCallbacks(strobeRunnable);
        handler.removeCallbacks(sosRunnable);
        
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }

        setBothLights(false);
    }

    private final Runnable strobeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActive) return;
            setBothLights(lightState);
            lightState = !lightState;
            int delay = 1000 / (strobeFrequency * 2);
            handler.postDelayed(this, delay);
        }
    };

    private final Runnable sosRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActive) return;
            // State: Even indices mean ON, odd indices mean OFF.
            boolean isOn = (sosIndex % 2 == 0);
            setBothLights(isOn);
            
            int delay = sosPattern[sosIndex];
            sosIndex = (sosIndex + 1) % sosPattern.length;
            
            handler.postDelayed(this, delay);
        }
    };

    private void setBothLights(boolean state) {
        activeFlashlightColor.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
        if (useCameraLed) {
            setLedTorch(state);
        }
    }

    private void setLedTorch(boolean state) {
        if (cameraManager != null && cameraId != null && isTorchOn != state) {
            try {
                cameraManager.setTorchMode(cameraId, state);
                isTorchOn = state;
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to set torch mode", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isActive) {
            stopFlashlight();
            // Automatically reset toggle state to keep UI in sync
            isActive = false;
        }
    }
}
