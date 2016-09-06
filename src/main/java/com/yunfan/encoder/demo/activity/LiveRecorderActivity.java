package com.yunfan.encoder.demo.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.util.DeviceUtil;
import com.yunfan.encoder.demo.util.Log;
import com.yunfan.encoder.demo.util.LogRecoder;
import com.yunfan.encoder.widget.YfEncoderKit;

public class LiveRecorderActivity extends AppCompatActivity implements YfEncoderKit.RecordMonitor {
    protected static final String TAG = "YfRecorder_Live";
    public static final String ORIENTATION = "orientation";
    public static final String ENABLE_FILTER = "enableFilter";
    public static final String ENABLE_HARD_ENCODER = "enableHardEncoder";
    public static final String CUSTOM_BITRATE = "bitrate";

    // 默认的直播发起url
//    public static String URL_LIVE = "rtmp://192.168.3.138/mytv/";
//    public static String URL_LIVE = "rtmp://live.live3721.com/mytv/grand110";
    public static String URL_LIVE = "rtmp://push.yftest.yflive.net/live/test111";
//    public static String URL_LIVE = "rtmp://yf.push.cztv.com/live/627f95ea169e8f2fc254d2ff5a1c9875_540p";
//    public static String URL_LIVE = "rtmp://publish.langlive.com/live/huang";
//        public static String URL_LIVE = "rtmp://";
    //114.215.182.69:1935
    //172.17.11.233:1936/test/yunfan

    // 默认的录制文件存放目录
    public static String URL_VOD = "测试视频";
    //设置保存截图等文件的文件夹
    public static String CACHE_DIRS = Environment.getExternalStorageDirectory().getPath() + "/yunfanencoder";
    protected YfEncoderKit yfEncoderKit;
    protected boolean setBeauty = false;
    protected boolean setLogo = false;
    protected boolean dataShowing = false;
    protected boolean enableVideo = true;
    protected boolean enableAudio = true;
    protected EditText mUrl;
    private boolean startRecoderAuto = true;
    private boolean flashIsOn = false;
    private GLSurfaceView mTextureView;
    private LinearLayout actionbarLayout, infoLayout;
    private TextView textBitrate, textBuffer;
    private ActionBar actionBar;
    private int surfaceWidth, surfaceHeight;
    private boolean mLandscape = false;
    private boolean mEnableFilter = true;
    private boolean mEnableHardEncoder = true;
    private int inputBitrate;
    /**
     * 安卓4.3以下版本或不开启滤镜支持的情况下，视频编码宽高必须均小于摄像头输出宽高，编码器会自动进行裁剪；
     * <p>
     * 安卓4.3及以上版本并开启滤镜支持的情况下，视频编码宽高只需要与摄像头输出宽高比例一致即可，编码器会自动缩放；
     */
    protected static final int VIDEO_WIDTH = 640;
    protected static final int VIDEO_HEIGHT = 368;
    protected static final int VIDEO_FRAME_RATE = 24;

    protected int PREVIEW_WIDTH;
    protected int PREVIEW_HEIGHT;
    protected boolean HARD_ENCODER = true;
    protected int VIDEO_BITRATE = 1 * 1024;

    protected LogRecoder logRecoder = new LogRecoder();
    private Handler infoShower = new Handler();
    private int mCurrentBitrate, mCurrentBufferMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLandscape = getIntent().getBooleanExtra(ORIENTATION, false);
        mEnableFilter = getIntent().getBooleanExtra(ENABLE_FILTER, true);
        HARD_ENCODER = getIntent().getBooleanExtra(ENABLE_HARD_ENCODER, true);
        inputBitrate = getIntent().getIntExtra(CUSTOM_BITRATE, 1 * 1024);
        Log.d(TAG, "自定义码率:" + inputBitrate);
        VIDEO_BITRATE = inputBitrate;
        mEnableFilter = mEnableFilter && YfEncoderKit.canUsingFilter();

