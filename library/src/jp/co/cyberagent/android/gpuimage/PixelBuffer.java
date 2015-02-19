/*
 * Copyright (C) 2012 CyberAgent
 * Copyright (C) 2010 jsemler 
 * 
 * Original publication without License
 * http://www.anddev.org/android-2d-3d-graphics-opengl-tutorials-f2/possible-to-do-opengl-off-screen-rendering-in-android-t13232.html#p41662
 */

package jp.co.cyberagent.android.gpuimage;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;

import com.android.gl2jni.GLES3PBOReadPixelsFix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.util.UtilsGL;

import static javax.microedition.khronos.egl.EGL10.EGL_ALPHA_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_BLUE_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_DEPTH_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_GREEN_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_HEIGHT;
import static javax.microedition.khronos.egl.EGL10.EGL_NONE;
import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_RED_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_STENCIL_SIZE;
import static javax.microedition.khronos.egl.EGL10.EGL_WIDTH;
import static javax.microedition.khronos.opengles.GL10.GL_UNSIGNED_BYTE;

public class PixelBuffer {

    private static final String LOG_TAG = PixelBuffer.class.getSimpleName();

    final static boolean LIST_CONFIGS = false;

    GLSurfaceView.Renderer mRenderer; // borrow this interface
    int                    mWidth, mHeight;
    Bitmap mBitmap;

    EGL10       mEGL;
    EGLDisplay  mEGLDisplay;
    EGLConfig[] mEGLConfigs;
    EGLConfig   mEGLConfig;
    EGLContext  mEGLContext;
    EGLSurface  mEGLSurface;
    GL10        mGL;

    String mThreadOwner;
    private int[] mPboHandleContainer;

