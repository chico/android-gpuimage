#include <jni.h>

#include <GLES3/gl3.h>


#ifdef __cplusplus
extern "C" {
    JNIEXPORT void JNICALL Java_com_android_gl2jni_GLES3PBOReadPixelsFix_glReadPixelsPBO(JNIEnv * env, jobject obj, jint x, jint y, jint width, jint height, jint format, jint type, jint offsetPBO);
};
#endif

JNIEXPORT void JNICALL Java_com_android_gl2jni_GLES3PBOReadPixelsFix_glReadPixelsPBO(JNIEnv * env, jobject obj, jint x, jint y, jint width, jint height, jint format, jint type, jint offsetPBO)
{
    glReadPixels(x, y, width, height, format, type, offsetPBO);
}