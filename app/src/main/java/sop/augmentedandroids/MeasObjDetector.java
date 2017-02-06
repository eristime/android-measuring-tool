package sop.augmentedandroids;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;

/**
 * \class   This class implements blob detection that can be adjusted via given parameters
 * \brief   Blob decetion algorithm class
 * \author  Joonas Jyrkkä
 * \notes   All the variables are either local or private. If you need to modify the variables, please see the API functions
 *
 * Usage:
 *          Create local List to store the algorithm return points (List<MatOfPoint> contours = new ArrayList<>(); for example)
 *          measDetector.detectMeasurable(inputFrame);
 *          contours = measDetector.getContours();
 *          Imgproc.drawContours(inputFrame, contours, -1, new Scalar(0,0,255), 2) - or other measuring preparations
 *          measDetector.ptsDistance(contour) to set the active blob paramters
 *          measDetector.getMinLen() / getMaxLen() / getMidpoint()
 */

public class MeasObjDetector {

    /*
    * \brief    Private variables of the class
    * \note     These should not be set within the class (TODO)
    */
    private List<MatOfPoint> contours;
    private List<MatOfPoint> rawContours;
    List<MatOfPoint> drawContour;
    private static final String TAG = "SOP::MeasObj";

    private int bound = 100;
    private static int max_bound = 255;
    private int max_area = 100000;
    private int min_area = 1000;

    private double dist0 = 0.0;
    private double dist1 = 0.0;
    private Point middle = new Point();

    RotatedRect _minAreaRect;

    /*
    * \brief    Constructor of the class
    * \param    None
    * \returns  None
    */
    public MeasObjDetector() {
        contours = new ArrayList<>();
        _minAreaRect = new RotatedRect();
    }

    public RotatedRect get_minAreaRect() {
        return _minAreaRect;
    }

    /*
    * \brief    Method to get the valid contours for detected blobs
    * \param    None
    * \returns  MatOfPoints for valid contours
    */

    public List<MatOfPoint> getContours() {
        return contours;
    }

    /*
    * \brief    Method to set the required parameters for valid blobs
    * \param    min is minimum area that the blob has to meet in order to pass the algorithm
    * \returns  None
    */

    public List<MatOfPoint> getDrawContour() {
        return drawContour;
    }

    public void setMin(int val) {
        min_area = val;
    }

    /*
    * \brief    Method to set the required parameters for valid blobs
    * \param    max_area is maximum area that the blob has to meet in order to pass the algorithm
    * \returns  None
    */

    public void setMax_area(int val) {
        max_area = val;
    }

    /*
    * \brief    Method to set the required parameters for valid blobs
    * \param    bound is minimum threadshold that the blob has to meet in order to pass the algorithm
    * \returns  None
    */

    public void setBound(int val) {
        bound = val;
    }

    /*
    * \brief    Function to calculate distance between point set and middle point of the system
    * \param    Int i active blob
    * \returns  None
    */

    public void ptsDistance(int i) {

        // Selected contour is converted to MatOfPoint2f
        MatOfPoint2f contourAsFloat = new MatOfPoint2f();
        contours.get(i).convertTo(contourAsFloat, CvType.CV_32F);

        /* option 2 _minAreaRect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
        * http://stackoverflow.com/questions/11273588/how-to-convert-matofpoint-to-matofpoint2f-in-opencv-java-api
        * */

        _minAreaRect = Imgproc.minAreaRect(contourAsFloat);
        Point[] pts = new Point[4];
        _minAreaRect.points(pts);

        drawContour = new ArrayList<>();
        drawContour.add(new MatOfPoint(pts));

        double aSub = pts[0].x - pts[1].x;
        double bSub = pts[0].y - pts[1].y;
        dist0 = Math.sqrt((aSub * aSub) + (bSub * bSub));

        aSub = pts[1].x - pts[2].x;
        bSub = pts[1].y - pts[2].y;
        dist1 = Math.sqrt((aSub * aSub) + (bSub * bSub));

        middle = _minAreaRect.center;
    }


    /*
    * \brief    Method to get the smaller diameter of the blob
    * \param    None
    * \returns  Double diameter
    */

    public double getMinLen() {
        return Math.min(dist0, dist1);
    }

    /*
    * \brief    Method to get the larger diameter of the blob
    * \param    None
    * \returns  Double diameter
    */

    public double getMaxLen() { return Math.max(dist0, dist1);
    }

    /*
    * \brief    Method to get the middle point
    * \param    None
    * \returns  Double middle
    */

    public Point getMidpoint() { return middle;
    }

    /*
    * \brief    Algorithm for blob detection and filtering
    * \param    inputFrame is the captured frame from public Mat onCameraFrame()
    * \returns  None
    *
    * This function currently implements the filtering and detecting logic as following:
    *
    * PSEUDO-CODE:
    * (0000) Clear the local and private variables for new algorithm run
    * (0100) Convert the received Mat to grayscale image for mask processing
    *        The conversion is done on principle that follows the following guideline:
    *        - RGB[A] to Gray: Y <- 0.299 * R + 0.587 * G + 0.114 * B
    * (0200) Applies the bounded parameters to the filtered image via bound and max_bound variables
    *        The threadshold is applies as THRESH_BINARY, which follows the following rule:
    *           (IF)    src(x,y) > bound
    *               MAXVAL
    *           (ELSE)
    *               0
    * (0300) Find the applicable contours from the threadshold detected Mat and return the full RETR_TREE points
    *        RETR_TREE is used is current implementation but RETR_EXTERNAL could be used to save progress time
    *        \note Please see if the current implementation is good enough
    * (0400) Filter the found blobs using the parameters set (min_area, max_area)
    * 
    */

    public void detectMeasurable(Mat inputFrame) {

        // (0000)
        contours.clear();
        Mat gFrame = new Mat();
        Mat mhierarchy = new Mat();

        // (0100)
        /*
        *   \note   This works smoother than Core.mixChannels(). Needs more investigation if this is best method to allocate the filtered image
        */
        Imgproc.cvtColor(inputFrame, gFrame, Imgproc.COLOR_BGRA2GRAY);

        // (0200)
        /*
        *   \note   This works smoother than Imgproc.blur() or Imgproc.adaptiveThreshold()  but we still need to implement blur/adaptive methods to reduce unwanted noise
        *           There is also possibility to use Imgproc.inRange() which would also require Core.dilate()
        */
        Imgproc.threshold(gFrame, gFrame, bound, max_bound, Imgproc.THRESH_BINARY);

        // (0300)
        Imgproc.findContours(gFrame, contours, mhierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // (0400)
        int cnt_count = contours.size();
        if(cnt_count > 0) {
            for (int i = cnt_count; i == 0; i--) {
                if (Imgproc.contourArea(contours.get(i)) < min_area || Imgproc.contourArea(contours.get(i)) > max_area) {
                    contours.remove(i);
                }
            }
        }

        /*
        * \note     We still need to adapt the following features before measuring: (TODO)
        *           - adaptiveThreadshold of some kind to avoid unwanted noise in measurements
        */

    }
}
