package sop.augmentedandroids;

import android.util.Log;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

/**
 * \class   This class implements blob detection that can be adjusted via given parameters
 * \brief   Blob decetion algorithm class
 * \author  Joonas Jyrkkä
 * \notes   All the variables are either local or private. If you need to modify the variables, please see the API functions
 *
 * Usage:
 *          Create local List to store the algorithm return poits (List<MatOfPoint> contours = new ArrayList<>(); for example)
 *          measDetector.detectMeasurable(inputFrame);
 *          contours = measDetector.getContours();
 *          Imgproc.drawContours(inputFrame, contours, -1, new Scalar(0,0,255), 2) - or other measuring preparations
 */

public class MeasObjDetector {

    /*
    * \brief    Private variables of the class
    * \note     These should not be set within the class (TODO)
    */
    private List<MatOfPoint> contours;
    private static final String TAG = "SOP::MeasObj";

    private int bound = 100;
    private static int max_bound = 255;
    private int max_area = 1000;
    private int min_area = 10;

    /*
    * \brief    Constructor of the class
    * \param    None
    * \returns  None
    */
    public MeasObjDetector() {
        contours = new ArrayList<>();
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
        for (int i = 0; i < contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) < min_area || Imgproc.contourArea(contours.get(i)) > max_area) {
                contours.remove(i);
            }
        }

        /*
        * \note     We still need to adapt the following features before measuring: (TODO)
        *           - adaptiveThreadshold of some kind to avoid unwanted noise in measurements
        *           - Filtering by convexity or minAreaRect
        *           - Implement the usage in MainActivty onTouch function
        */

    }
}
