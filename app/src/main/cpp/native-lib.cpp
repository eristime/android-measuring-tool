#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;

/*example structure from tutorial2*/

extern "C" {
JNIEXPORT void JNICALL Java_sop_augmentedandroids_MainActivity_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);
JNIEXPORT int JNICALL Java_sop_augmentedandroids_MainActivity_Test(JNIEnv*, jobject, int number);

JNIEXPORT void JNICALL Java_sop_augmentedandroids_MainActivity_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{
    Mat& mGr  = *(Mat*)addrGray;
    Mat& mRgb = *(Mat*)addrRgba;
    vector<KeyPoint> v;

    FastFeatureDetector detector(50);
    detector.detect(mGr, v);
    for( unsigned int i = 0; i < v.size(); i++ )
    {
        const KeyPoint& kp = v[i];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }
}

JNIEXPORT int JNICALL Java_sop_augmentedandroids_MainActivity_Test(JNIEnv*, jobject, int number)
{
       return number;
}



}
