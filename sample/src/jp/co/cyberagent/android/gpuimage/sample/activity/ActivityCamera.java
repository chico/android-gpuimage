/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.sample.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage.OnPictureSavedListener;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.FilterAdjuster;
import jp.co.cyberagent.android.gpuimage.sample.GPUImageFilterTools.OnGpuImageFilterChosenListener;
import jp.co.cyberagent.android.gpuimage.sample.R;
import jp.co.cyberagent.android.gpuimage.sample.utils.CameraHelper;
import jp.co.cyberagent.android.gpuimage.sample.utils.CameraHelper.CameraInfo2;

public class ActivityCamera
        extends Activity
        implements OnSeekBarChangeListener, OnClickListener {

    private GPUImage       mGPUImage;
    private CameraHelper   mCameraHelper;
    private CameraLoader   mCamera;
    private GPUImageFilter mFilter;
    private FilterAdjuster mFilterAdjuster;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ((SeekBar) findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);
        findViewById(R.id.button_choose_filter).setOnClickListener(this);
        findViewById(R.id.button_capture).setOnClickListener(this);

        mGPUImage = new GPUImage(this);
        mGPUImage.setGLSurfaceView((GLSurfaceView) findViewById(R.id.surfaceView));

        mCameraHelper = new CameraHelper(this);
        mCamera = new CameraLoader();

        View cameraSwitchView = findViewById(R.id.img_switch_camera);
        cameraSwitchView.setOnClickListener(this);
        if (!mCameraHelper.hasFrontCamera() || !mCameraHelper.hasBackCamera()) {
            cameraSwitchView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera.onResume();
    }

    @Override
    protected void onPause() {
        mCamera.onPause();
        super.onPause();
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.button_choose_filter:
                GPUImageFilterTools.showDialog(this, new OnGpuImageFilterChosenListener() {

                    @Override
                    public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
                        switchFilterTo(filter);
                    }
                });
                break;

            case R.id.button_capture:
                if (mCamera.mCameraInstance.getParameters()
                                           .getFocusMode()
                                           .equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    takePicture();
                } else {
                    mCamera.mCameraInstance.autoFocus(new Camera.AutoFocusCallback() {

                        @Override
                        public void onAutoFocus(final boolean success, final Camera camera) {
                            takePicture();
                        }
                    });
                }
                break;

            case R.id.img_switch_camera:
                mCamera.switchCamera();
                break;
        }
    }

    private void takePicture() {
        // TODO get a size that is about the size of the screen
        Camera.Parameters params = mCamera.mCameraInstance.getParameters();
        params.setRotation(90);

        // Setting best quality of photo taken by camera(highest resolution & 100% jpeg quality)
        int resolutionMultiplied = 0;
        for (Camera.Size size : params.getSupportedPictureSizes()) {
//            Log.i("ASDF", "Supported: " + size.width + "x" + size.height);
            final int newResMult = size.width * size.height;
            if (newResMult > resolutionMultiplied) {
                resolutionMultiplied = newResMult;
                params.setPictureSize(size.width, size.height);
            }
        }
        Log.i("ASDF", "picture size: " + params.getPictureSize().width + "x" + params.getPictureSize().height);
        params.setJpegQuality(90);

        mCamera.mCameraInstance.setParameters(params);

        final long start = System.currentTimeMillis();

        mCamera.mCameraInstance.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, final Camera camera) {

                long timeTaken = (System.currentTimeMillis() - start);
                Log.i("ASDF", "mCamera.mCameraInstance.takePicture: " + timeTaken +
                              " millis");

                final File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null) {
                    Log.d("ASDF", "Error creating media file, check storage permissions");
                    return;
                }

                long timeTakenDiff = (System.currentTimeMillis() - start) - timeTaken;
                timeTaken = (System.currentTimeMillis() - start);
                Log.i("ASDF", "getOutputMediaFile: " + timeTakenDiff + " millis (" +
                              timeTaken + " millis in total)");

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d("ASDF", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("ASDF", "Error accessing file: " + e.getMessage());
                }

                timeTakenDiff = (System.currentTimeMillis() - start) - timeTaken;
                timeTaken = (System.currentTimeMillis() - start);
                Log.i("ASDF", "fos.write(data): " + timeTakenDiff + " millis (" +
                              timeTaken + " millis in total)");

                data = null;
                Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());

                timeTakenDiff = (System.currentTimeMillis() - start) - timeTaken;
                timeTaken = (System.currentTimeMillis() - start);
                Log.i("ASDF", "BitmapFactory.decodeFile: " + timeTakenDiff + " millis (" +
                              timeTaken + " millis in total)");

                // Enable if doing byte[] to bitmap directly
                //                        Bitmap bitmap = getBitmapFromBytes(data, 2448, 3264);
                //                        long timeTakenDiff = (System.currentTimeMillis() - start) - timeTaken;
                //                        timeTaken = (System.currentTimeMillis() - start);
                //                        Log.i("ASDF", "getBitmapFromBytes: " + timeTakenDiff + " millis (" + timeTaken + " millis in total)");

                // mGPUImage.setImage(bitmap);
                final GLSurfaceView view = (GLSurfaceView) findViewById(R.id.surfaceView);
                view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

                timeTakenDiff = (System.currentTimeMillis() - start) - timeTaken;
                timeTaken = (System.currentTimeMillis() - start);
                Log.i("ASDF", "setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY): " +
                              timeTakenDiff + " millis (" + timeTaken +
                              " millis in total)");

                final long timeTakenTemp = timeTaken;

                mGPUImage.saveToPicturesFast(bitmap, "GPUImage", System.currentTimeMillis() + ".jpg",
                                             new GPUImage.OnPictureRenderedListener() {

                                                 @Override
                                                 public void onPictureRendered() {
                                                     long timeTakenDiff =
                                                             (System.currentTimeMillis() - start) - timeTakenTemp;
                                                     long timeTaken = (System.currentTimeMillis() - start);
                                                     Log.i("ASDF", "onPictureRendered is called: " + timeTakenDiff +
                                                                   " millis (" + timeTaken +
                                                                   " millis in total)");


                                                     camera.startPreview();

                                                     timeTakenDiff = (System.currentTimeMillis() - start) - timeTaken;
                                                     timeTaken = (System.currentTimeMillis() - start);
                                                     Log.i("ASDF", "after camera.startPreview(): " + timeTakenDiff +
                                                                   " millis (" + timeTaken +
                                                                   " millis in total)");

                                                     view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

                                                     Log.i("ASDF", "onPictureRendered done: " + timeTaken + " millis");
                                                 }
                                             }, new OnPictureSavedListener() {

                            @Override
                            public void onPictureSaved(final Uri uri) {
                                pictureFile.delete();
                                long timeTaken = (System.currentTimeMillis() - start);
                                Log.i("ASDF", "All done: " + timeTaken + " millis");
                            }
                        });
            }
        });
    }

    public static Bitmap getBitmapFromBytes(byte[] content, int width, int height) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(content, 0, content.length, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, width, height);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeByteArray(content, 0, content.length, options);

        } catch (Exception e) {
            Log.e("ASDF", "Failed to get bitmap - " + e.getMessage());
            return null;
        }
    }

    protected static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static File getOutputMediaFile(final int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir =
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                                 "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                                 "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null || (filter != null && !mFilter.getClass()
                                                          .equals(filter.getClass()))) {
            mFilter = filter;
            mGPUImage.setFilter(mFilter);
            mFilterAdjuster = new FilterAdjuster(mFilter);
        }
    }

    @Override
    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
        if (mFilterAdjuster != null) {
            mFilterAdjuster.adjust(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(final SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
    }

    private class CameraLoader {

        private int mCurrentCameraId = 0;
        private Camera mCameraInstance;

        public void onResume() {
            setUpCamera(mCurrentCameraId);
        }

        public void onPause() {
            releaseCamera();
        }

        public void switchCamera() {
            releaseCamera();
            mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
            setUpCamera(mCurrentCameraId);
        }

        private void setUpCamera(final int id) {
            mCameraInstance = getCameraInstance(id);
            Parameters parameters = mCameraInstance.getParameters();
            // TODO adjust by getting supportedPreviewSizes and then choosing
            // the best one for screen size (best fill screen)
            if (parameters.getSupportedFocusModes()
                          .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            mCameraInstance.setParameters(parameters);

            int orientation = mCameraHelper.getCameraDisplayOrientation(ActivityCamera.this, mCurrentCameraId);
            CameraInfo2 cameraInfo = new CameraInfo2();
            mCameraHelper.getCameraInfo(mCurrentCameraId, cameraInfo);
            boolean flipHorizontal = cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
            mGPUImage.setUpCamera(mCameraInstance, orientation, flipHorizontal, false);
        }

        /** A safe way to get an instance of the Camera object. */
        private Camera getCameraInstance(final int id) {
            Camera c = null;
            try {
                c = mCameraHelper.openCamera(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return c;
        }

        private void releaseCamera() {
            mCameraInstance.setPreviewCallback(null);
            mCameraInstance.release();
            mCameraInstance = null;
        }
    }
}
