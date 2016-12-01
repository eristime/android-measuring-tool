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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener, View.OnTouchListener {

    private static final String TAG = "Sample::SOP::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;

    private static int frameskip = 5;
    private static int frame_i = 0;

    Camera c = Camera.open();

    static Rect refRect = new Rect(0,0,0,0);

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

        if(frame_i < frameskip) {
            frame_i++;
        } else {
            frame_i = 0;
            UpdateContours(inputFrame);
        }

        //UpdateContours(inputFrame);
        Core.rectangle(inputFrame, refRect.tl(), refRect.br(), new Scalar(0,255,10,255), 3);
        return inputFrame;

        //Core.putText(frame_out, String.valueOf(biggestContourArea), new Point(50.0, 140.0), Core.FONT_HERSHEY_PLAIN, 7, new Scalar(200,150,250), 3);
    }

    public synchronized void UpdateContours(Mat frame_in) {

        Mat frame = frame_in.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2GRAY);

        Core.bitwise_not(frame, frame);     // Seems to improve detection, maybe. Test more..

        Imgproc.Canny(frame, frame, 50.0, 175.0);
        Imgproc.dilate(frame, frame, new Mat(), new Point(-1,-1), 1);   // Improves ignoring of small shapes that are not squarish, fps impact of 1

        Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        int contoursCounter = contours.size();

        if (contoursCounter == 0) {
            return;
        }

        int biggestContour_i = 0;
        double biggestContourArea = 0;

        for (int i=0; i<contoursCounter; i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > biggestContourArea && area < 800000) {
                biggestContour_i = i;
                biggestContourArea = area;
            }
        }

        List<MatOfPoint> bigContour = new ArrayList<MatOfPoint>();
        bigContour.add(contours.get(biggestContour_i));

        MatOfPoint2f contour2f = new MatOfPoint2f(bigContour.get(0).toArray());
        MatOfPoint2f curve = new MatOfPoint2f();

        double dist = Imgproc.arcLength(contour2f, true)*0.02;

        Imgproc.approxPolyDP(contour2f, curve, dist, true);

        MatOfPoint biggestContour = new MatOfPoint(curve.toArray());

        refRect = Imgproc.boundingRect(biggestContour);

        //Log.d(TAG, Double.toString(biggestContourArea));
    }
}