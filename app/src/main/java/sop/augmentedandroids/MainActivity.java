package sop.augmentedandroids;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
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
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener, View.OnTouchListener {

    private static final String TAG = "Sample::SOP::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;

    private static boolean detecting = true;
    private static boolean saving = false;

    // Intent check-values
    static final int GET_INPUT_DATA = 1;

	// MainActivity parameters
    private static int frameskip = 30;
    private static int frame_i = 0;
    private static double cmToPxRatio = 1.0;
    private static double measSide1 = 0.0;
    private static double measSide2 = 0.0;
    private static double measRectShortSide = 0.0;
    private static double measRectLongSide = 0.0;
    private static double refRectShortSide = 0.0;
    private static double refRectLongSide = 0.0;

    // RefObjDetector parameters
    // variables which are set by slide bar are ints instead of doubles because of poor support
    private static int refObjHue = 56;
    private static int refObjColThreshold = 12;
    private static int refObjSatMinimum = 120;
    private static int numberOfDilations = 1;
    private static double refObjMinContourArea = 500.0;
    private static double refObjMaxContourArea = 800000.0;
    private static double refObjSideRatioLimit = 1.45;


    // MeasObjDetector variables
    private static int measObjBound = 100;
    private static int measObjMaxBound = 255;
    private static int measObjMinArea = 10000;
    private static int measObjMaxArea = 100000;

    Camera c = Camera.open();

    static RotatedRect rotRect = new RotatedRect();
    static RotatedRect measRect = new RotatedRect();
    static List<MatOfPoint> measDrawRect = new ArrayList<>();
    static List<RotatedRect> measRects;
    static List<List<MatOfPoint>> measDrawRects;

    NumberFormat nF1 = new DecimalFormat("#0.0");
    NumberFormat nF2 = new DecimalFormat("#0.00");
    NumberFormat nF4 = new DecimalFormat("#0.0000");

    Scalar textCol = new Scalar(10, 255, 10);
    Scalar graphCol = new Scalar(255, 0, 255);
    Scalar measCol = new Scalar(0, 127, 255);

    int uiFont = Core.FONT_HERSHEY_PLAIN;
    int uiTextScale = 1;
    int uiTextThickness = 2;

    RefObjDetector cubeDetector;
    MeasObjDetector measDetector;

    boolean circleOption = false;


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

        measRects = new ArrayList<>();
        measDrawRects = new ArrayList<>();

        cubeDetector = new RefObjDetector(
                refObjHue,
                refObjColThreshold,
                refObjSatMinimum,
                numberOfDilations,
                refObjMinContourArea,
                refObjMaxContourArea,
                refObjSideRatioLimit);

        measDetector = new MeasObjDetector(measObjBound, measObjMaxBound, measObjMinArea, measObjMaxArea);
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
                intent.putExtra("frameSkip", Integer.toString(frameskip));
                intent.putExtra("numberOfDilations", Integer.toString(numberOfDilations));
                intent.putExtra("refObjMinContourArea", Double.toString(refObjMinContourArea));
                intent.putExtra("refObjMaxContourArea", Double.toString(refObjMaxContourArea));
                intent.putExtra("sideRatioLimit", Double.toString(refObjSideRatioLimit));
                intent.putExtra("measObjBound", Integer.toString(measObjBound));
                intent.putExtra("measObjMaxBound", Integer.toString(measObjMaxBound));
                intent.putExtra("measObjMaxArea", Integer.toString(measObjMaxArea));
                intent.putExtra("measObjMinArea", Integer.toString(measObjMinArea));
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

            case R.id.shape_circle:
                circleOption = true;
                return true;
            case R.id.shape_rectangle:
                circleOption = false;
                return true;
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* If data coming from SettingsActivity exists, the method sets variable values accordingly.
          If a value doesn't exist, the previous value for the variable is selected. */
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GET_INPUT_DATA) {
            Log.v(TAG, "Requestcode OK.");
            if(resultCode == RESULT_OK){

                // TextView input
                frameskip = data.getIntExtra("frameSkip", frameskip);

                numberOfDilations = data.getIntExtra("numberOfDilations", numberOfDilations);
                cubeDetector.setNumberOfDilations(numberOfDilations);

                refObjMinContourArea = data.getDoubleExtra("refObjMinContourArea", refObjMinContourArea);
                cubeDetector.setMinContourArea(refObjMinContourArea);

                refObjMaxContourArea = data.getDoubleExtra("refObjMaxContourArea", refObjMaxContourArea);
                cubeDetector.setMaxContourArea(refObjMaxContourArea);

                refObjSideRatioLimit = data.getDoubleExtra("refObjSideRatioLimit", refObjSideRatioLimit);
                cubeDetector.setSideRatioLimit(refObjSideRatioLimit);

                measObjBound = data.getIntExtra("measObjBound", measObjBound);
                measDetector.setBound(measObjBound);

                measObjMaxBound = data.getIntExtra("measObjMaxBound", measObjMaxBound);
                measDetector.setMaxBound(measObjMaxBound);
                // Need for setBoundMax??

                measObjMinArea = data.getIntExtra("measObjMinArea", measObjMinArea);
                measDetector.setMin(measObjMinArea);

                measObjMaxArea = data.getIntExtra("measObjMaxArea", measObjMaxArea);
                measDetector.setMax_area(measObjMaxArea);

                // Seekbar input
                refObjHue = data.getIntExtra("refObjHue", refObjHue);
                cubeDetector.setRefHue(refObjHue);

                refObjColThreshold = data.getIntExtra("refObjColThreshold", refObjColThreshold);
                cubeDetector.setColThreshold(refObjColThreshold);

            }
        }
    }

    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    public boolean onTouch(View view, MotionEvent event) {
        detecting = !detecting;
        return false;
    }

    public Mat onCameraFrame(Mat inputFrame) {

        if(detecting) {

            // Skip processing for as many frames as indicated by 'frameskip' variable
            if (frame_i < frameskip) {
                frame_i++;
            } else {
                frame_i = 0;

                /**
                 * Detecting reference object through RefObjDetector class
                 */
                cubeDetector.ProcessFrame(inputFrame);
                rotRect = cubeDetector.getRotRect();

                refRectShortSide = cubeDetector.getShortSideLength();
                refRectLongSide = cubeDetector.getLongSideLength();

                cmToPxRatio = (5.0 / cubeDetector.getShortSideLength());


                if(rotRect != null) {

                    /**
                     * Detecting possible measured objects through MeasObjDetector class
                     */
                    measRects = new ArrayList<>();
                    measDrawRects = new ArrayList<>();
                    measDetector.detectMeasurable(inputFrame);

                    double largestMeasArea = 0.0;
                    int largestMeasIndex = -1;

                    for (int i = 0; i < measDetector.getContours().size(); i++) {
                        measDetector.ptsDistance(i);
                        measRects.add(measDetector.get_minAreaRect());
                        measDrawRects.add(measDetector.getDrawContour());
                        double a = Imgproc.contourArea(measDetector.getContours().get(i));
                        if (a > largestMeasArea && !IsRectInContour(rotRect, measDetector.getContours().get(i))) {
                            largestMeasArea = a;
                            largestMeasIndex = i;
                            measRectShortSide = measDetector.getMinLen();
                            measRectLongSide = measDetector.getMaxLen();
                        }
                    }

                    /**
                     * Convert measured object dimension from pixels to centimeters
                     */
                    if (largestMeasIndex > -1) {
                        measDrawRect = measDrawRects.get(largestMeasIndex);
                        measRect = measRects.get(largestMeasIndex);
                        measSide1 = measRectShortSide * cmToPxRatio;
                        measSide2 = measRectLongSide * cmToPxRatio;
                    }
                }
            }
            inputFrame = OnScreenDrawings(inputFrame);
        }

        if(saving) {
            saving = false;
            SaveImage(inputFrame);
        }

        return inputFrame;  // Return the final (possibly edited) image frame
    }

    private Mat OnScreenDrawings(Mat inputFrame) {

        /**
         *  Drawing of miscellaneous info texts to user interface
         *
         */
        // Write calculated centimeters per pixel ratio on-screen
        Core.putText(inputFrame, "cm/px ratio: " + nF4.format(cmToPxRatio), new Point(10.0, 60), uiFont, uiTextScale, textCol, uiTextThickness);

        // Debugging: Write reference rect side ratio on-screen
        //Core.putText(inputFrame, "rectangle side ratio: " + nF2.format(cubeDetector.getRectSideRatio()), new Point(10.0, 140), uiFont, uiTextScale, textCol, uiTextThickness);
        // Debugging: Write reference rect area on-screen
        //Core.putText(inputFrame, "area: " + nF2.format(cubeDetector.getRectArea()) + "px^2, new Point(10.0, 180), uiFont, uiTextScale, textCol, uiTextThickness);

        // Debugging: Draw a circle marker and write color info of rotated rectangle center point
        //DrawRotRectCenterData(inputFrame);


        /**
         *  Draw the reference rectangle on-screen in magenta color
         *
         */
        Imgproc.drawContours(inputFrame, cubeDetector.getRotRectCnt(), -1, graphCol, 2);   // Draw rotated rect into image frame

        if(rotRect != null) {
            Core.putText(inputFrame, "refObject", new Point(rotRect.center.x - 60.0, rotRect.center.y + 120.0), uiFont, 2, textCol, 1);

            if(!circleOption) {
                Core.putText(inputFrame, "meas. angle: " + nF1.format(measRect.angle), new Point(10.0, 100), uiFont, uiTextScale, textCol, uiTextThickness);
            }

            // Debugging:
            //Core.putText(inputFrame, "ref angle: " + nF1.format(rotRect.angle), new Point(10.0, 220), uiFont, uiTextScale, textCol, uiTextThickness);
        }


        /**
         *  Drawing the measured rectangle of circle and informative texts about measured dimensions
         *
         */

        if(circleOption) {

            Point circleTextPos1 = new Point(measRect.center.x, measRect.center.y + (int)(measRect.size.height/2) + 25*uiTextScale);
            Point circleTextPos2 = new Point(measRect.center.x, measRect.center.y + (int)(measRect.size.height/2) + 40*uiTextScale);

            double radius = ((Math.sqrt(measRectLongSide*measRectShortSide))/2);
            double radius_cm = radius * cmToPxRatio;
            double area_cm = 3.14 * (radius_cm*radius_cm);

            Core.circle(inputFrame, measRect.center, (int)radius, measCol, 1);
            Core.putText(inputFrame, "radius: " + nF1.format(radius_cm) + "cm", circleTextPos1, uiFont, uiTextScale, textCol, 1);
            Core.putText(inputFrame, "area: " + nF1.format(area_cm) + "cm^2", circleTextPos2, uiFont, uiTextScale, textCol, 1);

        } else {

            Point rectTextPos1 = new Point(measRect.center.x, measRect.center.y - 10*uiTextScale);
            Point rectTextPos2 = new Point(measRect.center.x, measRect.center.y + 10*uiTextScale);

            // Draw the measured rectangle on-screen in magenta color
            Imgproc.drawContours(inputFrame, measDrawRect, -1, measCol, 2);

            // Write dimensions of measured object on-screen
            Core.putText(inputFrame, "side 1: " + nF1.format(measSide1) + " cm", rectTextPos1, uiFont, uiTextScale, textCol, 1);
            Core.putText(inputFrame, "side 2: " + nF1.format(measSide2) + " cm", rectTextPos2, uiFont, uiTextScale, textCol, 1);

        }

        return inputFrame;
    }

    private void SaveImage(Mat mImage) {
        Bitmap bmap;
        try {
            bmap = Bitmap.createBitmap(mImage.cols(), mImage.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mImage, bmap);

            //String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Pictures";
            String savePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
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

            Log.d(TAG, savePath);
            Log.d(TAG, fname);

            //Toast.makeText(getApplicationContext(), "The image was saved as " + fname + ".jpg to " + savePath, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    boolean IsRectInContour(RotatedRect rect, MatOfPoint cnt) {

        MatOfPoint2f cnt2 = new MatOfPoint2f(cnt.toArray());

        for(int i=0; i<4; i++) {

            if(Imgproc.pointPolygonTest(cnt2, rect.center, false) > 0) {
                Log.d(TAG, "REF INSIDE MEAS");
                return true;
            }
        }
        return false;
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