        if (mEnableFilter) {
            PREVIEW_WIDTH = 1280;
            PREVIEW_HEIGHT = 720;
        } else {
            PREVIEW_WIDTH = 640;
            PREVIEW_HEIGHT = 480;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPerssion();
        } else {
            initView();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_encoder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_start:
                startRecorder();
                break;
            case R.id.action_stop:
                stopRecorder();
                break;
            case R.id.action_close_capture:
                yfEncoderKit.captureCurrentFrame(System.currentTimeMillis() + "");
                break;
            case R.id.action_switch:
                yfEncoderKit.switchCamera();
                break;
            case R.id.action_torch:
                 yfEncoderKit.setFlash(!yfEncoderKit.isFlashOn());
//                openVideo();
                break;
            case R.id.action_enable_audio:
                enableAudio = !enableAudio;
                yfEncoderKit.enalePushAudio(enableAudio);
                Toast.makeText(this, (enableAudio ? "恢复" : "暂停") + "推送音频流", Toast.LENGTH_LONG).show();
                break;
            case R.id.action_set_beauty:
                if (mEnableFilter) {
                    if (!setBeauty) {
                        yfEncoderKit.addFilter(YfEncoderKit.YfFilterType.BEAUTY);
                    } else {
                        yfEncoderKit.removeFilter(YfEncoderKit.YfFilterType.BEAUTY);
                    }
                    setBeauty = !setBeauty;
                } else {
                    Toast.makeText(this, "滤镜只支持安卓4.3版本以上系统", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.action_set_logo:
                if (mEnableFilter) {
                    if (!setLogo) {
                        yfEncoderKit.addFilter(YfEncoderKit.YfFilterType.LOGO);
                    } else {
                        yfEncoderKit.removeFilter(YfEncoderKit.YfFilterType.LOGO);
                    }
                    setLogo = !setLogo;
                } else {
                    Toast.makeText(this, "滤镜只支持安卓4.3版本以上系统", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.action_show_data:
                if (dataShowing) {
                    infoShower.removeCallbacks(updateDisplay);
                    infoLayout.setVisibility(View.GONE);
                } else {
                    infoShower.removeCallbacks(updateDisplay);
                    infoShower.postDelayed(updateDisplay, 1000);
                    infoLayout.setVisibility(View.VISIBLE);
                }
                dataShowing = !dataShowing;
                break;
            case R.id.action_show_history:
                showLogs();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "on resume!");
        if (yfEncoderKit != null)
            yfEncoderKit.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        destroyRecorder();
    }

    private void toggleActionBar() {
        if (actionBar.isShowing()) {
            actionBar.hide();
            mUrl.setVisibility(View.GONE);
        } else {
            actionBar.show();
            mUrl.setVisibility(View.VISIBLE);
        }
    }

    protected void initView() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_recorder);
        infoLayout = (LinearLayout) findViewById(R.id.cache_info_layout);
        textBitrate = (TextView) findViewById(R.id.current_bitrate);
        textBuffer = (TextView) findViewById(R.id.current_buffer_size_ms);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        mUrl = ((EditText) findViewById(R.id.url));
        mUrl.setText(getDefaultUrl());
        mUrl.setSelection(getDefaultUrl().length());
        actionbarLayout = (LinearLayout) findViewById(R.id.actionbar_layout);
        mTextureView = (GLSurfaceView) findViewById(R.id.surface);
        mTextureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                toggleActionBar();
                return false;
            }
        });
