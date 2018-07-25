package com.wasu.arcface;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.FaceDetector;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FaceDetectionService extends Service {
    public static final String EXTRA_CAM_INDEX = "camera_index";
    private static final String TAG = "FaceDetectionService";
    private volatile boolean isRunning = false;
    long counter = 0;
    int current = 0;


    private static final int FT_SCALE = 16;
    private static final int FT_MAX_FACE_NUM = 3;
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    private static final String APPID = "5jv873H5yTUVYFHFykzbqqBoNPbs7DU1VssUYi1ob49f";
    private static final String KEY = "7RJAa3NB1xtpWmyWP9iX42L1hFoh7bc389QTeMDSLgcM";
    List<AFT_FSDKFace> result = new ArrayList<>();

    private int camIndex = -1;

    private volatile int framesCount = 10;
    private volatile boolean pitureCommandExecuted = false;

    private Camera mCamera;

    public FaceDetectionService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Staring camera service");

        Bundle extra = intent.getExtras();
        if (extra != null && extra.containsKey(EXTRA_CAM_INDEX)) {
            camIndex = extra.getInt(EXTRA_CAM_INDEX);
        }

        if (!isRunning) {
            takePhoto();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void takePhoto() {

        isRunning = true;

        System.out.println("Preparing to take photo");
        SystemClock.sleep(1000);

        if (camIndex != -1) {
            mCamera = Camera.open(camIndex);
        } else {
            mCamera = Camera.open(0);
        }
        try {
            if (null == mCamera) {
                System.out.println("Could not get camera instance");
            } else {
                System.out.println("Got the camera, creating the dummy surface texture");
                //SurfaceTexture dummySurfaceTextureF = new SurfaceTexture(0);
                try {

                    // set preview size to maximum
                    Camera.Parameters params = mCamera.getParameters();
                    List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
//                    Camera.Size prevSize = getBigSize(supportedPreviewSizes);
                    Camera.Size prevSize = getOptimalSize(params, 640, 480);
                    params.setPreviewSize(prevSize.width, prevSize.height);

                    List<Camera.Size> supportedPictureSizes = params.getSupportedPictureSizes();
                    Camera.Size picSize = getBigSize(supportedPictureSizes);
                    params.setPictureSize(picSize.width, picSize.height);

                    AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(APPID, KEY, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, FT_SCALE, FT_MAX_FACE_NUM);
                    Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());

                    mCamera.setParameters(params);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        mCamera.enableShutterSound(false);
                    }

                    //camera.setPreviewTexture(dummySurfaceTextureF);
                    SurfaceTexture sf = new SurfaceTexture(0);
                    sf.setDefaultBufferSize(prevSize.width, prevSize.height);
                    mCamera.setPreviewTexture(new SurfaceTexture(0));
                    //<editor-fold desc="PreviewCallback for camera">

                    mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] bytes, Camera camera) {
                            Camera.Size previewSize = camera.getParameters().getPreviewSize();
                            int width = previewSize.width;
                            int height = previewSize.height;
                            YuvImage data = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
                            bytes = data.getYuvData();



                            Log.d(TAG, "onPictureTaken!  " + Arrays.toString(bytes));
                            AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(bytes, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
                            Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());

                            int max = 0;
                            if (result == null) {
                                Log.d("result", "is null");
                            }
                            else {
                                Log.d("result", "is: " + result.size());
                            }

                            for (AFT_FSDKFace face : result) {
                                if (face.getRect().right - face.getRect().left >= max) {
                                    max = face.getRect().right - face.getRect().left;
                                }
                                Log.d(TAG, "**********************************************************Face:" +(face.getRect().right - face.getRect().left) + "*******" + (face.getRect().bottom - face.getRect().top));
                            }

                            counter++;
                            Log.d(TAG, "**********************************************************counter: " + counter);


                            //copy rects
                            Rect[] rects = new Rect[result.size()];
                            for (int i = 0; i < result.size(); i++) {
                                rects[i] = new Rect(result.get(i).getRect());
                            }
                            //clear result.
                            result.clear();
                            //return the rects for render.

                            if (max > 100 && max >= current && counter >= 30) {
                                // TODO: broadcast

                                Intent intent = new Intent();
                                intent.setAction("Welcome");
                                intent.putExtra("Welcome", 1);
                                sendBroadcast(intent);

                                Log.d("TEST", "welcome!");
                                counter = 0;
                            }
                            else if (counter >= 30) {
                                counter = 0;
                                current = 0;
                            }
                            current = max;

                        }
                    });
                    //</editor-fold>
                    mCamera.startPreview();
                } catch (Exception e) {
                    System.out.println("Could not set the surface preview texture");
                    e.printStackTrace();
                }

                Thread.sleep(100);
                //<editor-fold desc="Camera take picture call back">
//                mCamera.takePicture(null, null, new Camera.PictureCallback() {
//
//                    @Override
//                    public void onPictureTaken(byte[] data, Camera camera) {
//                        File pictureFileDir = getDirectory();
//                        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
//                            return;
//                        }
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
//                        String date = dateFormat.format(new Date());
//                        String photoFile = "PictureFront_" + camIndex + "_" + date + ".jpg";
//                        String filename = pictureFileDir.getPath() + File.separator + photoFile;
//                        File mainPicture = new File(filename);
//
//                        try {
//                            FileOutputStream fos = new FileOutputStream(mainPicture);
//                            fos.write(data);
//                            fos.close();
//                            System.out.println("image saved");
//                        } catch (Exception error) {
//                            System.out.println("Image could not be saved");
//                        }
//                        camera.release();
//                        isRunning = false;
//                        stopSelf();
//                    }
//                });
                //</editor-fold>
            }
        } catch (Exception e) {
            e.printStackTrace();
            mCamera.release();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: stopping service");
        isRunning = false;
        super.onDestroy();
        mCamera.release();

    }

    private Camera.Size getBigSize(List<Camera.Size> sizes) {
        Camera.Size big = sizes.get(0);

        for (Camera.Size s : sizes) {
            if (s.height > big.height || s.width > big.width) {
                big = s;
            }
        }

        return big;
    }

    private Camera.Size getOptimalSize(Camera.Parameters params, int w, int h) {

        List<Camera.Size> sizes = params.getSupportedPreviewSizes();

        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}