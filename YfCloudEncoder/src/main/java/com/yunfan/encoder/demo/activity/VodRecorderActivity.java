/**
 * @版权 : 深圳云帆世纪科技有限公司
 * @作者 : 刘群山
 * @日期 : 2015年4月23日
 */
package com.yunfan.encoder.demo.activity;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import com.yunfan.encoder.widget.YfEncoderKit;

public class VodRecorderActivity extends LiveRecorderActivity {

    protected String getDefaultUrl() {
        return URL_VOD;
    }

    protected void startRecorder(boolean changeUI) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //android6.0权限机制
            checkPermission();
        } else {
            startRecorderInternal();
        }
    }

    protected void startRecorderInternal() {
        yfEncoderKit.changeMode(YfEncoderKit.MODE_VOD, mInputBitrate );
        yfEncoderKit.setVodSaveName(mUrl.getText().toString());
        yfEncoderKit.startRecord();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermission() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_FOR_WRITE_PERMISSION);
        } else {
            startRecorderInternal();
        }
    }

    private static final int CODE_FOR_WRITE_PERMISSION = 100;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODE_FOR_WRITE_PERMISSION) {
            if (permissions.length > 0 && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecorderInternal();
            } else {
                finish();
            }
        }
    }

}
