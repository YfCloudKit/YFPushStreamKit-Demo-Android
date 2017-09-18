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
import android.graphics.Rect;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.util.Log;
import com.yunfan.encoder.demo.util.LogRecorder;
import com.yunfan.encoder.demo.util.Util;
import com.yunfan.encoder.demo.widget.ScaleGLSurfaceView;
import com.yunfan.encoder.filter.AlphaBlendFilter;
import com.yunfan.encoder.filter.FaceUnityFilter;
import com.yunfan.encoder.filter.YfBlurBeautyFilter;
import com.yunfan.encoder.filter.YfFilterFactory;
import com.yunfan.encoder.filter.YfWaterMarkFilter;
import com.yunfan.encoder.widget.RecordMonitor;
import com.yunfan.encoder.widget.YfEncoderKit;

import static com.yunfan.encoder.demo.util.Util.intToIp;

public class LiveRecorderActivity extends AppCompatActivity implements RecordMonitor {
    protected static final String TAG = "YfRecorder_Live";
    public static final String ORIENTATION = "orientation";
    public static final String ENABLE_UDP = "enableUDP";
    public static final String ENABLE_HTTP_DNS = "enableDNS";
    public static final String ENABLE_HARD_ENCODER = "enableHardEncoder";
    public static final String CUSTOM_BITRATE = "bitrate";

    protected final static String CACHE_DIRS = Environment.getExternalStorageDirectory().getPath() + "/yunfanencoder";
    //    public static String URL_LIVE = "rtmp://publish.langlive.com/live/123456789";
    public static String URL_LIVE = "rtmp://push-zk.yftest.yflive.net/live/test111";
    // 默认的直播发起url
    public static String URL_LIVE_UDP = "rtmp://push-zk.yftest.yflive.net/live/test111";
//    public static String URL_LIVE = "rtmp://push-zk.yftest.yflive.net/live/test111";
//        public static String URL_LIVE = "rtmp://yfstream.livestar.com/live/test111";//

    // 默认的录制文件存放目录
    public static String URL_VOD = "测试视频";
    //设置保存截图等文件的文件夹
    protected YfEncoderKit yfEncoderKit;
    protected boolean showBeautyPanel = false;
    protected boolean dropVideoFrameOnly = false;
    protected boolean setBeauty = false;
    protected boolean setLogo = false;
    protected boolean setFace = false;
    protected boolean dataShowing = false;
    protected boolean enableAudio = true;
    protected boolean autoFocus = true;//默认自动对焦
    protected EditText mUrl;
    private boolean startRecoderAuto = true;
    private ScaleGLSurfaceView mGLSurfaceView;
    protected LinearLayout actionbarLayout, infoLayout, beautyPanel;
    private TextView textBitrate, textBuffer, textFps, textSpeed, textCostTime;
    private ActionBar actionBar;
    private int surfaceWidth, surfaceHeight;
    protected boolean mLandscape = false;
    protected boolean mEnableUdp = false;
    protected boolean mEnableHttpDns = false;
    protected int mInputBitrate;
    private boolean mForceStop;
    protected boolean mHardEncoder = true;
    private SeekBar seekBar0;
    private Button mFuChangeEffectBtn, mFuChangeGestureBtn, mFuEnableBeautyBtn, mFuEnableEffectBtn, mFuEnableGestureBtn;
    private boolean mFuEnableBeauty = true, mFuEnableEffect = true, mFuEnableGesture = true;
    protected int VIDEO_WIDTH = 640;
    protected int VIDEO_HEIGHT = 368;
    protected int VIDEO_FRAME_RATE = 24;

    protected int PREVIEW_WIDTH = 1280;
    protected int PREVIEW_HEIGHT = 720;

