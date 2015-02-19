package com.android.gl2jni;

public class GLES3PBOReadPixelsFix {

    static {
        System.loadLibrary("gpuimage-library");
    }

    // PBO version of glReadPixels()
    // C function void glReadPixels ( GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels )

    public static native void glReadPixelsPBO(int x, int y, int width, int height, int format, int type, int offsetPBO);

}
