package com.wasu.arcfacetest;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.wasu.arcface.CameraActivity;
import com.wasu.arcface.DetectionPermissionActivity;
import com.wasu.arcface.HomeActivity;
import com.wasu.arcface.PermissionAcitivity;

import static com.wasu.arcface.DetecterActivity.DETECTER_RESULT_NO;
import static com.wasu.arcface.DetecterActivity.DETECTER_RESULT_OK;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void record(View view){

        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {

                // Open the permission page
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 11);
                return;
            }
        }

        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(HomeActivity.IN_KEY, false);
        startActivityForResult(intent, 11);
    }


    public void detection(View view) {
        Intent intent = new Intent(this, DetectionPermissionActivity.class);
        intent.putExtra(HomeActivity.IN_KEY, false);
        startActivityForResult(intent, 11);
    }


    public void compare(View view) {
        Intent intent = new Intent(this, PermissionAcitivity.class);
        intent.putExtra(HomeActivity.IN_KEY, false);
        startActivityForResult(intent, 11);
    }

    public void register(View view) {
        Intent intent = new Intent(this, PermissionAcitivity.class);
        intent.putExtra(HomeActivity.IN_KEY, true);
        startActivityForResult(intent, 11);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 11) {
            if (resultCode == DETECTER_RESULT_OK) {
                Toast.makeText(getApplicationContext(), "success!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == DETECTER_RESULT_NO) {
                Toast.makeText(getApplicationContext(), "比对失败!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
