package sop.augmentedandroids;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.yahoo.mobile.client.android.util.rangeseekbar.RangeSeekBar;

public class SettingsActivity extends AppCompatActivity{

    private static final String TAG = "SOP::SettingsActivity";

    // User inputs are strings because the input is given through EditText
    private String frameSkip = "5";
    private String numberOfDilations = "1";
    private String minContourArea = "500";
    private String sideRatioLimit = "1.45";
    private int saturation;
    private int value;
    private double minHue;
    private double maxHue;

    private TextView saturationTextView;
    private TextView valueTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("Settings activity", "Settings activity created.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        initTextViews();
        initSeekbars();
    }

    public void onApplySettings(View view) {
        /* Function gets called when Apply-button is pressed and gets values of the fields below
        *  and passes them to MainActivity via created intent. */
        Intent intent = new Intent();

        EditText frameSkipEditText = (EditText) findViewById(R.id.edit_text_frameskip);
        frameSkip = frameSkipEditText.getText().toString();
        EditText dilationsEditText = (EditText) findViewById(R.id.edit_text_dilations);
        numberOfDilations = dilationsEditText.getText().toString();
        EditText minContourAreaEditText = (EditText) findViewById(R.id.edit_text_min_contour_area);
        minContourArea = minContourAreaEditText.getText().toString();
        EditText sideRatioLimitEditText = (EditText) findViewById(R.id.edit_text_side_ratio_limit);
        sideRatioLimit= sideRatioLimitEditText.getText().toString();

        if (!frameSkip.isEmpty()) {
            if (Integer.parseInt(frameSkip) >= 0) {
                intent.putExtra("frameskip", Integer.parseInt(frameSkip));
            }
        }
        if (!numberOfDilations.isEmpty()){
            if (Integer.parseInt(numberOfDilations) >= 0) {
                intent.putExtra("number_of_dilations", Integer.parseInt(numberOfDilations));
            }
        }
        if (!minContourArea.isEmpty()) {
            if (Double.parseDouble(minContourArea) >= 0) {
                intent.putExtra("minContourArea", Double.parseDouble(minContourArea));
            }
        }
        if (!sideRatioLimit.isEmpty()) {
            if (Double.parseDouble(sideRatioLimit) >= 0) {
                intent.putExtra("sideRatioLimit", Double.parseDouble(sideRatioLimit));
            }
        }
        if (minHue >= 0 && minHue < maxHue) {
            intent.putExtra("minHue", minHue);
        }
        if (maxHue >= 0 && maxHue > minHue) {
            intent.putExtra("maxHue", maxHue);
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initSeekbars() {
        RangeSeekBar<Double> hueControl = (RangeSeekBar<Double>)findViewById(R.id.hue_bar);
        SeekBar saturationControl = (SeekBar) findViewById(R.id.saturation_bar);
        SeekBar valueControl = (SeekBar) findViewById(R.id.value_bar);
        try {
            // Note: setting values for hueControl in settings.xml resulted in type error
            hueControl.setRangeValues(0.0, 179.0);
            minHue = getIntent().getDoubleExtra("minHue");
            maxHue = getIntent().getDoubleExtra("maxHue");
            hueControl.setSelectedMinValue(minHue);
            hueControl.setSelectedMaxValue(maxHue);
            hueControl.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Double>() {
                @Override
                public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Double minValue, Double maxValue) {
                    minHue = minValue;
                    maxHue = maxValue;
                }
            });

            // Listener to receive changes to the saturation-progress level
            saturationControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar arg0) {}
                public void onStartTrackingTouch(SeekBar arg0) {}
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    saturation = progress;
                    saturationTextView.setText(Integer.toString(saturation));
                }
            });

            // Listener to receive changes to the value-progress level
            valueControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar arg0) {}
                public void onStartTrackingTouch(SeekBar arg0) {}
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    value = progress;
                    valueTextView.setText(Integer.toString(value));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTextViews(){

        // initialize TextViews
        TextView frameSkipTextView = (EditText) findViewById(R.id.edit_text_frameskip);
        TextView dilationsTextView = (EditText) findViewById(R.id.edit_text_dilations);
        TextView minContourAreaTextView = (EditText) findViewById(R.id.edit_text_min_contour_area);
        TextView sideRatioLimitTextView = (EditText) findViewById(R.id.edit_text_side_ratio_limit);

        // Saturation and valueTextView need to be visible to OnSeekBarChangeListener
        saturationTextView = (TextView) findViewById(R.id.saturation_value);
        valueTextView = (TextView) findViewById(R.id.value_value);

        // Get values from MainActivity through Intent
        frameSkip = getIntent().getStringExtra("currentFrameSkip");
        numberOfDilations = getIntent().getStringExtra("currentNumberOfDilations");
        minContourArea = getIntent().getStringExtra("currentMinContourArea");
        sideRatioLimit = getIntent().getStringExtra("currentSideRatioLimit");

        // Set values to TextViews
        frameSkipTextView.setText(frameSkip);
        dilationsTextView.setText(numberOfDilations);
        minContourAreaTextView.setText(minContourArea);
        sideRatioLimitTextView.setText(sideRatioLimit);
        saturationTextView.setText("0");
        valueTextView.setText("0");
    }
}
