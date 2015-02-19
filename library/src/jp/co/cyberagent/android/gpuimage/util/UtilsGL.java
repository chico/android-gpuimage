package jp.co.cyberagent.android.gpuimage.util;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.util.Log;

/**
 <p> Utility class for OpenGL </p>
 */
public class UtilsGL {

    private static final String LOG_TAG = UtilsGL.class.getSimpleName();

    /**
     <p> Checks given integer for being power of two </p>

     @param pNumber
     integer to check for being power of two

     @return true if supplied integer is power of two, false otherwise
     */
    public static boolean isPowerOfTwo(final int pNumber) {
        return (pNumber & -pNumber) == pNumber;
    }

    /**
     <p> 4-byte allocation needed for float type </p>
     */
    public static final int BYTES_PER_FLOAT = 4;

    /**
     <p> 2-byte allocation needed for short type </p>
     */
    public static final int BYTES_PER_SHORT = 2;

    /**
     <p> Queries OpenGL for errors and if there have been some - shows all found information in logs </p>

     @param pMessage
     additional message to show in logs if error found
     */
    public static void logErrorGL(final String pMessage) {

        final int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(UtilsGL.LOG_TAG, (pMessage == null ? "" : pMessage + " >>> ") + "Opengl error " + error + ": " +
                                   GLU.gluErrorString(error));
        }

    }

}