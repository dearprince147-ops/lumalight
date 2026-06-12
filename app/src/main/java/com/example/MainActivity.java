package com.example;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
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
    private static final String PREF_COLOR_1 = "pref_color_1";
    private static final String PREF_COLOR_2 = "pref_color_2";
    private static final int PERMISSION_REQUEST_CAMERA = 1001;

    // UI
    private TextView tabSolid, tabStrobe;
    private TextView lblColor1;
    private View colorSwatch1, colorSwatch2;
    private LinearLayout presetContainer1, presetContainer2;
    private SeekBar hueSlider1, hueSlider2;
    private LinearLayout strobeContainer;
    private SeekBar strobeSlider;
    private SwitchMaterial ledToggle;
    private Button activateButton;

    private FrameLayout activeOverlay;
    private View activeFlashlightColor;
    private TextView activeHintText;

    // State
    private int currentColor1 = Color.WHITE;
    private int currentColor2 = Color.BLACK;
    private boolean isStrobeMode = false;
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
    private boolean isColor1Turn = true;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        currentColor1 = prefs.getInt(PREF_COLOR_1, Color.WHITE);
        currentColor2 = prefs.getInt(PREF_COLOR_2, Color.BLACK);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        }

        initCamera();
        bindViews();
        setupListeners();
        setupSliders();
        setupPresets();
        updateColorState(1, currentColor1);
        updateColorState(2, currentColor2);
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
        tabSolid = findViewById(R.id.tabSolid);
        tabStrobe = findViewById(R.id.tabStrobe);
        lblColor1 = findViewById(R.id.lblColor1);

        colorSwatch1 = findViewById(R.id.colorSwatch1);
        presetContainer1 = findViewById(R.id.presetContainer1);
        hueSlider1 = findViewById(R.id.hueSlider1);

        strobeContainer = findViewById(R.id.strobeContainer);
        strobeSlider = findViewById(R.id.strobeSlider);

        colorSwatch2 = findViewById(R.id.colorSwatch2);
        presetContainer2 = findViewById(R.id.presetContainer2);
        hueSlider2 = findViewById(R.id.hueSlider2);

        ledToggle = findViewById(R.id.ledToggle);
        activateButton = findViewById(R.id.activateButton);

        activeOverlay = findViewById(R.id.activeOverlay);
        activeFlashlightColor = findViewById(R.id.activeFlashlightColor);
        activeHintText = findViewById(R.id.activeHintText);

        if (!hasCameraFlash || cameraId == null) {
            ledToggle.setVisibility(View.GONE);
        }
    }

    private void setupSliders() {
        ShapeDrawable.ShaderFactory shaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, height / 2f, width, height / 2f,
                        new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED},
                        null, Shader.TileMode.CLAMP);
            }
        };

        float density = getResources().getDisplayMetrics().density;
        float radius = 8 * density;

        for (SeekBar slider : new SeekBar[]{hueSlider1, hueSlider2}) {
            ShapeDrawable shape = new ShapeDrawable(new RoundRectShape(
                    new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
            shape.setShaderFactory(shaderFactory);
            shape.setIntrinsicHeight((int)(12 * density));

            LayerDrawable ld = new LayerDrawable(new Drawable[]{shape, new ColorDrawable(Color.TRANSPARENT)});
            ld.setId(0, android.R.id.background);
            ld.setId(1, android.R.id.progress);
            slider.setProgressDrawable(ld);
        }
    }

    private void setupListeners() {
        SeekBar.OnSeekBarChangeListener hueSelectListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float[] hsv = {progress, 1.0f, 1.0f};
                    int color = Color.HSVToColor(hsv);
                    if (seekBar == hueSlider1) updateColorState(1, color);
                    else updateColorState(2, color);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        hueSlider1.setOnSeekBarChangeListener(hueSelectListener);
        hueSlider2.setOnSeekBarChangeListener(hueSelectListener);

        View.OnClickListener tabListener = v -> {
            isStrobeMode = (v == tabStrobe);

            tabSolid.setBackgroundResource(isStrobeMode ? 0 : R.drawable.bg_pill_thumb_selected);
            tabSolid.setTextColor(isStrobeMode ? Color.parseColor("#888888") : Color.WHITE);

            tabStrobe.setBackgroundResource(isStrobeMode ? R.drawable.bg_pill_thumb_selected : 0);
            tabStrobe.setTextColor(isStrobeMode ? Color.WHITE : Color.parseColor("#888888"));

            strobeContainer.setVisibility(isStrobeMode ? View.VISIBLE : View.GONE);
            lblColor1.setVisibility(isStrobeMode ? View.VISIBLE : View.GONE);
        };

        tabSolid.setOnClickListener(tabListener);
        tabStrobe.setOnClickListener(tabListener);

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
                Color.WHITE, Color.parseColor("#FFF4E5"), Color.RED,
                Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN, Color.MAGENTA, Color.parseColor("#FFA500")
        };

        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (48 * density);
        int marginPx = (int) (12 * density);

        for (int i = 1; i <= 2; i++) {
            LinearLayout container = i == 1 ? presetContainer1 : presetContainer2;
            final int index = i;

            for (int color : presets) {
                View presetView = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
                params.setMargins(0, 0, marginPx, 0);
                presetView.setLayoutParams(params);

                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(color);
                presetView.setBackground(circle);
                presetView.setTag(color);

                presetView.setOnClickListener(v -> updateColorState(index, color));
                container.addView(presetView);
            }
        }
    }

    private void updateColorState(int index, int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        float density = getResources().getDisplayMetrics().density;

        if (index == 1) {
            currentColor1 = color;
            colorSwatch1.setBackgroundColor(color);
            hueSlider1.setProgress((int) hsv[0]);
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_COLOR_1, currentColor1).apply();

            for (int i = 0; i < presetContainer1.getChildCount(); i++) {
                View child = presetContainer1.getChildAt(i);
                GradientDrawable bg = (GradientDrawable) child.getBackground();
                int childColor = (int) child.getTag();
                bg.setStroke(color == childColor ? (int)(3 * density) : 0, Color.WHITE);
            }
        } else {
            currentColor2 = color;
            colorSwatch2.setBackgroundColor(color);
            hueSlider2.setProgress((int) hsv[0]);
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_COLOR_2, currentColor2).apply();

            for (int i = 0; i < presetContainer2.getChildCount(); i++) {
                View child = presetContainer2.getChildAt(i);
                GradientDrawable bg = (GradientDrawable) child.getBackground();
                int childColor = (int) child.getTag();
                bg.setStroke(color == childColor ? (int)(3 * density) : 0, Color.WHITE);
            }
        }
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
        int initialColor = currentColor1;
        activeFlashlightColor.setBackgroundColor(initialColor);
        activateButton.setTextColor(initialColor);

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
        isColor1Turn = true;

        if (!isStrobeMode) {
            activeFlashlightColor.setVisibility(View.VISIBLE);
            if (useCameraLed) setLedTorch(true);
        } else {
            handler.post(strobeRunnable);
        }
    }

    private void stopFlashlight() {
        activeOverlay.setVisibility(View.GONE);
        activateButton.setTextColor(Color.parseColor("#0D0D0D"));

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
        if (useCameraLed) setLedTorch(false);
    }

    private final Runnable strobeRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActive) return;

            int colorToShow = isColor1Turn ? currentColor1 : currentColor2;
            activeFlashlightColor.setBackgroundColor(colorToShow);
            activeFlashlightColor.setVisibility(View.VISIBLE);

            if (useCameraLed) {
                setLedTorch(isColor1Turn);
            }

            isColor1Turn = !isColor1Turn;
            int delay = 1000 / (strobeFrequency * 2);
            handler.postDelayed(this, delay);
        }
    };

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
