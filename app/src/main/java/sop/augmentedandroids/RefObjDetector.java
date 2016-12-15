package sop.augmentedandroids;

import org.opencv.core.Core;
import org.opencv.core.Mat;
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
 * Created by Janne on 6.12.2016.
 *
 */

public class RefObjDetector {

    /* CLASS VARS */
    private RotatedRect rotRect;
    private double avgSideLen;
    private List<MatOfPoint> rotRectCnt;
    double rectArea;
    double minContourArea;
    private double[] rectCenterCols;
    private double refHue;
    private double colThreshold;
    private double satMinimum;
    private int numberOfDilations = 1;


    /* GETTERS */

    public double getRectArea() {
        return rectArea;
    }

    public double getMinContourArea() {
        return minContourArea;
    }

    public double[] getRectCenterCols() {
        return rectCenterCols;
    }

    public List<MatOfPoint> getRotRectCnt() {
        return rotRectCnt;
    }

    public double getAvgSideLen() {
        return avgSideLen;
    }

    public RotatedRect getRotRect() {
        return rotRect;
    }


    /* SETTERS */
    public void setNumberOfDilations(int n){
        this.numberOfDilations = n;
    }

    /* CONSTRUCTOR */
    public RefObjDetector(double referenceHue, double colorThreshold, double saturationMinimum) {

        rotRect = null;
        rotRectCnt = new ArrayList<>();
        minContourArea = 100;
        this.refHue = referenceHue;
        this.colThreshold = colorThreshold;
        this.satMinimum = saturationMinimum;
    }


    // CALCULATE AVG RECT SIDE LENGTH
    private double RectAvgLength(Point[] ps) {
        double L1 = GetDist(ps[0], ps[1]);
        double L2 = GetDist(ps[1], ps[2]);
        double L3 = GetDist(ps[2], ps[3]);
        double L4 = GetDist(ps[3], ps[0]);

        return ((L1 + L2 + L3 + L4)/4);
    }

    private double GetDist(Point p1, Point p2) {
        return Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
    }

    // CALCULATE ROTATED RECTANGLE (MAGENTA ONE)
    private void CalcRotRectContour() {

        Point[] ps = new Point[4];
        rotRect.points(ps);

        avgSideLen = RectAvgLength(ps);

        List<MatOfPoint> cnt = new ArrayList<>();
        cnt.add(new MatOfPoint(ps));
        rotRectCnt = cnt;
    }

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

        Mat frame = frame_in.clone();
        Mat frameHSV = frame_in.clone();

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(frameHSV, frameHSV, Imgproc.COLOR_RGB2HSV);

        Imgproc.Canny(frame, frame, 50.0, 130.0);
        Imgproc.dilate(frame, frame, new Mat(), new Point(-1,-1), 1);   // Improves ignoring of small shapes that are not squarish, fps impact of 1
        //Imgproc.erode(frame, frame, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

        Imgproc.findContours(frame, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        int contoursCounter = contours.size();

        if (contoursCounter == 0) {
            return;
        }

        int biggestContour_i = 0;
        double biggestContourArea = 0;

        for (int i=0; i<contoursCounter; i++) {

            MatOfPoint contour = contours.get(i);
            double area = Imgproc.contourArea(contour);

            Moments m = Imgproc.moments(contour);

            double centerX = m.get_m10() / m.get_m00();
            double centerY = m.get_m01() / m.get_m00();

            double[] contourCols = CalcAvgCenterCols(frameHSV, new Point(DoubleToInt(centerX), DoubleToInt(centerY)), 4);
            double hue = contourCols[0];
            double sat = contourCols[1];

            boolean hueOK = (hue > (refHue - colThreshold)) && (hue < (refHue + colThreshold));

            if (area > biggestContourArea && area < 800000 && hueOK && sat >= satMinimum && area > minContourArea) {
                biggestContour_i = i;
                biggestContourArea = area;
                rectCenterCols = contourCols;
                rectArea = area;
            }
        }

        List<MatOfPoint> bigContour = new ArrayList<>();
        bigContour.add(contours.get(biggestContour_i));

        MatOfPoint2f contour2f = new MatOfPoint2f(bigContour.get(0).toArray());
        MatOfPoint2f curve = new MatOfPoint2f();

        double dist = Imgproc.arcLength(contour2f, true)*0.02;

        Imgproc.approxPolyDP(contour2f, curve, dist, true);

        rotRect = Imgproc.minAreaRect(curve);

        CalcRotRectContour();   // This is the contour for on-screen drawing

        //rectCenterCols = CalcAvgCenterCols(frameHSV, rotRect.center, 4);
    }
}