    protected LogRecorder logRecorder = new LogRecorder(this);
    private Handler infoShower = new Handler();
    private int mCurrentBitrate, mCurrentBufferMs, mCurrentSpeed, mCurrentFPS, mAvgCostTimeMS;
    private final int BEAUTY_INDEX = 1, LOGO_INDEX = 2, FACE_INDEX = 3,WATER_INDEX=4;
    private YfBlurBeautyFilter mBeautyFilter;
    private FaceUnityFilter mFaceUnityFilter;
    private YfWaterMarkFilter mYfWaterMarkFilter;
    private int mCurrentEffectId;
    private int mCurrentGestureId;
    private AlphaBlendFilter mLogoFilter;
    private boolean mEnableAudioPlay;
    private boolean mAutoAdaptiveBitrate = true;
    private static final String[] m_item_names = {"tiara.mp3", "item0208.mp3", "einstein.mp3", "YellowEar.mp3", "PrincessCrown.mp3",
            "Mood.mp3", "Deer.mp3", "BeagleDog.mp3", "item0501.mp3", "ColorCrown.mp3", "item0210.mp3", "HappyRabbi.mp3",
            "item0204.mp3", "hartshorn.mp3"
    };

    private String[] mGestureItemNames = {"heart.mp3"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLandscape = getIntent().getBooleanExtra(ORIENTATION, false);
        mEnableUdp = getIntent().getBooleanExtra(ENABLE_UDP, false);
        mEnableHttpDns = getIntent().getBooleanExtra(ENABLE_HTTP_DNS, false);
        mHardEncoder = getIntent().getBooleanExtra(ENABLE_HARD_ENCODER, true);
        mInputBitrate = getIntent().getIntExtra(CUSTOM_BITRATE, MainActivity.DEFAULT_BITRATE);
        Log.d(TAG, "自定义码率:" + mInputBitrate);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkEncoderPermission();
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
                startRecorder(true);
                break;
            case R.id.action_stop:
                mForceStop = true;
                stopRecorder();
                break;
            case R.id.action_close_capture:
                yfEncoderKit.captureCurrentFrame(System.currentTimeMillis() + "", new YfEncoderKit.OnPictureSaveListener() {
                    @Override
                    public void onSaved(final String result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LiveRecorderActivity.this, "截图成功:" + result, Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
                break;
            case R.id.action_switch:
                yfEncoderKit.switchCamera();
                break;
            case R.id.action_manual_focus:
                if (!autoFocus) {
                    autoFocus = true;
                    yfEncoderKit.autoFocus();
                } else {
                    actionBar.show();
                    mUrl.setVisibility(View.VISIBLE);
                    autoFocus = false;
                }
                break;
            case R.id.action_torch:
                yfEncoderKit.setFlash(!yfEncoderKit.isFlashOn());
                break;
            case R.id.action_enable_audio:
                enableAudio = !enableAudio;
                yfEncoderKit.enablePushAudio(enableAudio);
                Toast.makeText(this, (enableAudio ? "恢复" : "暂停") + "推送音频流", Toast.LENGTH_LONG).show();
                break;
            case R.id.action_set_beauty:
                if (!setBeauty) {
                    if (mBeautyFilter == null) {
                        mBeautyFilter = new YfBlurBeautyFilter(LiveRecorderActivity.this);
                        mBeautyFilter.setIndex(BEAUTY_INDEX);
                    }
                    yfEncoderKit.addFilter(mBeautyFilter);
                } else {
                    yfEncoderKit.removeFilter(BEAUTY_INDEX);
                }
                setBeauty = !setBeauty;
                break;
//            case R.id.action_beauty_panel:
//                if (!showBeautyPanel) {
//                    beautyPanel.setVisibility(View.VISIBLE);
//                } else {
//                    beautyPanel.setVisibility(View.GONE);
//                }
//                showBeautyPanel = !showBeautyPanel;
//                break;
            case R.id.action_face_u:
                if (!setFace) {
                    if (mYfWaterMarkFilter == null) {
                        try {
                            mYfWaterMarkFilter = new YfFilterFactory.Factory(LiveRecorderActivity.this).createWaterMarkFilter();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        mYfWaterMarkFilter.setIndex(WATER_INDEX);
                        mYfWaterMarkFilter.setWaterMark(BitmapFactory.decodeResource(getResources(), R.mipmap.logo));
                        int percent = 50;
                        int padding = 50;
                        float logoWidth = 454, logoHeight = 160;
                        int width = PREVIEW_HEIGHT * percent / 100;
                        int height = (int) (width * logoWidth / logoHeight);
                        mYfWaterMarkFilter.setPosition(0, 0, width, height);
                    }
                    yfEncoderKit.addFilter(mYfWaterMarkFilter);
                } else {
                    yfEncoderKit.removeFilter(WATER_INDEX);
                }
                setFace = !setFace;
//                if (!setFace) {
//                    if (setBeauty) {//开启face u 则默认关闭原本的美颜
//                        yfEncoderKit.removeFilter(BEAUTY_INDEX);
//                        setBeauty = !setBeauty;
//                    }
//                    yfEncoderKit.addFilter(mFaceUnityFilter);
//                    mFuChangeEffectBtn.setVisibility(View.VISIBLE);
//                    mFuChangeGestureBtn.setVisibility(View.GONE);
//                    mFuEnableBeautyBtn.setVisibility(View.VISIBLE);
//                    mFuEnableEffectBtn.setVisibility(View.VISIBLE);
//                    mFuEnableGestureBtn.setVisibility(View.VISIBLE);
//                } else {
//                    yfEncoderKit.removeFilter(FACE_INDEX);
//                    mFuChangeEffectBtn.setVisibility(View.GONE);
//                    mFuChangeGestureBtn.setVisibility(View.GONE);
//                    mFuEnableBeautyBtn.setVisibility(View.GONE);
//                    mFuEnableEffectBtn.setVisibility(View.GONE);
//                    mFuEnableGestureBtn.setVisibility(View.GONE);
//                }
//                setFace = !setFace;
                break;
            case R.id.action_set_logo:
                if (!setLogo) {
                    if (mLogoFilter == null) {
                        mLogoFilter = new AlphaBlendFilter(1);
                        mLogoFilter.setIndex(LOGO_INDEX);
                        float landscapeMarginRight = 0.1f;//横屏模式下logo的marginright所占宽度的比例
                        float portMarginRight = 0.05f;//竖屏模式下logo的marginright所占宽度的比例
                        float landsapeMarginTop = 0.05f;//横屏模式下logo的marginTop所占宽度的比例
                        float portMarginTop = 0.1f;//竖屏模式下logo的marginTop所占宽度的比例
                        float landscapeLogoHeight = 0.2f;//横屏模式下logo的高度所占屏幕高度的比例
                        float logoWidth = 454, logoHeight = 160;//计算logo的比例
                        if (mLandscape) {
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
                            mLogoFilter.config(BitmapFactory.decodeResource(getResources(), R.mipmap.logo), landscapeLogoHeight * 9 / 16 * logoWidth / logoHeight, landscapeLogoHeight, 1 - landscapeLogoHeight * 9 / 16 * logoWidth / logoHeight - landscapeMarginRight, landsapeMarginTop);
                        } else {
                            mLogoFilter.config(BitmapFactory.decodeResource(getResources(), R.mipmap.logo), landscapeLogoHeight * logoWidth / logoHeight, landscapeLogoHeight * 9 / 16, 1 - landscapeLogoHeight * logoWidth / logoHeight - portMarginRight, portMarginTop);

                        }
                    }
                    yfEncoderKit.addFilter(mLogoFilter);
                } else {
                    yfEncoderKit.removeFilter(LOGO_INDEX);
                }
                setLogo = !setLogo;
                break;
            case R.id.action_drop_strategy:
                dropVideoFrameOnly = !dropVideoFrameOnly;
                yfEncoderKit.setDropVideoFrameOnly(dropVideoFrameOnly);
                Toast.makeText(LiveRecorderActivity.this, dropVideoFrameOnly ? "打开只丢视频帧策略" : "关闭只丢视频帧策略", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_flip_camera:
                yfEncoderKit.enableFlipFrontCamera(!yfEncoderKit.isFlipFrontCameraEnable());
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
            case R.id.action_audio_play:
                mEnableAudioPlay = !mEnableAudioPlay;
                yfEncoderKit.enableAudioPlay(mEnableAudioPlay);
                break;
            case R.id.action_auto_adaptive_bitrate:
                mAutoAdaptiveBitrate = !mAutoAdaptiveBitrate;
                yfEncoderKit.setAdjustQualityAuto(mAutoAdaptiveBitrate, 300);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "on resume!");
        if (yfEncoderKit != null)
            yfEncoderKit.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (yfEncoderKit != null) {
            yfEncoderKit.onPause();
        }
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
        beautyPanel = (LinearLayout) findViewById(R.id.beautyPanel);
        textBitrate = (TextView) findViewById(R.id.current_bitrate);
        textSpeed = (TextView) findViewById(R.id.current_speed);
        textFps = (TextView) findViewById(R.id.current_fps);
        textCostTime = (TextView) findViewById(R.id.cost_time);
        textBuffer = (TextView) findViewById(R.id.current_buffer_size_ms);
        mFuChangeEffectBtn = (Button) findViewById(R.id.change_effect);
        mFuChangeGestureBtn = (Button) findViewById(R.id.change_gesture);
        mFuEnableBeautyBtn = (Button) findViewById(R.id.enable_beauty);
        mFuEnableEffectBtn = (Button) findViewById(R.id.enable_effect);
        mFuEnableGestureBtn = (Button) findViewById(R.id.enable_gesture);
        mFuChangeEffectBtn.setText("动画效果:" + mCurrentEffectId);
        mFuChangeGestureBtn.setText(mGestureItemNames[mCurrentGestureId].
                substring(0, mGestureItemNames[mCurrentGestureId].length() - 4));

        mFuChangeEffectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentEffectId++;
                if (mCurrentEffectId >= m_item_names.length) {
                    mCurrentEffectId = 0;
                }
                if (mFaceUnityFilter != null) {
                    mFaceUnityFilter.setEffect(m_item_names[mCurrentEffectId], false);
                }
                mFuChangeEffectBtn.setText("动画效果:" + mCurrentEffectId);
            }
        });
        mFuChangeGestureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentGestureId++;
                if (mCurrentGestureId >= mGestureItemNames.length) {
                    mCurrentGestureId = 0;
                }
                if (mFaceUnityFilter != null) {
                    mFaceUnityFilter.setGestureEffect(mGestureItemNames[mCurrentGestureId], false);
                }
                mFuChangeGestureBtn.setText(mGestureItemNames[mCurrentGestureId].
                        substring(0, mGestureItemNames[mCurrentGestureId].length() - 4));
            }
        });
        mFuEnableBeautyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFaceUnityFilter != null) {
                    mFuEnableBeauty = !mFuEnableBeauty;
                    mFaceUnityFilter.enableBeautyEffect(mFuEnableBeauty);
                    mFuEnableBeautyBtn.setText(mFuEnableBeauty ? "关闭美颜" : "开启美颜");
                }
            }
        });
        mFuEnableEffectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFaceUnityFilter != null) {
                    mFuEnableEffect = !mFuEnableEffect;
                    if (mFuEnableEffect) {
                        mFaceUnityFilter.setEffect(m_item_names[mCurrentEffectId], false);
                        mFuEnableEffectBtn.setText("关闭特效");
                    } else {
                        mFaceUnityFilter.setEffect(null, false);
                        mFuEnableEffectBtn.setText("开启特效");
                    }
                }
            }
        });
        mFuEnableGestureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFaceUnityFilter != null) {
                    mFuEnableGesture = !mFuEnableGesture;
                    if (mFuEnableGesture) {
                        mFaceUnityFilter.setGestureEffect(mGestureItemNames[mCurrentGestureId], false);
                        mFuEnableGestureBtn.setText("关闭手势");
                    } else {
                        mFaceUnityFilter.setGestureEffect(null, false);
                        mFuEnableGestureBtn.setText("开启手势");
                    }
                }
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        mUrl = ((EditText) findViewById(R.id.url));
        mUrl.setText(getDefaultUrl());
        mUrl.setSelection(getDefaultUrl().length());

        seekBar0 = (SeekBar) findViewById(R.id.seekBar0);
        seekBar0.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //直接设置美颜效果为预设的等级
                mBeautyFilter.setBeautyLevel(progress);