    public PixelBuffer(final int width, final int height) {
        mWidth = width;
        mHeight = height;

        int[] version = new int[2];
        int[] attribList = new int[]{EGL_WIDTH, mWidth, EGL_HEIGHT, mHeight, EGL_NONE};

        // No error checking performed, minimum required code to elucidate logic
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetDisplay(EGL_DEFAULT_DISPLAY);
        mEGL.eglInitialize(mEGLDisplay, version);
        mEGLConfig = chooseConfig(); // Choosing a config is a little more
        // complicated

        // mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig,
        // EGL_NO_CONTEXT, null);
        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig, EGL_NO_CONTEXT, attrib_list);

        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, attribList);
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);

        mGL = (GL10) mEGLContext.getGL();

        // Record thread owner of OpenGL context
        mThreadOwner = Thread.currentThread()
                             .getName();

        if (GPUImage.sGLESVersion == 3) {
            this.setupPBO();
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void setupPBO() {

        // Create PBO
        mPboHandleContainer = new int[1];
        GLES30.glGenBuffers(1, mPboHandleContainer, 0);

        UtilsGL.logErrorGL("glGenBuffers x1");
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboHandleContainer[0]);
        UtilsGL.logErrorGL("glBindBuffer pix pbo create");
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, mWidth * mHeight * 4, null, GLES30.GL_STATIC_DRAW);
        UtilsGL.logErrorGL("glBufferData pix pbo");
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
        UtilsGL.logErrorGL("glBindBuffer pix 0 create end");

    }

    public void setRenderer(final GLSurfaceView.Renderer renderer) {
        mRenderer = renderer;

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread()
                   .getName()
                   .equals(mThreadOwner)) {
            Log.e(LOG_TAG, "setRenderer: This thread does not own the OpenGL context.");
            return;
        }

        // Call the renderer initialization routines
        mRenderer.onSurfaceCreated(mGL, mEGLConfig);
        mRenderer.onSurfaceChanged(mGL, mWidth, mHeight);
    }

    public Bitmap getBitmap() {
        // Do we have a renderer?
        if (mRenderer == null) {
            Log.e(LOG_TAG, "getBitmap: Renderer was not set.");
            return null;
        }

        // Does this thread own the OpenGL context?
        if (!Thread.currentThread()
                   .getName()
                   .equals(mThreadOwner)) {
            Log.e(LOG_TAG, "getBitmap: This thread does not own the OpenGL context.");
            return null;
        }

        // Call the renderer draw routine (it seems that some filters do not
        // work if this is only called once)
        mRenderer.onDrawFrame(mGL);
        mRenderer.onDrawFrame(mGL);
        convertToBitmap();
        return mBitmap;
    }

    public void destroy() {
        mRenderer.onDrawFrame(mGL);
        mRenderer.onDrawFrame(mGL);
        mEGL.eglMakeCurrent(mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

        mEGL.eglDestroySurface(mEGLDisplay, mEGLSurface);
        mEGL.eglDestroyContext(mEGLDisplay, mEGLContext);
        mEGL.eglTerminate(mEGLDisplay);
    }

    private EGLConfig chooseConfig() {
        int[] attribList =
                new int[]{EGL_DEPTH_SIZE, 0, EGL_STENCIL_SIZE, 0, EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8,
                          EGL_ALPHA_SIZE, 8, EGL10.EGL_RENDERABLE_TYPE, 4, EGL_NONE};

        // No error checking performed, minimum required code to elucidate logic
        // Expand on this logic to be more selective in choosing a configuration
        int[] numConfig = new int[1];
        mEGL.eglChooseConfig(mEGLDisplay, attribList, null, 0, numConfig);
        int configSize = numConfig[0];
        mEGLConfigs = new EGLConfig[configSize];
        mEGL.eglChooseConfig(mEGLDisplay, attribList, mEGLConfigs, configSize, numConfig);

        if (LIST_CONFIGS) {
            listConfig();
        }

        return mEGLConfigs[0]; // Best match is probably the first configuration
    }

    private void listConfig() {
        Log.i(LOG_TAG, "Config List {");

        for (EGLConfig config : mEGLConfigs) {
            int d, s, r, g, b, a;

            // Expand on this logic to dump other attributes
            d = getConfigAttrib(config, EGL_DEPTH_SIZE);
            s = getConfigAttrib(config, EGL_STENCIL_SIZE);
            r = getConfigAttrib(config, EGL_RED_SIZE);
            g = getConfigAttrib(config, EGL_GREEN_SIZE);
            b = getConfigAttrib(config, EGL_BLUE_SIZE);
            a = getConfigAttrib(config, EGL_ALPHA_SIZE);
            Log.i(LOG_TAG, "    <d,s,r,g,b,a> = <" + d + "," + s + "," +
                           r + "," + g + "," + b + "," + a + ">");
        }

        Log.i(LOG_TAG, "}");
    }

    private int getConfigAttrib(final EGLConfig config, final int attribute) {
        int[] value = new int[1];
        return mEGL.eglGetConfigAttrib(mEGLDisplay, config, attribute, value) ? value[0] : 0;
    }

    private void convertToBitmap() {
        int[] iat = new int[mWidth * mHeight];
        int[] ia;

        ia = this.readPixelsGLES2();

        //        Log.i(LOG_TAG, "GPUImage GLES version = " + GPUImage.sGLESVersion);
        //        if (GPUImage.sGLESVersion == 3) {
        //            ia = this.readPixelsGLES3();
        //        } else {
        //            ia = this.readPixelsGLES2();
        //        }

        // Convert upside down mirror-reversed image to right-side up normal
        // image.
        for (int i = 0; i < mHeight; i++) {
            ia[i] = ia[i];
        }
        for (int i = 0; i < mHeight; i++) {

            System.arraycopy(ia, i * mWidth, iat, (mHeight - i - 1) * mWidth, mWidth);
        }

        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(IntBuffer.wrap(iat));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int[] readPixelsGLES3() {

        long time = System.currentTimeMillis();

        //set framebuffer to read from
        GLES30.glReadBuffer(GLES30.GL_BACK);
        UtilsGL.logErrorGL("glReadBuffer back");

        Log.d(LOG_TAG, "1. glReadBuffer: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        // bind pbo
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mPboHandleContainer[0]);
        UtilsGL.logErrorGL("glBindBuffer pix pbo write");

        Log.d(LOG_TAG, "2. glBindBuffer: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();


        // read pixels(should be instant)
        GLES3PBOReadPixelsFix.glReadPixelsPBO(0, 0, mWidth, mHeight, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0);
        UtilsGL.logErrorGL("glReadPixels");

        Log.d(LOG_TAG, "3. glReadPixels: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();

        // map pbo to bb
        ByteBuffer byteBuffer =
                ((ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, 4 * mWidth * mHeight,
                                                      GLES30.GL_MAP_READ_BIT)).order(ByteOrder.nativeOrder());
        UtilsGL.logErrorGL("glMapBufferRange");

        Log.d(LOG_TAG, "4. glMapBufferRange: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();


        IntBuffer result = byteBuffer.asIntBuffer();

        int[] pixels;
        result.position(0);
        if (!result.hasArray()) {

            pixels = new int[result.capacity()];
            result.get(pixels);

        } else {
            pixels = result.array();
        }

        Log.d(LOG_TAG, "5. pixels IntBuffer -> int[]: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();


        // unmap pbo
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
        UtilsGL.logErrorGL("glUnmapBuffer pix");

        Log.d(LOG_TAG, "6. glUnmapBuffer pix: " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();


        // unbind pbo
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
        UtilsGL.logErrorGL("glBindBuffer pix 0 end");

        Log.d(LOG_TAG, "7. glBindBuffer pix 0: " + (System.currentTimeMillis() - time));

        return pixels;
    }

    private int[] readPixelsGLES2() {

        String GLESExtensions = mGL.glGetString(GL10.GL_EXTENSIONS);

        final IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);

        boolean hasBGRA = GLESExtensions.contains("GL_EXT_texture_format_BGRA8888");


        if(hasBGRA){
            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);// maybe don't need that
        }
        GLES20.glReadPixels(0, 0, mWidth, mHeight, hasBGRA ? GLES11Ext.GL_BGRA : GLES20.GL_RGB, GL_UNSIGNED_BYTE, ib);

        return ib.array();
    }

}