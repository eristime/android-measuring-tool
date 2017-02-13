package sop.augmentedandroids;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

/**
 * \class   This class detects a reference measurement object using edge-detection based algorithm
 * \brief   Reference object detection class
 * \author  Janne Mustaniemi
 */

public class RefObjDetector {

    private static final String TAG = "SOP::RefObj";
    /* CLASS VARS */
    private RotatedRect rotRect;
    private double rectSideRatio;
    private double sideRatioLimit;
    private double shortSideLength;
    private double longSideLength;
    private List<MatOfPoint> rotRectCnt;
    private double rectArea;
    private double minContourArea;
    private double maxContourArea;
    private double[] rectCenterCols;
    private int refHue;
    private int colThreshold;
    private int satMinimum;
    private int numberOfDilations;


    /* GETTERS */

    public double getRectArea() {
        return rectArea;
    }

    public double[] getRectCenterCols() {
        return rectCenterCols;
    }

    public List<MatOfPoint> getRotRectCnt() {
        return rotRectCnt;
    }

    public double getRectSideRatio() { return rectSideRatio; }

    public double getShortSideLength() { return shortSideLength; }

    public double getLongSideLength() { return longSideLength; }

    public RotatedRect getRotRect() {
        return rotRect;
    }


    /* SETTERS */
    public void setNumberOfDilations(int n){
        this.numberOfDilations = n;
        Log.d(TAG, "numberOfDilations" + numberOfDilations);
    }

    public void setMinContourArea(double area){
        this.minContourArea = area;
        Log.d(TAG, "minContourArea " + minContourArea);
    }

    public void setMaxContourArea(double area) {
        this.maxContourArea = area;
        Log.d(TAG, "maxContourArea " + maxContourArea);
    }

    public void setSideRatioLimit(double limit){
        this.sideRatioLimit = limit;
        Log.d(TAG, "sideRatioLimit " + sideRatioLimit);
    }

    public void setRefHue(int hue){ this.refHue = hue;
        Log.d(TAG, "refHue " + refHue);
    }

    public void setColThreshold(int t){
        this.colThreshold = t;
        Log.d(TAG, "colThreshold " + colThreshold);
    }

    public void setSatMinimum(int s){
        this.satMinimum = s;
        Log.d(TAG, "satMinimum " + satMinimum);
    }


    /* CONSTRUCTOR */
    public RefObjDetector(int referenceHue, int colorThreshold, int saturationMinimum, int numberOfDilations,
                          double contourAreaMinimum, double contourAreaMaximum, double sideRatioLimit) {

        rotRect = null;
        rotRectCnt = new ArrayList<>();
        this.refHue = referenceHue;
        this.colThreshold = colorThreshold;
        this.satMinimum = saturationMinimum;
        this.numberOfDilations = numberOfDilations;
        this.minContourArea = contourAreaMinimum;
        this.maxContourArea = contourAreaMaximum;
        this.sideRatioLimit = sideRatioLimit;
    }


    // CALCULATE AVERAGE RECTANGLE SIDE LENGTH
    private double RectAvgLength(Point[] ps) {
        double L1 = GetDist(ps[0], ps[1]);
        double L2 = GetDist(ps[1], ps[2]);
        double L3 = GetDist(ps[2], ps[3]);
        double L4 = GetDist(ps[3], ps[0]);

        return ((L1 + L2 + L3 + L4)/4);
    }

    // CALCULATE RATIO OF SHORT AND LONG SIDE OF RECTANGLE
    private double CalcRectSideRatio(Point[] ps) {
        double L1 = GetDist(ps[0], ps[1]);
        double L2 = GetDist(ps[1], ps[2]);

        if(L1 > L2) {
            return L1/L2;
        } else {
            return L2/L1;
        }
    }

    // CALCULATE DISTANCE BETWEEN TWO COORDINATE POINTS
    private double GetDist(Point p1, Point p2) {
        return Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
    }

    // CALCULATE ROTATED RECTANGLE CONTOUR
    private void CalcRotRectContour() {

        Point[] ps = new Point[4];
        rotRect.points(ps);

        //avgSideLen = RectAvgLength(ps);

        List<MatOfPoint> cnt = new ArrayList<>();
        cnt.add(new MatOfPoint(ps));
        rotRectCnt = cnt;
    }

    // SIMPLE DOUBLE->INT CONVERSION WITH ROUNDING
    private int DoubleToInt(double a) {
        return ((int) Math.round(a));
    }

