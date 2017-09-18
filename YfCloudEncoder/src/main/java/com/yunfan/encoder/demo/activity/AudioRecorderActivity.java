/**
 * @版权 : 深圳云帆世纪科技有限公司
 * @作者 : 刘群山
 * @日期 : 2015年4月23日
 */
package com.yunfan.encoder.demo.activity;


import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.util.Log;
import com.yunfan.encoder.widget.YfEncoderKit;

import java.io.IOException;
import java.io.InputStream;

public class AudioRecorderActivity extends LiveRecorderActivity {
    private static final String TAG = "Yf_AudioRecorderActivity";
    private final static String CACHE_DIRS = Environment.getExternalStorageDirectory().getPath() + "/yunfanencoder";


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_audio_only_encoder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_start:
                startRecorder(true);
                break;
            case R.id.action_stop:
                stopRecorder();
                break;
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void initView() {
        setContentView(R.layout.activity_audio_only_recorder);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mUrl = ((EditText) findViewById(R.id.url));
        mUrl.setText(getDefaultUrl());
        mUrl.setSelection(getDefaultUrl().length());
        actionbarLayout = (LinearLayout) findViewById(R.id.actionbar_layout);
        initRecorder();
    }

    protected void initRecorder() {
        yfEncoderKit = new YfEncoderKit(this, CACHE_DIRS);
        yfEncoderKit.setRecordMonitor(this);

    }
    protected void startRecorder(boolean changeUI) {

        if (changeUI)
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        startRecorderInternal();

    }



    private void startRecorderInternal() {
        Log.d(TAG, "startRecorderInternal");
        yfEncoderKit.changeMode(YfEncoderKit.MODE_LIVE, -1);
        yfEncoderKit.setBufferSizeBySec(1);
        yfEncoderKit.enableUDP(mEnableUdp);
        yfEncoderKit.enableHttpDNS(mEnableHttpDns);
        yfEncoderKit.setLiveUrl(mUrl.getText().toString());
        yfEncoderKit.startRecord();
    }


}
