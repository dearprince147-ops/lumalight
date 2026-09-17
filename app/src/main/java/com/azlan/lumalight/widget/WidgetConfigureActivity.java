package com.azlan.lumalight.widget;

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

import com.azlan.lumalight.R;

/** Configures one or two colors depending on the widget being placed. */
public class WidgetConfigureActivity extends AppCompatActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private View configureSwatch, configureSwatch2;
    private SeekBar hueSlider, brightnessSlider, hueSlider2, brightnessSlider2;
    private TextView tvHue, tvBrightness, tvHue2, tvBrightness2;
    private LinearLayout presetContainer, presetContainer2, color2Section;
    private float hue = 0f, hue2 = 0f;
    private int brightness = 100, brightness2 = 0;
    private int currentColor = Color.WHITE, currentColor2 = Color.BLACK;
    private boolean twoColors;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        setContentView(R.layout.activity_widget_configure);
        configureSwatch = findViewById(R.id.configureSwatch);
        configureSwatch2 = findViewById(R.id.configureSwatch2);
        hueSlider = findViewById(R.id.configureHueSlider);
        brightnessSlider = findViewById(R.id.configureBrightnessSlider);
        hueSlider2 = findViewById(R.id.configureHueSlider2);
        brightnessSlider2 = findViewById(R.id.configureBrightnessSlider2);
        tvHue = findViewById(R.id.tvConfigureHue);
        tvBrightness = findViewById(R.id.tvConfigureBrightness);
        tvHue2 = findViewById(R.id.tvConfigureHue2);
        tvBrightness2 = findViewById(R.id.tvConfigureBrightness2);
        presetContainer = findViewById(R.id.configurePresetContainer);
        presetContainer2 = findViewById(R.id.configurePresetContainer2);
        color2Section = findViewById(R.id.configureColor2Section);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName provider = manager.getAppWidgetInfo(appWidgetId).provider;
        twoColors = provider != null && provider.getClassName().equals(StrobeWidgetProvider.class.getName());
        color2Section.setVisibility(twoColors ? View.VISIBLE : View.GONE);
        setupSliderTracks(); setupPresets(); setupListeners();
        applyColor(1, currentColor); applyColor(2, currentColor2);
        findViewById(R.id.btnAddWidget).setOnClickListener(v -> confirmAndFinish());
    }

    private Drawable track(ShapeDrawable.ShaderFactory factory, float radius, float density) {
        ShapeDrawable shape = new ShapeDrawable(new RoundRectShape(
                new float[]{radius,radius,radius,radius,radius,radius,radius,radius}, null, null));
        shape.setShaderFactory(factory); shape.setIntrinsicHeight((int)(12 * density));
        LayerDrawable layer = new LayerDrawable(new Drawable[]{shape, new ColorDrawable(Color.TRANSPARENT)});
        layer.setId(0, android.R.id.background); layer.setId(1, android.R.id.progress); return layer;
    }

    private void setupSliderTracks() {
        float d = getResources().getDisplayMetrics().density, r = 8 * d;
        ShapeDrawable.ShaderFactory hueTrack = (w,h) -> new LinearGradient(0,h/2f,w,h/2f,
                new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED},null,Shader.TileMode.CLAMP);
        ShapeDrawable.ShaderFactory brightnessTrack = (w,h) -> new LinearGradient(0,h/2f,w,h/2f,Color.BLACK,Color.WHITE,Shader.TileMode.CLAMP);
        for (SeekBar s : new SeekBar[]{hueSlider,hueSlider2}) s.setProgressDrawable(track(hueTrack,r,d));
        for (SeekBar s : new SeekBar[]{brightnessSlider,brightnessSlider2}) s.setProgressDrawable(track(brightnessTrack,r,d));
    }

    private void setupPresets() {
        int[] colors={Color.WHITE,Color.parseColor("#FFF4E5"),Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW,Color.CYAN,Color.MAGENTA,Color.parseColor("#FFA500")};
        addPresets(presetContainer, colors, 1); addPresets(presetContainer2, colors, 2);
    }
    private void addPresets(LinearLayout container, int[] colors, int slot) {
        float d=getResources().getDisplayMetrics().density; int size=(int)(48*d), margin=(int)(12*d);
        for (int color:colors) { View v=new View(this); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(size,size); p.setMargins(0,0,margin,0); v.setLayoutParams(p); GradientDrawable bg=new GradientDrawable(); bg.setShape(GradientDrawable.OVAL); bg.setColor(color); v.setBackground(bg); v.setOnClickListener(x->applyColor(slot,color)); container.addView(v); }
    }
    private void setupListeners() {
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean user){if(!user)return; if(s==hueSlider) applyHueBrightness(1,p,brightness); else if(s==brightnessSlider) applyHueBrightness(1,hue,p); else if(s==hueSlider2) applyHueBrightness(2,p,brightness2); else applyHueBrightness(2,hue2,p);}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        };
        hueSlider.setOnSeekBarChangeListener(listener); brightnessSlider.setOnSeekBarChangeListener(listener); hueSlider2.setOnSeekBarChangeListener(listener); brightnessSlider2.setOnSeekBarChangeListener(listener);
    }
    private void applyHueBrightness(int slot,float h,int b){applyColor(slot,Color.HSVToColor(new float[]{h,1f,b/100f}));}
    private void applyColor(int slot,int color){float[] hsv=new float[3];Color.colorToHSV(color,hsv); if(slot==1){currentColor=color;hue=hsv[0];brightness=Math.round(hsv[2]*100);configureSwatch.setBackgroundColor(color);hueSlider.setProgress((int)hue);brightnessSlider.setProgress(brightness);tvHue.setText((int)hue+"°");tvBrightness.setText(brightness+"%");}else{currentColor2=color;hue2=hsv[0];brightness2=Math.round(hsv[2]*100);configureSwatch2.setBackgroundColor(color);hueSlider2.setProgress((int)hue2);brightnessSlider2.setProgress(brightness2);tvHue2.setText((int)hue2+"°");tvBrightness2.setText(brightness2+"%");}}
    private void confirmAndFinish(){WidgetColorPrefs.setColor1(this,appWidgetId,currentColor);if(twoColors)WidgetColorPrefs.setColor2(this,appWidgetId,currentColor2);AppWidgetManager m=AppWidgetManager.getInstance(this);if(twoColors)StrobeWidgetProvider.updateWidget(this,m,appWidgetId);else FlashlightWidgetProvider.updateWidget(this,m,appWidgetId);Intent result=new Intent();result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId);setResult(RESULT_OK,result);finish();}
}
