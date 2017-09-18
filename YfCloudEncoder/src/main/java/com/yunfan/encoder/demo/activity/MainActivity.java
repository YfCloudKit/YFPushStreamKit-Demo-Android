package com.yunfan.encoder.demo.activity;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yfcloudencoder.BuildConfig;
import com.example.yfcloudencoder.R;
import com.yunfan.auth.YfAuthentication;
import com.yunfan.auth.internal.YfAuthenticationInternal;
import com.yunfan.encoder.demo.util.Util;
import com.yunfan.encoder.demo.widget.AudioSelectDialog;
import com.yunfan.encoder.widget.YfEncoderKit;

import static android.os.Build.VERSION_CODES.M;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String ACCESS_KEY = "d5ff42e55cf8400cf0ba67cff8e69f425718d702";
    private static final String TOKEN = "1f7cdfe73aa0babd94cda16be6d87ecc39cab7e0";
    public static final int DEFAULT_BITRATE = 600;
    public static final int DEFAULT_BITRATE_SEGMENT = 5000;
    private Switch enableLandscape, mEnableHardEncoder, mUdp, mHttpDns;
    private EditText bitrateEditor;
    private final int REQUEST_CODE = 1;
    private static final Class[] CLASSES = {
//            FilterPlayerActivity.class,
            LiveRecorderActivity.class,
//            VodRecorderActivity.class,
            AudioRecorderActivity.class,
            SegmentRecordActivity.class
    };
    private int CODE_FOR_READ_EXTERNAL = 0x1001;
    private AudioSelectDialog mAudioSelectDialog;
    private ListView mL;
    private final String PATH = "/sdcard/yunfanencoder/audio";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YfAuthentication.getInstance().authenticate(ACCESS_KEY,TOKEN, authCallBack);
        if (android.os.Build.VERSION.SDK_INT >= M) {
            checkEncoderPermission();
        } else {
            initView();
        }
        copyMp3ToSdcard();
    }

    private void copyMp3ToSdcard() {
        String[] musics = getResources().getStringArray(R.array.music_name);
        boolean copy = Util.copyAssetsFileToSD(MainActivity.this, PATH, musics, getResources().getStringArray(R.array.music_saved_name));
        Log.d(TAG, "copyAssetsFileToSD: " + copy);
    }

    private YfAuthentication.AuthCallBack authCallBack = new YfAuthentication.AuthCallBack() {
        @Override
        public void onAuthenticateSuccess() {
            Log.d(TAG, "鉴权成功~！");
        }

        @Override
        public void onAuthenticateError(int errorCode) {
            Log.d(TAG, "鉴权失败啦：" + errorCode);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        enableLandscape = (Switch) findViewById(R.id.enableLandscape);
        mEnableHardEncoder = (Switch) findViewById(R.id.enableHardEncoder);
        mUdp = (Switch) findViewById(R.id.udp);
        mHttpDns = (Switch) findViewById(R.id.http_dns);
        bitrateEditor = (EditText) findViewById(R.id.bitrate);
        bitrateEditor.setHint(String.format("请设置码率，默认值为 %d ,分段录制为 %d ：", DEFAULT_BITRATE, DEFAULT_BITRATE_SEGMENT));
        mL = (ListView) findViewById(R.id.list);
        mL.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (position == 0) {
                    if (!YfAuthentication.getInstance().isAuthenticateSucceed()) {
                        Toast.makeText(MainActivity.this, "鉴权未通过，不能推流，开始重新鉴权~", Toast.LENGTH_SHORT).show();
                        YfAuthentication.getInstance().authenticate(ACCESS_KEY,TOKEN, authCallBack);
                        return;
                    }
                }
                if (position < CLASSES.length - 1) {
                    Intent i = new Intent(MainActivity.this, CLASSES[position]);
                    i.putExtra(LiveRecorderActivity.ENABLE_HARD_ENCODER, mEnableHardEncoder.isChecked());
                    i.putExtra(LiveRecorderActivity.ORIENTATION, enableLandscape.isChecked());
                    i.putExtra(LiveRecorderActivity.ENABLE_UDP, mUdp.isChecked());
                    i.putExtra(LiveRecorderActivity.ENABLE_HTTP_DNS, mHttpDns.isChecked());
                    try {
                        i.putExtra(LiveRecorderActivity.CUSTOM_BITRATE, Integer.parseInt(bitrateEditor.getText().toString()));
                    } catch (NumberFormatException e) {
                        i.putExtra(LiveRecorderActivity.CUSTOM_BITRATE, DEFAULT_BITRATE);
                    }
                    startActivity(i);
                } else if (position == CLASSES.length - 1) {
                    showDialog();
                }
            }
        });

        mL.setAdapter(new Adapter(this, CLASSES));

        ((TextView)findViewById(R.id.version)).setText("v"+ BuildConfig.VERSION_NAME+"_"+ YfEncoderKit.getSDKVersion());
    }

    private static final int CODE_FOR_OPEN_CAMERA = 100;

    @TargetApi(M)
    private void checkEncoderPermission() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                | checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                | checkSelfPermission(Manifest.permission.CAMERA)| checkSelfPermission(Manifest.permission.RECORD_AUDIO)| checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE}, CODE_FOR_OPEN_CAMERA);
        } else {
            initView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODE_FOR_OPEN_CAMERA) {
            if (permissions[0].equals(Manifest.permission.CAMERA)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && permissions[1].equals(Manifest.permission.RECORD_AUDIO)
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && permissions[2].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED
                    && permissions[3].equals(Manifest.permission.READ_EXTERNAL_STORAGE)
                    && grantResults[3] == PackageManager.PERMISSION_GRANTED
                    && permissions[4].equals(Manifest.permission.READ_PHONE_STATE)
                    && grantResults[4] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用camera
                initView();
            } else {
                finish();
            }
        }
    }


    private void showDialog() {
        if (mAudioSelectDialog == null) {
            mAudioSelectDialog = new AudioSelectDialog(MainActivity.this);
            mAudioSelectDialog.setOnItemClickListener(mItemClickListener);
        }
        mAudioSelectDialog.show();
    }

    private AudioSelectDialog.ItemClickListener mItemClickListener
            = new AudioSelectDialog.ItemClickListener() {
        @Override
        public void onItemClickListener(String path) {
            Intent intent = new Intent(MainActivity.this, SegmentRecordActivity2.class);
            intent.putExtra(SegmentRecordActivity.AUDIO_PATH, path);
            intent.putExtra(LiveRecorderActivity.ENABLE_HARD_ENCODER, mEnableHardEncoder.isChecked());
            intent.putExtra(LiveRecorderActivity.ORIENTATION, enableLandscape.isChecked());
            intent.putExtra(LiveRecorderActivity.ENABLE_UDP, mUdp.isChecked());
            intent.putExtra(LiveRecorderActivity.ENABLE_HTTP_DNS, mHttpDns.isChecked());
            try {
                intent.putExtra(LiveRecorderActivity.CUSTOM_BITRATE, Integer.parseInt(bitrateEditor.getText().toString()));
            } catch (NumberFormatException e) {
                intent.putExtra(LiveRecorderActivity.CUSTOM_BITRATE, DEFAULT_BITRATE_SEGMENT);
            }
            startActivity(intent);
            mAudioSelectDialog.dismiss();
        }
    };

    class Adapter extends ArrayAdapter<Class> {

        public Adapter(Context context, Class[] classes) {
            super(context, R.layout.item_class, R.id.class_name, classes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final TextView v = (TextView) super.getView(position, convertView,
                    parent);
            v.setText(CLASSES[position].getSimpleName());
            return v;
        }

    }


}
