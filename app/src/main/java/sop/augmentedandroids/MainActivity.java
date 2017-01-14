package sop.augmentedandroids;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener, View.OnTouchListener {

    private static final String TAG = "Sample::SOP::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;

    // MainActivity parameters
    private static int frameskip = 30;
    private static int frame_i = 0;
    private static int numberOfDilations = 1;
    private static double cmToPxRatio = 1.0;
    private static boolean detecting = true;
    private static boolean saving = false;

    // RefObjDetector parameters
    private static int refObjHue = 56;
    private static int refObjColThreshold = 12;
    private static int refObjValue = 0;
    private static int refObjSatMinimum = 0;
    private static double refObjMinContourArea = 500;
    private static double refObjSideRatioLimit = 1.45;

    // MeasObjDetector variables
    private static int measObjBound = 100;
    private static int measObjMaxBound = 255;
    private static int measObjMaxArea = 1000;
    private static int measObjMinArea = 10;


    Camera c = Camera.open();

    static RotatedRect rotRect = new RotatedRect();


    NumberFormat nF = new DecimalFormat("#0.00");
    NumberFormat nF2 = new DecimalFormat("#0.0000");

    Scalar textCol = new Scalar(10, 255, 10);
    Scalar graphCol = new Scalar(255, 0, 255);

    int uiFont = Core.FONT_HERSHEY_PLAIN;
    int uiTextScale = 2;
    int uiTextThickness = 3;

    RefObjDetector cubeDetector;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    /* Now enable camera view to start receiving frames */
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d(TAG, "Creating and setting view");

        Camera.Parameters params = c.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        c.setParameters(params);

        // Log supported camera resolutions
        for( Camera.Size size: params.getSupportedPreviewSizes()){
            Log.d(TAG, Integer.toString(size.width) + "x"  + Integer.toString(size.height));
        }

        setContentView(R.layout.activity_main);

        // Initialize UI
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle("");
        setSupportActionBar(myToolbar);


        // Initialize OpenCV camera
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_main_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // set max size to 1280 x 720 to increase performance
        mOpenCvCameraView.setMaxFrameSize(1280, 720);
        mOpenCvCameraView.enableFpsMeter();

        cubeDetector = new RefObjDetector(56.0, 12.0, 120.0);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        c.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Create a new intent and pass parameters for settingsActivity when Settings button is clicked
                Log.d("onOptionsItemSelected", "Action_settings selected.");
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra("currentFrameSkip", Integer.toString(frameskip));
                intent.putExtra("currentNumberOfDilations", Integer.toString(numberOfDilations));
                intent.putExtra("currentMinContourArea", Double.toString(refObjMinContourArea));
                intent.putExtra("currentSideRatioLimit", Double.toString(refObjSideRatioLimit));
                intent.putExtra("currentMeasObjBound", Integer.toString(measObjBound));
                intent.putExtra("currentMeasObjMaxBound", Integer.toString(measObjMaxBound));
                intent.putExtra("currentMeasObjMaxArea", Integer.toString(measObjMaxArea));
                intent.putExtra("currentMeasObjMinArea", Integer.toString(measObjMinArea));
                //intent.putExtra("minHue", minHue);
                //intent.putExtra("maxHue", maxHue);
                intent.putExtra("refObjHue", refObjHue);
                intent.putExtra("refObjColThreshold", refObjColThreshold);
                startActivityForResult(intent, 1);
                return true;

            case R.id.action_detection_toggle:
                detecting = !detecting;
                return  true;

            case R.id.action_save_image:
                saving = true;
                return true;
        }
        return true;
    }

    // Method runs when Apply button is pressed in SettingsActivity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* If data coming from SettingsActivity exists, the method sets variable values accordingly.
          If a value doesn't exist, the previous value for the variable is selected. */
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            Log.v(TAG, "Requestcode OK.");
            if(resultCode == RESULT_OK){

                // TextView input
                frameskip = data.getIntExtra("frameskip", frameskip);

                numberOfDilations = data.getIntExtra("numberOfDilations", numberOfDilations);
                cubeDetector.setNumberOfDilations(numberOfDilations);

                refObjMinContourArea = data.getDoubleExtra("refObjMinContourArea", refObjMinContourArea);
                cubeDetector.setMinContourArea(refObjMinContourArea);

                refObjSideRatioLimit = data.getDoubleExtra("refObjSideRatioLimit", refObjSideRatioLimit);
                cubeDetector.setSideRatioLimit(refObjSideRatioLimit);


                measObjBound = data.getIntExtra("measObjBound", measObjBound);
                measObjMaxBound = data.getIntExtra("measObjMaxBound", measObjMaxBound);
                measObjMaxArea = data.getIntExtra("measObjMaxArea", measObjMaxArea);
                measObjMinArea = data.getIntExtra("measObjMinArea", measObjMinArea);

                // Seekbar input
                refObjHue = data.getIntExtra("refObjHue", refObjHue);
                cubeDetector.setHueMinimum(refObjHue);

            }
        }
    }


    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    public boolean onTouch(View view, MotionEvent event) {
        return false;
    }

    public Mat onCameraFrame(Mat inputFrame) {

        if(detecting) {
            // Skip calculating contour for as many frames as indicated by 'frameskip' variable
            if (frame_i < frameskip) {
                frame_i++;
            } else {
                frame_i = 0;
                cubeDetector.ProcessFrame(inputFrame);
                rotRect = cubeDetector.getRotRect();

                cmToPxRatio = (5.0 / cubeDetector.getShortSideLength());

                //CornerTest(inputFrame);
            }

            inputFrame = OnScreenDrawings(inputFrame);
        }

        if(saving) {
            saving = false;
            SaveImage(inputFrame);
        }

        return inputFrame;  // Return the final (possibly edited) image frame
    }

    private void SaveImage(Mat mImage) {
        Bitmap bmap;
        try {
            bmap = Bitmap.createBitmap(mImage.cols(), mImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mImage, bmap);

            String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures";
            File dir = new File(savePath);
            if(!dir.isDirectory()) {
                dir.mkdir();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
            Calendar c = Calendar.getInstance();
            String fname = dateFormat.format(c.getTime());

            File f = new File(savePath, fname + ".jpg");

            FileOutputStream fout = new FileOutputStream(f);
            bmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            fout.flush();
            fout.close();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private Mat OnScreenDrawings(Mat inputFrame) {
        // Draw the rotated rectangle on-screen in magenta color
        Imgproc.drawContours(inputFrame, cubeDetector.getRotRectCnt(), -1, graphCol, 2);   // Draw rotated rect into image frame

        // Write rect side ratio on-screen
        Core.putText(inputFrame, "rectangle side ratio: " + nF.format(cubeDetector.getRectSideRatio()), new Point(10.0, 60), uiFont, uiTextScale, textCol, uiTextThickness);
        Core.putText(inputFrame, "area: " + nF.format(cubeDetector.getRectArea()), new Point(10.0, 100), uiFont, uiTextScale, textCol, uiTextThickness);
        Core.putText(inputFrame, "cm/px ratio: " + nF2.format(cmToPxRatio), new Point(10.0, 140), uiFont, uiTextScale, textCol, uiTextThickness);

        // Draw a circle marker and write color info of rotated rectangle center point
        DrawRotRectCenterData(inputFrame);

        if(rotRect != null) {
            Core.putText(inputFrame, "refObject", new Point(rotRect.center.x - 60.0, rotRect.center.y + 50.0), uiFont, 2, textCol, 1);
        }

        return inputFrame;
    }

    private void CornerTest(Mat inputFrame) {

        Mat dst = new Mat(inputFrame.size(), CvType.CV_32FC1);
        Mat dst_norm = new Mat();
        Mat dst_norm_scaled = new Mat();

        Mat klooni = inputFrame.clone();
        Imgproc.cvtColor(klooni, klooni, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cornerHarris(klooni, dst, 2, 3, 0.04, Imgproc.BORDER_DEFAULT);
        //Imgproc.dilate(corners, corners, new Mat(), new Point(-1,-1), 2);

        Core.normalize(dst, dst_norm, 0, 255, Core.NORM_MINMAX, CvType.CV_32FC1, new Mat());

        Core.convertScaleAbs(dst_norm, dst_norm_scaled);

        for(int j=0; j<dst_norm.rows(); j++)
        {
            for(int i=0; i<dst_norm.cols(); i++)
            {
                if((int)dst_norm.get(j,i)[0] > 200) {
                    Core.circle(inputFrame, new Point(i,j), 3, new Scalar(255,255,0), 1);
                }
            }
        }
    }

    private void DrawRotRectCenterData(Mat frame_in) {

        double [] rectCenterCols = cubeDetector.getRectCenterCols();
        // Workaround for app crash in a case where couldn't acquire color values of rect center
        if(rectCenterCols != null) {
            Core.putText(frame_in, "H: " + String.valueOf(rectCenterCols[0]), new Point(900.0, 40), uiFont, uiTextScale, textCol, uiTextThickness);
            Core.putText(frame_in, "S: " + String.valueOf(rectCenterCols[1]), new Point(900.0, 80), uiFont, uiTextScale, textCol, uiTextThickness);
            Core.putText(frame_in, "V: " + String.valueOf(rectCenterCols[2]), new Point(900.0, 120), uiFont, uiTextScale, textCol, uiTextThickness);
            Core.circle(frame_in, rotRect.center, 10, graphCol, 3);
        }
    }
}