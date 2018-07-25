package com.wasu.arcface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextureCameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private Camera mCamera;
    private TextureView mTextureView;
    private int BackId = -1, FrontId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture);
        device_check();
        startCamera();
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextureView.setSurfaceTextureListener(this);

    }

    /**
     * 检测摄像头
     */
    void device_check() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            BackId = 0;
            return;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameras = Camera.getNumberOfCameras();
        if (cameras <= 0) {
            Toast.makeText(getApplicationContext(), "未获取到摄像头", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        for (int i = 0; i < cameras; i++) {
            Camera.getCameraInfo(i, info);
            switch (info.facing) {
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    FrontId = i;
                    continue;

                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    BackId = i;
                    continue;
            }
        }
    }

    /**
     * 优先前置，如果无前置摄像头，使用后置
     */
    private void startCamera() {
        mCamera = openCamera(0);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//        mCamera = Camera.open();

        if (null == mCamera) {
            return;
        }
        try {
            setParameter();

            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                }
            });
        } catch (IOException ioe) {
            ioe.printStackTrace();
            // Something bad happened
        }
    }

    private void setParameter() {
        /*
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        for (Camera.Size size : supportedPictureSizes) {
            Log.i("TAG", "size.width:" + size.width + " size.height:" + size.height);
        }

        int index = supportedPictureSizes.get(0).width > supportedPictureSizes.get(supportedPictureSizes.size() - 1).width ? 0 : supportedPictureSizes.size() - 1;
        parameters.setPreviewSize(supportedPictureSizes.get(index).width, supportedPictureSizes.get(index).height);
        if (supportedPictureSizes.get(index).width < 1280) {
            parameters.setPictureSize(supportedPictureSizes.get(index).width, supportedPictureSizes.get(index).height);
        }else{
            parameters.setPictureSize(1280, 720);
        }

        ViewGroup.LayoutParams layoutParams = mTextureView.getLayoutParams();
        layoutParams.width = supportedPictureSizes.get(0).width;
        layoutParams.height = supportedPictureSizes.get(0).height;
        mTextureView.setLayoutParams(layoutParams);

        parameters.setPictureFormat(ImageFormat.JPEG);
        mCamera.setParameters(parameters);

        Log.e("TAG", mCamera.getParameters().getPreviewSize().width + "==========" + mCamera.getParameters().getPreviewSize().height);
        */
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public Camera openCamera(int cameraId) {
        try {
            return Camera.open(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //拍照
    public void takePhoto(View view) {
        if (!isCanClick()) {
            return;
        }
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //将字节数组
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                //输出流保存数据
                try {
                    String face = getSdcardCacheDir("face") + File.separator + System.currentTimeMillis() + ".jpg";
                    File file = new File(face);
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(face);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, fileOutputStream);
                    bitmap.recycle();
                    camera.stopPreview();
                    Application.getInstance(getApplicationContext()).setCameraPic(face);
                    Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private long lastClickTime;

    public boolean isCanClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime;
        if (0 < timeD && timeD < 5000) {
            return false;
        }
        lastClickTime = time;
        return true;
    }

    public File getSdcardCacheDir(String uniqueName) {
        String cachePath;

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())
                && !Environment.isExternalStorageRemovable()) {
            if (getExternalCacheDir() != null) {
                cachePath = getExternalCacheDir().getPath();
            } else {
                cachePath = getCacheDir().getPath();
            }
        } else {
            cachePath = getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }
}
