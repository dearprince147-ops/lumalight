package com.example;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
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
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
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
    private TextView lblColor1, tvHex1, tvHex2, tvHz, tvHue1, tvHue2;
    private TextView tvBrightness1, tvBrightness2;
    private View colorSwatch1, colorSwatch2;
    private LinearLayout presetContainer1, presetContainer2;
    private SeekBar hueSlider1, hueSlider2;
    private SeekBar brightnessSlider1, brightnessSlider2;
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
    // Hue (0-360) and brightness/value (0-100) are tracked separately from the
    // resulting RGB color so a hue can be "remembered" even while brightness is
    // dialed all the way down to black (where hue becomes visually meaningless).
    private float hue1 = 0f, hue2 = 0f;
    private int brightness1 = 100, brightness2 = 0;
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
        tvHex1 = findViewById(R.id.tvHex1);
        tvHex2 = findViewById(R.id.tvHex2);
        tvHz = findViewById(R.id.tvHz);
        tvHue1 = findViewById(R.id.tvHue1);
        tvHue2 = findViewById(R.id.tvHue2);

        colorSwatch1 = findViewById(R.id.colorSwatch1);
        presetContainer1 = findViewById(R.id.presetContainer1);
        hueSlider1 = findViewById(R.id.hueSlider1);
        brightnessSlider1 = findViewById(R.id.brightnessSlider1);
        tvBrightness1 = findViewById(R.id.tvBrightness1);

        strobeContainer = findViewById(R.id.strobeContainer);
        strobeSlider = findViewById(R.id.strobeSlider);

        colorSwatch2 = findViewById(R.id.colorSwatch2);
        presetContainer2 = findViewById(R.id.presetContainer2);
        hueSlider2 = findViewById(R.id.hueSlider2);
        brightnessSlider2 = findViewById(R.id.brightnessSlider2);
        tvBrightness2 = findViewById(R.id.tvBrightness2);

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

        // Brightness/value track: a static black -> white gradient. This is what
        // makes "black" (and every shade in between) reachable, since the hue
        // slider alone can only ever produce fully-saturated, fully-bright hues.
        ShapeDrawable.ShaderFactory brightnessShaderFactory = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, height / 2f, width, height / 2f,
                        Color.BLACK, Color.WHITE, Shader.TileMode.CLAMP);
            }
        };
        for (SeekBar slider : new SeekBar[]{brightnessSlider1, brightnessSlider2}) {
            ShapeDrawable shape = new ShapeDrawable(new RoundRectShape(
                    new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
            shape.setShaderFactory(brightnessShaderFactory);
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
                    if (seekBar == hueSlider1) {
                        applyHueBrightness(1, progress, brightness1);
                    } else {
                        applyHueBrightness(2, progress, brightness2);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        hueSlider1.setOnSeekBarChangeListener(hueSelectListener);
        hueSlider2.setOnSeekBarChangeListener(hueSelectListener);

        SeekBar.OnSeekBarChangeListener brightnessSelectListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (seekBar == brightnessSlider1) {
                        applyHueBrightness(1, hue1, progress);
                    } else {
                        applyHueBrightness(2, hue2, progress);
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        brightnessSlider1.setOnSeekBarChangeListener(brightnessSelectListener);
        brightnessSlider2.setOnSeekBarChangeListener(brightnessSelectListener);

        View.OnClickListener tabListener = v -> {
            if (isStrobeMode == (v == tabStrobe)) return;
            isStrobeMode = (v == tabStrobe);

            TransitionManager.beginDelayedTransition((android.view.ViewGroup) findViewById(R.id.scrollView), new AutoTransition().setDuration(200));

            tabSolid.setBackgroundResource(isStrobeMode ? 0 : R.drawable.bg_pill_thumb_selected);
            tabSolid.setTextColor(isStrobeMode ? Color.parseColor("#888888") : Color.WHITE);

            tabStrobe.setBackgroundResource(isStrobeMode ? R.drawable.bg_pill_thumb_selected : 0);
            tabStrobe.setTextColor(isStrobeMode ? Color.WHITE : Color.parseColor("#888888"));

            strobeContainer.setVisibility(isStrobeMode ? View.VISIBLE : View.GONE);
            lblColor1.setVisibility(isStrobeMode ? View.VISIBLE : View.GONE);
        };

        View.OnTouchListener pressAnimListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(100).start();
                    break;
            }
            return false;
        };

        tabSolid.setOnTouchListener(pressAnimListener);
        tabStrobe.setOnTouchListener(pressAnimListener);
        tabSolid.setOnClickListener(tabListener);
        tabStrobe.setOnClickListener(tabListener);

        strobeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                strobeFrequency = progress + 1; // 1 to 20
                if (tvHz != null) tvHz.setText(strobeFrequency + " Hz");
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

        activateButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.8f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(180)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                    v.performClick();
                    break;
            }
            return true;
        });

        activateButton.setOnClickListener(v -> toggleFlashlight());
    }

    private void setupPresets() {
        int[] presets = {
                Color.WHITE, Color.parseColor("#FFF4E5"), Color.RED,
                Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN, Color.MAGENTA, Color.parseColor("#FFA500")
        };

        float density = getResources().getDisplayMetrics().density;
        int paddingPx = (int) (16 * density);
        int marginPx = (int) (12 * density);

        for (int i = 1; i <= 2; i++) {
            LinearLayout container = i == 1 ? presetContainer1 : presetContainer2;
            final int index = i;
            int sizePx = (int) ((i == 1 ? 48 : 32) * density);

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
                presetView.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).start();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.animate().scaleX(1f).scaleY(1f).setDuration(180)
                                    .setInterpolator(new OvershootInterpolator())
                                    .start();
                            // allow click to handle logic
                            break;
                    }
                    return false; // let onClick trigger
                });
                container.addView(presetView);
            }
        }
    }

    /**
     * Builds a color from a hue (0-360) and brightness/value (0-100) at full
     * saturation and routes it through updateColorState. At brightness 0 this
     * always resolves to black, regardless of hue - this is what makes black
     * (and every shade down to it) reachable from the UI.
     */
    private void applyHueBrightness(int index, float hue, int brightnessPercent) {
        float[] hsv = {hue, 1.0f, brightnessPercent / 100f};
        int color = Color.HSVToColor(hsv);
        updateColorState(index, color);
    }

    private void updateColorState(int index, int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        int brightnessPercent = Math.round(hsv[2] * 100);
        float density = getResources().getDisplayMetrics().density;

        if (index == 1) {
            int previousColor = currentColor1;
            currentColor1 = color;
            hue1 = hsv[0];
            brightness1 = brightnessPercent;
            animateSwatchColor(colorSwatch1, previousColor, color);
            hueSlider1.setProgress((int) hsv[0]);
            brightnessSlider1.setProgress(brightnessPercent);
            if (tvHue1 != null) tvHue1.setText((int)hsv[0] + "°");
            if (tvBrightness1 != null) tvBrightness1.setText(brightnessPercent + "%");
            if (tvHex1 != null) tvHex1.setText(String.format("#%06X", (0xFFFFFF & color)));
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_COLOR_1, currentColor1).apply();

            for (int i = 0; i < presetContainer1.getChildCount(); i++) {
                View child = presetContainer1.getChildAt(i);
                GradientDrawable bg = (GradientDrawable) child.getBackground();
                int childColor = (int) child.getTag();
                bg.setStroke((int)(2 * density), color == childColor ? Color.WHITE : Color.TRANSPARENT);
            }
        } else {
            int previousColor = currentColor2;
            currentColor2 = color;
            hue2 = hsv[0];
            brightness2 = brightnessPercent;
            animateSwatchColor(colorSwatch2, previousColor, color);
            hueSlider2.setProgress((int) hsv[0]);
            brightnessSlider2.setProgress(brightnessPercent);
            if (tvHue2 != null) tvHue2.setText((int)hsv[0] + "°");
            if (tvBrightness2 != null) tvBrightness2.setText(brightnessPercent + "%");
            if (tvHex2 != null) tvHex2.setText(String.format("#%06X", (0xFFFFFF & color)));
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_COLOR_2, currentColor2).apply();

            for (int i = 0; i < presetContainer2.getChildCount(); i++) {
                View child = presetContainer2.getChildAt(i);
                GradientDrawable bg = (GradientDrawable) child.getBackground();
                int childColor = (int) child.getTag();
                bg.setStroke((int)(2 * density), color == childColor ? Color.WHITE : Color.TRANSPARENT);
            }
        }
    }

    /** Smoothly crossfades a swatch's background color instead of snapping to it. */
    private void animateSwatchColor(View swatch, int fromColor, int toColor) {
        if (fromColor == toColor) {
            swatch.setBackgroundColor(toColor);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        animator.setDuration(150);
        animator.addUpdateListener(a -> swatch.setBackgroundColor((int) a.getAnimatedValue()));
        animator.start();
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
        activeOverlay.setAlpha(0f);
        activeOverlay.setVisibility(View.VISIBLE);
        activeOverlay.animate().alpha(1f).setDuration(150).start();
        int initialColor = currentColor1;
        activeFlashlightColor.setBackgroundColor(initialColor);
        activateButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#93000A")));
        activateButton.setTextColor(Color.parseColor("#FFDAD6"));
        activateButton.setText("STOP");

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
            activeFlashlightColor.setLayerType(View.LAYER_TYPE_NONE, null);
            activeFlashlightColor.setVisibility(View.VISIBLE);
            if (useCameraLed) setLedTorch(true);
        } else {
            // Hardware layer speeds up compositing since the strobe swaps this
            // view's background color many times per second.
            activeFlashlightColor.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            handler.post(strobeRunnable);
        }
    }

    private void stopFlashlight() {
        activeOverlay.animate().alpha(0f).setDuration(120)
                .withEndAction(() -> activeOverlay.setVisibility(View.GONE))
                .start();
        activeFlashlightColor.setLayerType(View.LAYER_TYPE_NONE, null);
        activateButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#00FF00")));
        activateButton.setTextColor(Color.parseColor("#002200"));
        activateButton.setText("ACTIVATE");

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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isActive && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            toggleFlashlight();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