//                updateBeautyPanel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        actionbarLayout = (LinearLayout) findViewById(R.id.actionbar_layout);

        mGLSurfaceView = (ScaleGLSurfaceView) findViewById(R.id.surface);
        mGLSurfaceView.initScaleGLSurfaceView(new ScaleGLSurfaceView.OnScareCallback() {
            @Override
            public int getCurrentZoom() {
                return yfEncoderKit.getCurrentZoom();
            }

            @Override
            public int getMaxZoom() {
                return yfEncoderKit.getMaxZoom();
            }

            @Override
            public boolean onScale(int zoom) {
                return yfEncoderKit.manualZoom(zoom);
            }
        });
        mGLSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    if (autoFocus) {
                        toggleActionBar();
                        return false;
                    }
                    float xPercent = event.getX() / ((float) mGLSurfaceView.getWidth());
                    float yPercent = event.getY() / ((float) mGLSurfaceView.getHeight());
                    Rect focusRect = ScaleGLSurfaceView.calculateTapArea(xPercent, yPercent, 1f);
                    yfEncoderKit.manualFocus(focusRect);

                }
                return false;
            }
        });
        if (mFaceUnityFilter == null) {
            mFaceUnityFilter = new FaceUnityFilter(LiveRecorderActivity.this);
            mFaceUnityFilter.setEffect(m_item_names[mCurrentEffectId], false);//使用assets里的文件只需要写文件名即可，第二个参数填false;
//              mFaceUnityFilter.setEffect(CACHE_DIRS+"/"+m_item_names[mCurrentEffectId],true);//使用完整路径的话，第二个参数填true
            mFaceUnityFilter.setGestureEffect(mGestureItemNames[mCurrentGestureId], false);//同上
            mFaceUnityFilter.enableBeautyEffect(true);//开启faceUnity自带的美颜
            mFaceUnityFilter.setBeautyType(FaceUnityFilter.BEAUTY_NATURE);//设置美颜类型
//            mFaceUnityFilter.setBeautyBlurLevel(5);//设置磨皮等级，取值0~5
            mFaceUnityFilter.setBeautyBlurLevel2(5);//新的磨皮效果，取值0~6
            mFaceUnityFilter.setBeautyRedLevel(1);//设置红润效果，0为关闭，0.5为默认效果，大于1为继续增强效果
            mFaceUnityFilter.setBeautyCheekThinningLevel(0);//设置瘦脸等级，0为关闭，1为默认，大于1为继续增强效果
            mFaceUnityFilter.setBeautyColorLevel(0.6);//设置色彩效果等级，0为关闭，1为默认，大于1为继续增强效果；美颜类型为nature时代表美白等级
            mFaceUnityFilter.setBeautyEyeEnlargingLevel(0);//设置大眼效果等级，0为关闭，1为默认，大于1为继续增强效果
            mFaceUnityFilter.setIndex(FACE_INDEX);
        }
        if (mLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        Log.d(TAG, "当前角度：" + getWindowManager().getDefaultDisplay().getRotation());
        setSurfaceSize(mLandscape, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        initRecorder(mGLSurfaceView);
        initDefaultSetting();
    }


    /**
     * @param landscape 是否为横屏模式，预览宽度，预览高度
     */
    private void setSurfaceSize(boolean landscape, int width, int height) {
        mLandscape = landscape;
        LayoutParams lp = mGLSurfaceView.getLayoutParams();
        int realScreenWidth = Util.getScreenWidth(this);
        Log.d(TAG, "realScreenWidth:" + realScreenWidth + "," + landscape);
        if (landscape) {
            surfaceWidth = realScreenWidth * 16 / 9;
            surfaceHeight = surfaceWidth * height / width;
        } else {
            surfaceHeight = realScreenWidth * 16 / 9;
            //考虑到高度可能被内置虚拟按键占用，因此为了保证预览界面为16:9，不能直接获取高度。
            surfaceWidth = surfaceHeight * height / width;
        }
        lp.width = surfaceWidth;
        lp.height = surfaceHeight;
        Log.d(TAG, "计算出来的宽高:" + surfaceWidth + "___" + surfaceHeight);
        mGLSurfaceView.setLayoutParams(lp);
    }

    protected void initRecorder(GLSurfaceView s) {
        Log.d(TAG, "初始化编码器");
        //初始化编码工具：context、截图/录制视频等文件保存的根目录、摄像头输出宽度、摄像头输出高度、编码宽度、编码高度、是否硬编、视频帧率
        yfEncoderKit = new YfEncoderKit(this, CACHE_DIRS, PREVIEW_WIDTH, PREVIEW_HEIGHT, VIDEO_WIDTH, VIDEO_HEIGHT, mHardEncoder, VIDEO_FRAME_RATE);
        yfEncoderKit.setContinuousFocus()//设置连续自动对焦
                .setLandscape(mLandscape)//设置是否横屏模式（默认竖屏）
                .enableFlipFrontCamera(true)//设置前置摄像头是否镜像处理，默认为false
                .setRecordMonitor(this)//设置回调
                .setDefaultCamera(true)//设置默认打开摄像头---true为前置，false为后置
                .openCamera(s);//设置预览窗口

    }

    protected void initDefaultSetting() {
        mBeautyFilter = new YfBlurBeautyFilter(this);
        mBeautyFilter.setIndex(BEAUTY_INDEX);
        yfEncoderKit.addFilter(mBeautyFilter);//默认打开滤镜
        setBeauty = true;
        seekBar0.setProgress(5);//预设美颜等级
//        yfEncoderKit.setAdjustQualityAuto(mAutoAdaptiveBitrate, 300);//打开码率自适应，最低码率30
    }

    protected String getDefaultUrl() {
        if (mEnableUdp && !mEnableHttpDns)
            return URL_LIVE_UDP;
        else
            return URL_LIVE;
    }

    protected void startRecorder(boolean changeUI) {
        if (yfEncoderKit == null || yfEncoderKit.isRecording()) {//不允许推流过程中进行参数设置
            return;
        }

        if (changeUI)
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        Log.d(TAG, "开始录制");
        onServerConnected = false;
        //设置编码参数：直播/录制、码率
        yfEncoderKit.changeMode(YfEncoderKit.MODE_LIVE, mInputBitrate);
        yfEncoderKit.setBufferSizeBySec(1);//最多缓存1秒的数据，超过1秒则丢帧
        yfEncoderKit.enableUDP(mEnableUdp);
        yfEncoderKit.enableHttpDNS(mEnableHttpDns);
        yfEncoderKit.setLiveUrl(mUrl.getText().toString());
        yfEncoderKit.startRecord();
        if (dataShowing) {
            infoShower.removeCallbacks(updateDisplay);
            infoShower.postDelayed(updateDisplay, 500);
        }
        mForceStop = false;
    }


    private final Runnable updateDisplay = new Runnable() {
        @Override
        public void run() {
            textBuffer.setText("buffer-ms:" + mCurrentBufferMs);
            textBitrate.setText("bitrate:" + mCurrentBitrate);
            textSpeed.setText("speed:" + mCurrentSpeed);
            textFps.setText("fps:" + mCurrentFPS);
            textCostTime.setText("cost:" + mAvgCostTimeMS);
            infoShower.removeCallbacks(this);
            infoShower.postDelayed(this, 500);
        }
    };


    protected void stopRecorder() {
        if (yfEncoderKit == null) return;
        if (dataShowing) {
            infoShower.removeCallbacks(updateDisplay);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                actionbarLayout.setBackgroundColor(getResources().getColor(R.color.green));
            }
        });
        yfEncoderKit.stopRecord();
        onServerConnected = false;
    }

    private void destroyRecorder() {
        Log.d(TAG, "destroyRecorder");
        if (yfEncoderKit != null) {
            if (yfEncoderKit.isRecording())
                stopRecorder();
            yfEncoderKit.release();
            yfEncoderKit = null;
        }

    }

    private boolean onServerConnected = false;

    @Override
    public void onServerConnected() {
        onServerConnected = true;
        if (yfEncoderKit != null) {
            logRecorder.writeLog("成功连接服务器，编码方式:" + (yfEncoderKit.getEncodeMode() ? "硬编" : "软编"), true);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                }
            });
        }
    }

    @Override
    public void onError(int mode, int err, String msg) {
        actionbarLayout.setBackgroundColor(getResources().getColor(R.color.blue));
        onServerConnected = false;
        if (!mForceStop)
            startRecorder(false);
        Log.i(TAG, "####### error: " + err + " " + msg);
    }


    @Override
    public void onStateChanged(int mode, int oldState, int newState) {
        if (onServerConnected && newState == YfEncoderKit.STATE_RECORDING) {
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
            }

        }
        Log.i(TAG,
                "####### state changed: "
                        + YfEncoderKit.getRecordStateString(oldState) + " -> "
                        + YfEncoderKit.getRecordStateString(newState));
    }

    private NetworkConnectChangedReceiver receiver;

    private void maybeRegisterReceiver() {
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
    public void onFragment(int mode, String fragPath, boolean success) {
        Log.i(TAG, "####### fragment: " + fragPath);
    }


    private String currentConnectedIP;

    @Override
    public void onInfo(int what, int arg1, int arg2, Object obj) {
        switch (what) {
            case YfEncoderKit.INFO_IP:
                currentConnectedIP = intToIp(arg1);
                Log.d(TAG, "实际推流的IP地址:" + currentConnectedIP);
                logRecorder.writeLog("IP:" + currentConnectedIP, false);
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        logRecorder.writeLog(Util.ping(currentConnectedIP));
//                    }
//                }).start();

                break;
            case YfEncoderKit.INFO_DROP_FRAMES:
                Log.d(TAG, "frames had been dropped");
                logRecorder.writeLog("drop frames", false);
                break;
            case YfEncoderKit.INFO_PUSH_SPEED:
                mCurrentSpeed = arg1;
                break;
            case YfEncoderKit.INFO_FRAME:
                mCurrentFPS = arg1;
                mAvgCostTimeMS = arg2;
                break;
            case YfEncoderKit.INFO_PREVIEW_SIZE_CHANGED:
                Log.d(TAG, "on preview size changed:" + arg1 + "," + arg2);
                Log.d(TAG, "on preview size changed:" + (float) arg1 / arg1 + "," + (float) PREVIEW_WIDTH / PREVIEW_HEIGHT);
                break;
            case YfEncoderKit.INFO_ADAPTIVE_BITRATE_CALLBACK:
                switch ((int) obj) {
                    case YfEncoderKit.EVENT_DECREASE_BITRATE:
                        logRecorder.writeLog("decrease bitrate：" + arg1, false);
                        break;
                    case YfEncoderKit.EVENT_INCREASE_BITRATE:
                        logRecorder.writeLog("increase bitrate：" + arg2, false);
                        break;
                    case YfEncoderKit.EVENT_DROP_FRAME:
                        logRecorder.writeLog("drop frames~", false);
                        break;
                    case YfEncoderKit.EVENT_RETURN_LAST_SMOOTHING_BITRATE:
                        logRecorder.writeLog("return bitrate", false);
                        break;
                    case YfEncoderKit.EVENT_BUFFER_INCREASING:
                        logRecorder.writeLog("buffer increasing", false);
                        break;
                    case YfEncoderKit.EVENT_NONE:
                        break;
                }
                break;
            case YfEncoderKit.INFO_BITRATE_CHANGED:
                mCurrentBitrate = arg1;
                break;
            case YfEncoderKit.INFO_CURRENT_BUFFER:
                mCurrentBufferMs = arg1;
                break;
        }
    }

    protected boolean needAutoRecord = true;

    @Override
    protected void onStop() {
        super.onStop();
        if (yfEncoderKit != null)
            yfEncoderKit.onStop(needAutoRecord);
    }


    private boolean netIsConnected;

    public class NetworkConnectChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State state = networkInfo.getState();
                    netIsConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                    Log.e(TAG, "isConnected:" + netIsConnected);
                    if (netIsConnected) {
                        logRecorder.writeLog("WIFI已连接", true);
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                logRecorder.writeLog("WIFI已断开", true);
                            }
                        });
                    }
                }
            }
        }
    }

    private static final int CODE_FOR_OPEN_CAMERA = 100;

    @TargetApi(Build.VERSION_CODES.M)
    private void checkEncoderPermission() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.CAMERA) | checkSelfPermission(Manifest.permission.RECORD_AUDIO);
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

    public void showLogs() {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Log").setItems(
                logRecorder.getLogs(), null).setPositiveButton("清空", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logRecorder.clearLogs();
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
