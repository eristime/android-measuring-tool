package sop.augmentedandroids;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener, View.OnTouchListener {

    private static final String TAG = "Sample::SOP::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;

    private static int frameskip = 5;
    private static int frame_i = 0;

    Camera c = Camera.open();

    static Rect refRect = new Rect(0,0,0,0);
    static RotatedRect rotRect = new RotatedRect();
    static List<MatOfPoint> rotRectCnt = new ArrayList<>();

    Scalar textCol = new Scalar(255, 0, 255);

    RefObjDetector cubeDetector;
    MeasObjDetector measDetector;

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

        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.enableFpsMeter();

        cubeDetector = new RefObjDetector();
        measDetector = new MeasObjDetector();
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public boolean onTouch(View view, MotionEvent event) {
        return false;
    }

    public Mat onCameraFrame(Mat inputFrame) {

        // Skip calculating contour for as many frames as indicated by 'frameskip' variable
        if(frame_i < frameskip) {
            frame_i++;
        } else {
            frame_i = 0;
            cubeDetector.ProcessFrame(inputFrame);
            refRect = cubeDetector.getRefRect();
            rotRect = cubeDetector.getRotRect();
        }

        // Draw the bounding rectangle on-screen in green color
        Core.rectangle(inputFrame, refRect.tl(), refRect.br(), new Scalar(0,255,10,255), 2);


        // Draw the rotated rectangle on-screen in magenta color
        Imgproc.drawContours(inputFrame, rotRectCnt, -1, new Scalar(255, 0, 255), 2);   // Draw rotated rect into image frame


        // Write average rect side length on-screen
        NumberFormat nF = new DecimalFormat("#0.00");
        Core.putText(inputFrame, "avg side length: " + nF.format(cubeDetector.getAvgSideLen()), new Point(10.0, 100), Core.FONT_HERSHEY_PLAIN, 3, textCol, 3);


        // Draw a circle marker and write color info of rotated rectangle center point
        DrawRotRectCenterData(inputFrame);

        return inputFrame;  // Return the final edited image frame
    }

    private void DrawRotRectCenterData(Mat frame_in) {

        // Center of the rotated rectangle
        Point rectCenterPoint = rotRect.center;

        double [] rectCenterCols = cubeDetector.getRectCenterCols();
        // Workaround for app crash in a case where couldn't acquire color values of rect center
        if(rectCenterCols != null) {
            //Scalar rectCenterCols = new Scalar(DoubleToInt(rectCenterCols[0]), DoubleToInt(rectCenterCols[1]), DoubleToInt(rectCenterCols[2]));

            Core.putText(frame_in, "R: " + String.valueOf(rectCenterCols[0]), new Point(900.0, 40), Core.FONT_HERSHEY_PLAIN, 3, textCol, 3);
            Core.putText(frame_in, "G: " + String.valueOf(rectCenterCols[1]), new Point(900.0, 100), Core.FONT_HERSHEY_PLAIN, 3, textCol, 3);
            Core.putText(frame_in, "B: " + String.valueOf(rectCenterCols[2]), new Point(900.0, 160), Core.FONT_HERSHEY_PLAIN, 3, textCol, 3);

            Core.circle(frame_in, rectCenterPoint, 10, textCol, 3);
        }
    }

    double RectAvgLength(Point[] ps) {
        double L1 = GetDist(ps[0], ps[1]);
        double L2 = GetDist(ps[1], ps[2]);
        double L3 = GetDist(ps[2], ps[3]);
        double L4 = GetDist(ps[3], ps[0]);

        return ((L1 + L2 + L3 + L4)/4);
    }

    double GetDist(Point p1, Point p2) {
        return Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
    }
}