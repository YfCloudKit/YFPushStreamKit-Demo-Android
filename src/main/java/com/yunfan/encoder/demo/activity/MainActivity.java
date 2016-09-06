package com.yunfan.encoder.demo.activity;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yfcloudencoder.R;
import com.yunfan.encoder.widget.EncoderAuthentication;


public class MainActivity extends Activity {
    private static final String TOKEN = "7b90e88fad84de9e1333d5dd576093093ef4ae6d";
    private static final String TAG = "MainActivity";

    private Switch enableFilter, enableLandscape, mEnableHardEncoder;
    private EditText bitrateEditor;
    private static final Class[] CLASSES = {
            LiveRecorderActivity.class, VodRecorderActivity.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EncoderAuthentication.getInstance().authenticate(TOKEN, authCallBack);
        initView();
    }

    private EncoderAuthentication.AuthCallBack authCallBack = new EncoderAuthentication.AuthCallBack() {
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
        enableFilter = (Switch) findViewById(R.id.enableFilter);
        enableLandscape = (Switch) findViewById(R.id.enableLandscape);
        mEnableHardEncoder = (Switch) findViewById(R.id.enableHardEncoder);

        enableFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    //滤镜模式只能在硬编模式下使用
                    mEnableHardEncoder.setChecked(true);
                }
            }
        });
        mEnableHardEncoder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked){
                    //滤镜优先级高于软遍，使用软编必须禁止允许滤镜模式
                    enableFilter.setChecked(false);
                }
            }
        });
        bitrateEditor = (EditText) findViewById(R.id.bitrate);
        final ListView l = (ListView) findViewById(R.id.list);
        l.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if(position==0){
                    if(!EncoderAuthentication.getInstance().isAuthenticateSucceed()){
                        Toast.makeText(MainActivity.this, "鉴权未通过，不能推流，开始重新鉴权~", Toast.LENGTH_SHORT).show();
                        EncoderAuthentication.getInstance().authenticate(TOKEN,authCallBack);
                        return;
                    }
                }
                Intent i = new Intent(MainActivity.this, CLASSES[position]);
                i.putExtra(LiveRecorderActivity.ENABLE_FILTER, enableFilter.isChecked());
                i.putExtra(LiveRecorderActivity.ENABLE_HARD_ENCODER, mEnableHardEncoder.isChecked());
                i.putExtra(LiveRecorderActivity.ORIENTATION, enableLandscape.isChecked());
                try {
                    i.putExtra(LiveRecorderActivity.CUSTOM_BITRATE, Integer.parseInt(bitrateEditor.getText().toString()));
                } catch (NumberFormatException e) {

                }
                startActivity(i);
            }
        });

        l.setAdapter(new Adapter(this, CLASSES));
    }

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