//        if (!mEnableFilter)
        setSurfaceSize(mLandscape);
        initRecorder(mTextureView);
    }

    /**
     * 4.3以下版本或不允许滤镜模式的情况下，不支持视频压缩方法；
     * 因此为了保证同样码率下视频质量更清晰，分辨率要选择尽可能低的数值，640*480为一般选取的宽高；
     * 推送出去的画面可以经过sdk提供的方法进行裁剪（如640*480裁剪为640*360），
     * 而为了保证预览界面与推送的画面保持一致，必须对预览的surface进行一定的“遮挡"。
     *
     * @param landscape 是否为横屏模式
     */
    private void setSurfaceSize(boolean landscape) {
        mLandscape = landscape;
        LayoutParams lp = mTextureView.getLayoutParams();
        int realScreenWidth = DeviceUtil.getScreenWidth(this);
        if (landscape) {
            surfaceWidth = realScreenWidth * 16 / 9;
            surfaceHeight = mEnableFilter ? surfaceWidth * 9 / 16 : surfaceWidth * 3 / 4;
//            surfaceHeight = mEnableFilter ? surfaceWidth * 3 / 4 : surfaceWidth * 3 / 4;
        } else {
            surfaceHeight = realScreenWidth * 16 / 9;
            //考虑到高度可能被内置虚拟按键占用，因此为了保证预览界面为16:9，不能直接获取高度。
            surfaceWidth = mEnableFilter ? surfaceHeight * 9 / 16 : surfaceHeight * 3 / 4;
//            surfaceWidth = mEnableFilter ? surfaceHeight * 3 / 4 : surfaceHeight * 3 / 4;
            //不允许滤镜模式下推640*480分辨率的流，不是16:9，要进行裁剪
        }

        lp.width = surfaceWidth;
        lp.height = surfaceHeight;
        Log.d(TAG, "计算出来的宽高:" + surfaceWidth + "___" + surfaceHeight);
        mTextureView.setLayoutParams(lp);
    }

    private void initRecorder(GLSurfaceView s) {
        Log.d(TAG, "初始化编码器");
        //初始化编码工具：context、截图/录制视频等文件保存的根目录、允许开启滤镜、摄像头输出宽度、摄像头输出高度
        //允许开启滤镜模式下后台只能推送音频、且无法使用软编
        yfEncoderKit = new YfEncoderKit(this, CACHE_DIRS, mEnableFilter, PREVIEW_WIDTH, PREVIEW_HEIGHT, VIDEO_FRAME_RATE);
        yfEncoderKit.setContinuousFocus()//设置连续自动对焦
                .setLandscape(mLandscape)//设置是否横屏模式（默认竖屏）
                .setRecordMonitor(this)//设置回调
                .setDefaultCamera(false)//设置默认打开后置摄像头---不设置也默认打开后置摄像头
//                .configLogo(BitmapFactory.decodeResource(getResources(), R.mipmap.logo), 0.2f, 0.2f*9/16, 0.0f, 0.0f)
                .openCamera(s);//设置预览窗口
//        yfEncoderKit.setFilter(YfEncoderKit.YfFilterType.BEAUTY);//默认打开滤镜
//        setFilter = true;
        float landscapeMarginRight = 0.1f;//横屏模式下logo的marginright所占宽度的比例
        float portMarginRight = 0.05f;//竖屏模式下logo的marginright所占宽度的比例
        float landsapeMarginTop = 0.05f;//横屏模式下logo的marginTop所占宽度的比例
        float portMarginTop = 0.1f;//竖屏模式下logo的marginTop所占宽度的比例
        float landscapeLogoHeight = 0.2f;//横屏模式下logo的高度所占屏幕高度的比例
        float logoWidth = 454, logoHeight = 160;//计算logo的比例
        if (mLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            /**
             * 配置logo的源及在画面中的位置，请注意屏幕的横屏竖屏模式及屏幕比例
             * 需要注意的是在有内置虚拟键的情况下屏幕比例并不是16:9
             * 这里不对该情况进行处理，仅考虑一般的16:9的情况。
             * @param bitmap logo源
             * @param widthPercent logo的宽度占屏幕宽度的比例（0~1）
             * @param heightPercent logo的高度占屏幕高度的比例（0~1），譬如宽度设置为0.2f，那么在通常16:9竖屏的情况下，宽高比1：1的logo这里就应该是 0.2f * 9 / 16
             * @param xPercent logo左边缘相对屏幕左边缘的距离比（0~1），通常情况下，该值与widthPercent之和不应大于1，否则logo则无法完全显示在屏幕内
             * @param yPercent logo上边缘相对屏幕上边缘的距离比（0~1），通常情况下，该值与heightPercent之和不应大于1，否则logo则无法完全显示在屏幕内
             *                 清楚上述四个参数后，可以根据个人需求配置图片大小及位置。
             * @return
             */
            yfEncoderKit.configLogo(BitmapFactory.decodeResource(getResources(), R.mipmap.logo), landscapeLogoHeight * 9 / 16 * logoWidth / logoHeight, landscapeLogoHeight, 1 - landscapeLogoHeight * 9 / 16 * logoWidth / logoHeight - landscapeMarginRight, landsapeMarginTop);

        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            yfEncoderKit.configLogo(BitmapFactory.decodeResource(getResources(), R.mipmap.logo), landscapeLogoHeight * logoWidth / logoHeight, landscapeLogoHeight * 9 / 16, 1 - landscapeLogoHeight * logoWidth / logoHeight - portMarginRight, portMarginTop);

        }
        Log.d(TAG, "当前角度：" + getWindowManager().getDefaultDisplay().getRotation());
    }


    protected String getDefaultUrl() {
        return URL_LIVE;
    }

    protected void startRecorder() {
        if (yfEncoderKit.isRecording()) {//不允许推流过程中进行参数设置
            return;
        }
        Log.d(TAG, "开始录制");
        //设置编码参数：直播/录制、是否硬编、码率、宽、高
        yfEncoderKit.changeMode(YfEncoderKit.MODE_LIVE, HARD_ENCODER, VIDEO_BITRATE, VIDEO_WIDTH, VIDEO_HEIGHT);
//        yfEncoderKit.setMaxReconnectCount(5);//自动重连次数，0代表不自动重连
//        yfEncoderKit.setAdjustQualityAuto(true, 300);//打开码率自适应，最低码率300k
        yfEncoderKit.setBufferSizeBySec(1);//最多缓存1秒的数据，超过1秒则丢帧
        yfEncoderKit.setLiveUrl(mUrl.getText().toString());
        yfEncoderKit.startRecord();
        if (dataShowing) {
            infoShower.removeCallbacks(updateDisplay);
            infoShower.postDelayed(updateDisplay, 500);
        }

    }


    Runnable updateDisplay = new Runnable() {
        @Override
        public void run() {
            textBuffer.setText("buffer-ms:" + mCurrentBufferMs);
            textBitrate.setText("bitrate:" + mCurrentBitrate);
            infoShower.removeCallbacks(this);
            infoShower.postDelayed(this, 500);
        }
    };


    protected void stopRecorder() {
        if (dataShowing) {
            infoShower.removeCallbacks(updateDisplay);
        }
        yfEncoderKit.stopRecord();
    }

    private void destroyRecorder() {
        Log.d(TAG, "销毁编码器");
        if (yfEncoderKit != null) {
            if (currentState == YfEncoderKit.STATE_RECORDING)
                stopRecorder();
            yfEncoderKit.release();
            yfEncoderKit = null;
        }

    }

    @Override
    public void onServerConnected() {
        Toast.makeText(this, "推流成功，编码方式:" + (yfEncoderKit.getEncodeMode() ? "硬编" : "软编"), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(int mode, int camId, int err, String msg) {
        actionbarLayout.setBackgroundColor(getResources().getColor(R.color.blue));
        Log.i(TAG, "####### error: " + camId + "  " + err + " " + msg);
        Toast.makeText(this, "err: no=" + err + " msg=" + msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "push data disconnected,reconfigure and retry");
    }

    private int currentState;
    NetworkConnectChangedReceiver receiver;

    @Override
    public void onStateChanged(int mode, int camId, int oldState, int newState) {
        currentState = newState;
        if (newState == YfEncoderKit.STATE_RECORDING) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
            maybeRegisterReceiver();//监听wifi连接状况
        } else {
            if (startRecoderAuto && newState == YfEncoderKit.STATE_PREPARED) {
                startRecoderAuto = false;
//                startRecorder();

            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.green));
                }
            });
        }
        Log.i(TAG,
                "####### state changed: " + camId + "  "
                        + YfEncoderKit.getRecordStateString(oldState) + " -> "
                        + YfEncoderKit.getRecordStateString(newState));
        Toast.makeText(this, YfEncoderKit.getRecordStateString(oldState) + " -> "
                + YfEncoderKit.getRecordStateString(newState), Toast.LENGTH_SHORT).show();
    }

    protected void maybeRegisterReceiver() {
        Log.d(TAG, "maybeRegisterReceiver" + receiver);
        if (receiver != null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        receiver = new NetworkConnectChangedReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    public void onFragment(int mode, int camId, String fragPath) {
        Log.i(TAG, "####### fragment: " + camId + "  " + fragPath);
    }

    @Override
    public void onTimeUpdate(int time) {
//        Log.i(TAG, "####### time update: " + time);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (yfEncoderKit != null)
            yfEncoderKit.pause();
//        stopRecorder();
    }

    @Override
    public void onCapturedResult(String path) {
        Toast.makeText(this, "截图成功" + path, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBufferHandleCallback(int currentBitrate, int bufferMs, int event) {
        mCurrentBitrate = currentBitrate;
        mCurrentBufferMs = bufferMs;
        switch (event) {
            case YfEncoderKit.EVENT_DECREASE_BITRATE:
                logRecoder.writeLog("降低码率至：" + currentBitrate);
                break;
            case YfEncoderKit.EVENT_INCREASE_BITRATE:
                logRecoder.writeLog("提高码率至：" + currentBitrate);
                break;
            case YfEncoderKit.EVENT_DROP_FRAME:
                logRecoder.writeLog("丢帧~");
                break;
//            case YfEncoderKit.EVENT_RETURN_LAST_SMOOTHING_BITRATE:
//                logRecoder.writeLog("回归提升码率前的码率");
//                break;
//            case YfEncoderKit.EVENT_BUFFER_INCREASING:
//                logRecoder.writeLog("缓存5秒持续超过70%");
//                break;
//            case YfEncoderKit.EVENT_NONE:
//                break;
        }

    }

    @Override
    public void onEncodeOverLoad(YfEncoderKit.YfFilterType... removeFilterType) {
        Log.d(TAG, "负载过高，关闭高消耗滤镜:" + (removeFilterType == null ? "无移除滤镜" : removeFilterType.length));
        for (YfEncoderKit.YfFilterType type : removeFilterType) {
            if (type == YfEncoderKit.YfFilterType.BEAUTY && setBeauty) {
                Log.d(TAG, "美颜滤镜被移除~");
                setBeauty = false;
            } else if (type == YfEncoderKit.YfFilterType.LOGO && setLogo) {
                Log.d(TAG, "水印被移除~");
                setLogo = false;
            }
        }
        Toast.makeText(this, "负载过高", Toast.LENGTH_SHORT).show();
    }

    public class NetworkConnectChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                    Log.e(TAG, "isConnected:" + isConnected);
                    if (isConnected) {

                        logRecoder.writeLog("WIFI已连接");
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logRecoder.writeLog("WIFI已断开");
                            }
                        });
                    }
                }
            }
        }
    }

    private static final int CODE_FOR_OPEN_CAMERA = 100;

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPerssion() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.CAMERA) | checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, CODE_FOR_OPEN_CAMERA);
        } else {
            initView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODE_FOR_OPEN_CAMERA) {
            if (permissions[0].equals(Manifest.permission.CAMERA)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && permissions[1].equals(Manifest.permission.RECORD_AUDIO)
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //用户同意使用camera
                initView();
            } else {
                //用户不同意，自行处理即可
                finish();
            }
        }
    }

    public void showLogs() {
        android.util.Log.d(TAG, "查看消息");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Infos").setItems(
                logRecoder.getLogs(), null).setPositiveButton("清空", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logRecoder.clearLogs();
                dialog.dismiss();
            }
        }).setNegativeButton(
                "关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }


}
