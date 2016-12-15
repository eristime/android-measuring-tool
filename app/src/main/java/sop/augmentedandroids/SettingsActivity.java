package sop.augmentedandroids;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SettingsActivity extends AppCompatActivity{

    private static final String TAG = "SOP::SettingsActivity";
    private String frameSkip = "5";
    private String numberOfDilations = "1";
    private int hue;
    private int saturation;
    private int value;

    private TextView frameSkipTextView = null;
    private TextView dilationsTextView = null;
    private TextView hueTextView = null;
    private TextView saturationTextView = null;
    private TextView valueTextView = null;
    private Button back = null;
    private SeekBar hueControl = null;
    private SeekBar saturationControl = null;
    private SeekBar valueControl = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Settings activity", "Settings activity created.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        frameSkipTextView = (EditText) findViewById(R.id.edit_text_frameskip);
        dilationsTextView = (EditText) findViewById(R.id.edit_text_dilations);
        hueTextView = (TextView) findViewById(R.id.hue_value);
        saturationTextView = (TextView) findViewById(R.id.saturation_value);
        valueTextView = (TextView) findViewById(R.id.value_value);

        // Get values from MainActivity through Intent
        frameSkip = getIntent().getStringExtra("currentFrameSkip");
        Log.d(TAG, "currentFrameSkip " + frameSkip);
        numberOfDilations = getIntent().getStringExtra("currentNumberOfDilations");
        Log.d(TAG, "currentNumberOfDilations " + numberOfDilations);

        // Set values to TextViews
        frameSkipTextView.setText(frameSkip);
        dilationsTextView.setText(numberOfDilations);
        hueTextView.setText("0°");
        saturationTextView.setText("0%");
        valueTextView.setText("0%");

        back = (Button)findViewById(R.id.back);

        initSeekbars();

    }

    public void onApplySettings(View view) {
        Intent intent = new Intent();
        EditText frameSkipEditText = (EditText) findViewById(R.id.edit_text_frameskip);

        frameSkip = frameSkipEditText.getText().toString();
        EditText dilationsEditText = (EditText) findViewById(R.id.edit_text_dilations);
        numberOfDilations = dilationsEditText.getText().toString();
        if (!frameSkip.isEmpty()) {
            if (Integer.parseInt(frameSkip) >= 0) {
                intent.putExtra("frameskip", Integer.parseInt(frameSkip));
                Log.d(TAG, "Frame skip set.");
            }
        }
        if (!numberOfDilations.isEmpty()){
            if (Integer.parseInt(numberOfDilations) >= 0) {
                intent.putExtra("number_of_dilations", Integer.parseInt(numberOfDilations));
                Log.d(TAG, "number_of_dilations set.");
            }
        }
        setResult(RESULT_OK, intent);
        finish();
    }
    private void initSeekbars() {
        hueControl = (SeekBar) findViewById(R.id.hue_bar);
        saturationControl = (SeekBar) findViewById(R.id.saturation_bar);
        valueControl = (SeekBar) findViewById(R.id.value_bar);
        try {
            // Listener to receive changes to the hue-progress level
            hueControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                // Necessary method implementations for OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar arg0) {}
                public void onStartTrackingTouch(SeekBar arg0) {}

                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    hue = progress;
                    hueTextView.setText(Integer.toString(hue) + "°");
                    //Toast.makeText(getApplicationContext(), "Hue: " + progress, Toast.LENGTH_SHORT).show();
                }
            });

            // Listener to receive changes to the saturation-progress level
            saturationControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                // Necessary method implementations for OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar arg0) {}
                public void onStartTrackingTouch(SeekBar arg0) {}

                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    saturation = progress;
                    saturationTextView.setText(Integer.toString(saturation) + "%");
                }
            });

            // Listener to receive changes to the value-progress level
            valueControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                // Necessary method implementations for OnSeekBarChangeListener
                public void onStopTrackingTouch(SeekBar arg0) {}
                public void onStartTrackingTouch(SeekBar arg0) {}

                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    value = progress;
                    valueTextView.setText(Integer.toString(value) + "%");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
