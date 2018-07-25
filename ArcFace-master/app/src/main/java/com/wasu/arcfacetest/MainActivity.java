package com.wasu.arcfacetest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.icu.lang.UCharacter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.wasu.arcface.DetectionPermissionActivity;
import com.wasu.arcface.FaceDetectionService;
import com.wasu.arcface.HomeActivity;
import com.wasu.arcface.PermissionAcitivity;

import static com.wasu.arcface.DetecterActivity.DETECTER_RESULT_NO;
import static com.wasu.arcface.DetecterActivity.DETECTER_RESULT_OK;

public class MainActivity extends AppCompatActivity {

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ((action.equals("Welcome"))) {
                Toast.makeText(getApplicationContext(), "Welcome", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    public void onTakePicture(View view) {
        startService(new Intent(this, FaceDetectionService.class));
    }

    public void onStopService(View view) {

        stopService(new Intent(this, FaceDetectionService.class));

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
