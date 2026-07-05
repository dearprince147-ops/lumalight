package com.example.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.R;

/**
 * Single shared configure screen for both the Flashlight and Strobe widgets.
 * The system launches this automatically (via the android:configure attribute
 * in each widget's provider XML) the moment the user drops the widget onto
 * their home screen, before it's actually placed. Backing out leaves the
 * widget un-placed; tapping "Add Widget" saves the color and completes
 * placement.
 */
public class WidgetConfigureActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private View configureSwatch;
    private SeekBar hueSlider, brightnessSlider;
    private TextView tvHue, tvBrightness;
    private LinearLayout presetContainer;

    private float hue = 0f;
    private int brightness = 100;
    private int currentColor = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the user backs out without confirming, the widget host should not
        // place the widget at all - this is the standard Android contract for
        // widget configure activities.
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            appWidgetId = intent.getExtras().getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setContentView(R.layout.activity_widget_configure);

        configureSwatch = findViewById(R.id.configureSwatch);
        hueSlider = findViewById(R.id.configureHueSlider);
        brightnessSlider = findViewById(R.id.configureBrightnessSlider);
        tvHue = findViewById(R.id.tvConfigureHue);
        tvBrightness = findViewById(R.id.tvConfigureBrightness);
        presetContainer = findViewById(R.id.configurePresetContainer);

        setupSliderTracks();
        setupPresets();
        setupListeners();
        applyColor(currentColor);

        findViewById(R.id.btnAddWidget).setOnClickListener(v -> confirmAndFinish());
    }

    private void setupSliderTracks() {
        float density = getResources().getDisplayMetrics().density;
        float radius = 8 * density;

        ShapeDrawable.ShaderFactory hueShader = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, height / 2f, width, height / 2f,
                        new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED},
                        null, Shader.TileMode.CLAMP);
            }
        };
        hueSlider.setProgressDrawable(buildTrack(hueShader, radius, density));

        ShapeDrawable.ShaderFactory brightnessShader = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(0, height / 2f, width, height / 2f,
                        Color.BLACK, Color.WHITE, Shader.TileMode.CLAMP);
            }
        };
        brightnessSlider.setProgressDrawable(buildTrack(brightnessShader, radius, density));
    }

    private Drawable buildTrack(ShapeDrawable.ShaderFactory factory, float radius, float density) {
        ShapeDrawable shape = new ShapeDrawable(new RoundRectShape(
                new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
        shape.setShaderFactory(factory);
        shape.setIntrinsicHeight((int) (12 * density));

        LayerDrawable ld = new LayerDrawable(new Drawable[]{shape, new ColorDrawable(Color.TRANSPARENT)});
        ld.setId(0, android.R.id.background);
        ld.setId(1, android.R.id.progress);
        return ld;
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

        for (int color : presets) {
            View presetView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(0, 0, marginPx, 0);
            presetView.setLayoutParams(params);

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            presetView.setBackground(circle);

            presetView.setOnClickListener(v -> applyColor(color));
            presetContainer.addView(presetView);
        }
    }

    private void setupListeners() {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (seekBar == hueSlider) {
                    applyHueBrightness(progress, brightness);
                } else {
                    applyHueBrightness(hue, progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        hueSlider.setOnSeekBarChangeListener(listener);
        brightnessSlider.setOnSeekBarChangeListener(listener);
    }

    private void applyHueBrightness(float newHue, int newBrightness) {
        float[] hsv = {newHue, 1.0f, newBrightness / 100f};
        applyColor(Color.HSVToColor(hsv));
    }

    private void applyColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        currentColor = color;
        hue = hsv[0];
        brightness = Math.round(hsv[2] * 100);

        configureSwatch.setBackgroundColor(color);
        hueSlider.setProgress((int) hsv[0]);
        brightnessSlider.setProgress(brightness);
        tvHue.setText((int) hsv[0] + "°");
        tvBrightness.setText(brightness + "%");
    }

    private void confirmAndFinish() {
        WidgetColorPrefs.setColor(this, appWidgetId, currentColor);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = appWidgetManager.getAppWidgetInfo(appWidgetId) != null
                ? appWidgetManager.getAppWidgetInfo(appWidgetId).provider
                : null;

        if (provider != null && provider.getClassName().equals(StrobeWidgetProvider.class.getName())) {
            StrobeWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId);
        } else {
            FlashlightWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
