package com.wasu.arcface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import static com.wasu.arcface.DetecterActivity.DETECTER_RESULT_NO;
import static com.wasu.arcface.DetecterActivity.DETECTER_RESULT_OK;

public class HomeActivity extends Activity implements OnClickListener {
    public static final String IN_KEY = "isRegister";
    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;
    private static final int REQUEST_CODE_IMAGE_OP = 2;
    private static final int REQUEST_CODE_OP = 3;
    private static final int REQUEST_CODE_OP_DETECTER = 4;
    private final String TAG = this.getClass().toString();
    private Application application;
    private RegisterViewAdapter mRegisterViewAdapter;
    private ListView mHListView;
    private int BackId = -1;
    private int FrontId = -1;
    private boolean isRegister;

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main_test);
        isRegister = getIntent().getBooleanExtra(IN_KEY, false);
        device_check();
        if (!isRegister) {
            startDetector();
            return;
        }

        application = Application.getInstance(this);
        View v = this.findViewById(R.id.button1);
        v.setOnClickListener(this);
        v = this.findViewById(R.id.button2);
        v.setOnClickListener(this);

        mRegisterViewAdapter = new RegisterViewAdapter(this);
        mHListView = (ListView) findViewById(R.id.hlistView);
        mHListView.setAdapter(mRegisterViewAdapter);
        mHListView.setOnItemClickListener(mRegisterViewAdapter);
    }

    private void showCameraError() {
        Toast.makeText(getApplicationContext(), "未检测到摄像头", Toast.LENGTH_LONG).show();
        finish();
    }

    void device_check() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            BackId = 0;
            return;
        }
        try {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int cameras = Camera.getNumberOfCameras();
            if (cameras <= 0) {
                showCameraError();
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
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "未检测到摄像头", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMAGE_OP && resultCode == RESULT_OK) {
            Uri mPath = data.getData();
            String file = getPath(mPath);
            Bitmap bmp = Application.decodeImage(file);
            if (bmp == null || bmp.getWidth() <= 0 || bmp.getHeight() <= 0) {
                Log.e(TAG, "error");
            } else {
                Log.i(TAG, "bmp [" + bmp.getWidth() + "," + bmp.getHeight());
            }
            startRegister(bmp, file);
        } else if (requestCode == REQUEST_CODE_OP) {
            Log.i(TAG, "RESULT =" + resultCode);
            if (resultCode == RESULT_OK) {
                mRegisterViewAdapter.notifyDataSetChanged();
            }
            if (data == null) {
                return;
            }
            Bundle bundle = data.getExtras();
            String path = bundle.getString("imagePath");
            Log.i(TAG, "path=" + path);
        } else if (requestCode == REQUEST_CODE_OP_DETECTER) {
            Log.i(TAG, "RESULT =" + resultCode);
            if (isRegister) {
                if (resultCode == DETECTER_RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "比对通过!", Toast.LENGTH_SHORT).show();
                } else if (resultCode == DETECTER_RESULT_NO) {
                    Toast.makeText(getApplicationContext(), "比对失败!", Toast.LENGTH_SHORT).show();
                }
            } else {
                setResult(resultCode);
                finish();
            }
        } else if (requestCode == REQUEST_CODE_IMAGE_CAMERA && resultCode == RESULT_OK) {
//            Uri mPath = application.getCaptureImage();
//            String file = getPath(mPath);
            String file = application.getCameraPic();
            Bitmap bmp = Application.decodeImage(file);
            startRegister(bmp, file);
        }
    }

    @Override
    public void onClick(View paramView) {
        int i = paramView.getId();
        if (i == R.id.button2) {
            if (application.mFaceDB.mRegister.isEmpty()) {
                Toast.makeText(this, "没有注册人脸，请先注册！", Toast.LENGTH_SHORT).show();
            } else {
                startDetector();
                /*new AlertDialog.Builder(this)
                        .setTitle("请选择相机")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setItems(new String[]{"后置相机", "前置相机"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startDetector(which);
                            }
                        })
                        .show();*/
            }

        } else if (i == R.id.button1) {
            Intent intent = new Intent(HomeActivity.this, TextureCameraActivity.class);
            startActivityForResult(intent, REQUEST_CODE_IMAGE_CAMERA);
            new AlertDialog.Builder(this)
                    .setTitle("请选择注册方式")
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setItems(new String[]{"打开图片", "拍摄照片"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 1:

                                    Intent intent = new Intent(HomeActivity.this,TextureCameraActivity.class);
                                    startActivityForResult(intent, REQUEST_CODE_IMAGE_CAMERA);
                                    break;
                                case 0:
                                    Intent getImageByalbum = new Intent(Intent.ACTION_GET_CONTENT);
                                    getImageByalbum.addCategory(Intent.CATEGORY_OPENABLE);
                                    getImageByalbum.setType("image/jpeg");
                                    startActivityForResult(getImageByalbum, REQUEST_CODE_IMAGE_OP);
                                    break;
                                default:
                                    ;
                            }
                        }
                    })
                    .show();

        } else {
            ;
        }
    }

    /**
     * @param uri
     * @return
     */
    private String getPath(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                } else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(this, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(this, contentUri, selection, selectionArgs);
                }
            }
        }
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor actualimagecursor = this.getContentResolver().query(uri, proj, null, null, null);
        int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        actualimagecursor.moveToFirst();
        String img_path = actualimagecursor.getString(actual_image_column_index);
        String end = img_path.substring(img_path.length() - 4);
        if (0 != end.compareToIgnoreCase(".jpg") && 0 != end.compareToIgnoreCase(".png")) {
            return null;
        }
        return img_path;
    }

    /**
     * @param mBitmap
     */
    private void startRegister(Bitmap mBitmap, String file) {
        Intent it = new Intent(HomeActivity.this, RegisterActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("imagePath", file);
        it.putExtras(bundle);
        startActivityForResult(it, REQUEST_CODE_OP);
    }

    private void startDetector() {
        int dev = FrontId;
        if (dev == -1) {
            dev = BackId;
            if (dev == -1) {
                showCameraError();
                return; //no camera
            }
        }
        Intent it = new Intent(HomeActivity.this, DetecterActivity.class);
        it.putExtra("Camera", dev);
        startActivityForResult(it, REQUEST_CODE_OP_DETECTER);
    }

}