    /* Selects pixels in rectangular area of width 'scanWidth' around the 'rectCenter' and
     * calculates the average RGB values of those pixels. This gives much more accurate
     * result than single pixel, since there is some noise in most mobile cameras.
     */
    private double [] CalcAvgCenterCols(Mat frame_in, Point rectCenter, int scanWidth) {

        int totalPixels = scanWidth*scanWidth;
        int scanStartX = DoubleToInt(rectCenter.x) - (scanWidth/2);
        int scanStartY = DoubleToInt(rectCenter.y) - (scanWidth/2);

        if (scanStartX < 0) { scanStartX = 0; }
        if (scanStartY < 0) { scanStartY = 0; }

        double[] frameCols;
        double [] avgCols = new double[3];

        for (int i=0; i<scanWidth; i++) {
            for (int e=0; e<scanWidth; e++) {

                frameCols = frame_in.get(scanStartY + e, scanStartX + i);
                avgCols[0] += frameCols[0];
                avgCols[1] += frameCols[1];
                avgCols[2] += frameCols[2];
            }
        }

        avgCols[0] = avgCols[0] / totalPixels;
        avgCols[1] = avgCols[1] / totalPixels;
        avgCols[2] = avgCols[2] / totalPixels;

        return avgCols;
    }


    /* THE MAIN PROCESSING FUNCTION FOR IMAGE FRAME */
    public synchronized void ProcessFrame(Mat frame_in) {

        Mat frame = new Mat();
        Mat frameHSV = new Mat();
        Mat hueMat = new Mat();

        boolean rectUpdated = false;

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.cvtColor(frame_in, frame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(frame_in, frameHSV, Imgproc.COLOR_BGR2HSV);


        Core.inRange(frameHSV, new Scalar(refHue-colThreshold, 0, 0), new Scalar(refHue+colThreshold, 255, 255), hueMat);

        Imgproc.erode(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

        Imgproc.Canny(hueMat, frame, 50.0, 130.0);

        Imgproc.dilate(frame, frame, new Mat(), new Point(-1,-1), 1);   // Improves ignoring of small shapes that are not squarish, fps impact of 1

        Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        int contoursCounter = contours.size();

        if (contoursCounter == 0) {
            return;
        }

        double biggestContourArea = 0;

        for (int i=0; i<contoursCounter; i++) {

            MatOfPoint contour = contours.get(i);

            // Object contour area
            double area = Imgproc.contourArea(contour, true);

            // Calculate object moments to get the xy coordinates of its center
            Moments m = Imgproc.moments(contour);

            double centerX = m.get_m10() / m.get_m00();
            double centerY = m.get_m01() / m.get_m00();

            // Get average hue and saturation values around the center area of the object
            double[] contourCols = CalcAvgCenterCols(frameHSV, new Point(DoubleToInt(centerX), DoubleToInt(centerY)), 4);
            double hue = contourCols[0];
            double sat = contourCols[1];

            // Determine if object's hue is within allowed limits
            boolean hueOK = (hue > (refHue - colThreshold)) && (hue < (refHue + colThreshold));

            if (hueOK) {

                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                MatOfPoint2f curve = new MatOfPoint2f();

                double dist = Imgproc.arcLength(contour2f, true)*0.02;

                // Create a polygon approximation of contour2f, using 'dist' as accurary parameter, and storing the result in 'curve'
                Imgproc.approxPolyDP(contour2f, curve, dist, true);

                // Create a minimum size rotated rectangle approximation around the object
                RotatedRect r = Imgproc.minAreaRect(curve);
                Point[] ps = new Point[4];
                r.points(ps);

                double sideRatio, shortSide, longSide;

                double L1 = GetDist(ps[0], ps[1]);
                double L2 = GetDist(ps[1], ps[2]);

                // Compare rectangle side lengths and determine which is short and which is long
                if(L1 > L2) {
                    sideRatio = L1/L2;
                    shortSide = L2;
                    longSide = L1;
                } else {
                    sideRatio = L2/L1;
                    shortSide = L1;
                    longSide = L2;
                }

                /*
                 *If detected object has acceptable rectangle side ratio, area and color saturation,
                 * accept it as the true reference object and update class member variables
                 */
                if (sideRatio <= sideRatioLimit && area > biggestContourArea && area < maxContourArea && sat >= satMinimum && area > minContourArea) {
                    biggestContourArea = area;
                    rectCenterCols = contourCols;
                    rectArea = area;
                    rectSideRatio = sideRatio;
                    rotRect = r;
                    shortSideLength = shortSide;
                    longSideLength = longSide;
                    rectUpdated = true;
                }
            }
        }

        if (rectUpdated) {
            CalcRotRectContour();   // This is the contour for on-screen drawing
        }

    }
}
