package com.yunfan.encoder.demo.activity;


import android.annotation.SuppressLint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.appyvet.rangebar.RangeBar;
import com.bumptech.glide.Glide;
import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.adapter.VideoEditAdapter;
import com.yunfan.encoder.demo.bean.VideoEditInfo;
import com.yunfan.encoder.demo.util.ExtractFrameWorkThread;
import com.yunfan.encoder.demo.util.ExtractVideoInfoUtil;
import com.yunfan.encoder.demo.util.Util;
import com.yunfan.encoder.demo.widget.AudioCutView;
import com.yunfan.encoder.demo.widget.VideoProgressView;
import com.yunfan.encoder.widget.YfVodKit;
import com.yunfan.player.widget.YfCloudPlayer;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class SegmentRecordActivity2 extends LiveRecorderActivity implements YfVodKit.VodKitMonitor {
    public static String AUDIO_PATH = "AUDIO_PATH";
    private static final String TAG = "Yf_SegmentActivity2";
    public static final float MAX_RECORD_TIME = 15 * 1000f;  //设置录制的最大时间.
    public static final float MIN_RECORD_TIME = 5 * 1000f;   //录制的最小时间
    private float mSpeed = 1f;
    private VideoProgressView mVideoProgressView;
    private TextView mTvMerge;
    private TextView mTvDelete;
    private Button mBtnCut;
    private RangeBar mRangeBar;
    private RecyclerView mRecyclerView;
    private ImageView mCurrentLeftPic, mCurrentRightPic;
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private VideoEditAdapter videoEditAdapter;
    private YfVodKit mYfVodKit;
    private RadioGroup.OnCheckedChangeListener mCheckedChangeListener
            = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            switch (checkedId) {
                case R.id.rb_speed_lowest:
                    mSpeed = 3.0f;
                    break;
                case R.id.rb_speed_lower:
                    mSpeed = 2.0f;
                    break;
                case R.id.rb_speed_normal:
                    mSpeed = 1.0f;
                    break;
                case R.id.rb_speed_quicker:
                    mSpeed = 0.5f;
                    break;
                case R.id.rb_speed_quickest:
                    mSpeed = 0.33f;
                    break;
            }
        }
    };

    private RelativeLayout mRlSegmentRecord;
    private LinearLayout mLlPlayer;
    private RangeBar.OnRangeBarChangeListener mOnRangeBarChangeListener
            = new RangeBar.OnRangeBarChangeListener() {
        @Override
        public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex,
                                          String leftPinValue, String rightPinValue) {
            Log.d(TAG, String.format("onRangeChangeListener: %s-%s...%s-%s ", leftPinIndex,
                    rightPinIndex, leftPinValue, rightPinValue));
            mEtStart.setText(String.valueOf(leftPinIndex));
            mEtEnd.setText(String.valueOf(rightPinIndex));

            List<VideoEditInfo> videoEditInfoList = videoEditAdapter.getVideoEditInfoList();
            if (videoEditInfoList.isEmpty()) return;
            final int leftPicIndex = Math.min(videoEditAdapter.getVideoEditInfoList().size() - 1, leftPinIndex / 2);
            final int rightPicIndex = Math.min(videoEditAdapter.getVideoEditInfoList().size() - 1, rightPinIndex / 2);
            Glide.with(SegmentRecordActivity2.this)
                    .load("file://" + videoEditAdapter.getVideoEditInfoList().get(leftPicIndex).path)
                    .into(mCurrentLeftPic);
            Glide.with(SegmentRecordActivity2.this)
                    .load("file://" + videoEditAdapter.getVideoEditInfoList().get(rightPicIndex).path)
                    .into(mCurrentRightPic);
        }
    };

    private EditText mEtStart;
    private EditText mEtEnd;
    private FrameLayout mFlSegmentRecord;
    private long mAudioDuration;
    private TextView mTvSelectStartTime;
    private TextView mTvSelectOK;
    private AudioCutView mAudioCutView;
    private boolean mIsSelectAudioStartTime;
    private CheckBox mCbReverse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void initConfig() {
        VIDEO_WIDTH = 960;
        VIDEO_HEIGHT = 544;
        VIDEO_FRAME_RATE = 24;

    }

    private String mAudioPath, mMuxPath;

    private void initMediaKitAndPath() {
        mAudioPath = getIntent().getStringExtra(AUDIO_PATH);//原始音频文件路径
    }

    @Override
    protected void initView() {
        initConfig();
        super.initView();
        //录制
        mFlSegmentRecord = (FrameLayout) findViewById(R.id.fl_segment_record);
        mFlSegmentRecord.setVisibility(View.VISIBLE);
        mRlSegmentRecord = (RelativeLayout) findViewById(R.id.rl_segment_record);
        mVideoProgressView = (VideoProgressView) findViewById(R.id.recorder_progress);
        mVideoProgressView.setOnProgressListener(mOnProgressListener);
        mVideoProgressView.setMaxDuration(MAX_RECORD_TIME);
        mTvDelete = (TextView) findViewById(R.id.tv_recorder_delete);
        mTvMerge = (TextView) findViewById(R.id.tv_recorder_merge);
        mTvSelectStartTime = (TextView) findViewById(R.id.tv_select_start_time);
        mTvSelectOK = (TextView) findViewById(R.id.tv_select_ok);
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.rg_speed);
        radioGroup.setOnCheckedChangeListener(mCheckedChangeListener);
        mTvDelete.setOnClickListener(mOnClickListener);
        mTvMerge.setOnClickListener(mOnClickListener);
        mTvSelectStartTime.setOnClickListener(mOnClickListener);
        mTvSelectOK.setOnClickListener(mOnClickListener);
        //剪辑
        mLlPlayer = (LinearLayout) findViewById(R.id.ll_player);
        mRangeBar = (RangeBar) findViewById(R.id.range_bar);
        mRangeBar.setTickInterval(1);
        mRangeBar.setTickStart(0);
        mRangeBar.setTickEnd(MAX_RECORD_TIME / 1000);
        mRangeBar.setTemporaryPins(false);
        mRangeBar.setOnRangeBarChangeListener(mOnRangeBarChangeListener);

        mEtStart = (EditText) findViewById(R.id.et_start);
        mEtEnd = (EditText) findViewById(R.id.et_end);
        mBtnCut = (Button) findViewById(R.id.btn_video_cut);
        mBtnCut.setOnClickListener(mOnClickListener);

        mRecyclerView = (RecyclerView) findViewById(R.id.id_rv);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mRecyclerView.setLayoutManager(linearLayoutManager);
        int picWidth = (int) (Util.getDisplayWidth(this) / (MAX_RECORD_TIME / 1000 / 2));
        videoEditAdapter = new VideoEditAdapter(this, picWidth);
        mRecyclerView.setAdapter(videoEditAdapter);

        mCurrentLeftPic = (ImageView) findViewById(R.id.current_pic_left);
        mCurrentRightPic = (ImageView) findViewById(R.id.current_pic_right);

        mUrl.setHint("请输入保存名称");
//        mCbReverse = (CheckBox) findViewById(R.id.reverse);
        initPlayer();
    }

    protected void initRecorder(GLSurfaceView s) {
        Log.d(TAG, "初始化编码器");
        mYfVodKit = new YfVodKit(this, CACHE_DIRS, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                VIDEO_WIDTH, VIDEO_HEIGHT, mHardEncoder, VIDEO_FRAME_RATE, mInputBitrate, this);

        initMediaKitAndPath();
        Log.d(TAG, "mAudioPath: " + mAudioPath);
        mYfVodKit.setAudioSource(mAudioPath);
        mYfVodKit.setInputPlayer(mPlayerInterface);
        yfEncoderKit = mYfVodKit.getYfEncoderKit()//可以获取内部的YfEncoderKit以设置美颜等经典配置。
                .setContinuousFocus()//设置连续自动对焦
                .setDefaultCamera(true);//设置默认打开摄像头---true为前置，false为后置
        mYfVodKit.openCamera(s);
    }

    YfVodKit.InputPlayerInterface mPlayerInterface = new YfVodKit.InputPlayerInterface() {
        @Override
        public long getCurrentPosition() {
            return mYfCloudPlayer != null ? mYfCloudPlayer.getCurrentPosition() : 0;
        }

        @Override
        public void start() {
            playMusic();
        }

        @Override
        public void pause() {
            pauseMusic();
        }

        @Override
        public void seekTo(long l) {
            if (mYfCloudPlayer != null) {
                mYfCloudPlayer.seekTo(l);
            }
        }

    };

    private void showFrameThumbnails() {
        if (!new File(mMuxPath).exists()) {
            Log.d(TAG, "视频文件不存在:" + mMuxPath);
            return;
        }
        if (mExtractVideoInfoUtil == null)
            mExtractVideoInfoUtil = new ExtractVideoInfoUtil(mMuxPath);
        long endPosition = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());
        long startPosition = 0;
        thumbnailsCount =/* mTotalRecordTime < MAX_RECORD_TIME ? (int) (MAX_RECORD_TIME / 1000)
                : */(int) (MAX_RECORD_TIME / 1000 / 2);//2秒截取一帧
        int extractW = (Util.getDisplayWidth(this)) / 4;
        int extractH = Util.dip2px(this, 55);
        mExtractFrameWorkThread = new ExtractFrameWorkThread(
                extractW, extractH, mUIHandler, mMuxPath,
                CACHE_DIRS + "/Thumbnails", startPosition, endPosition, thumbnailsCount);
        mExtractFrameWorkThread.start();
    }

    private final MainHandler mUIHandler = new MainHandler(this);
    private int thumbnailsCount, picCount;
    public static final int MSG_SAVE_SUCCESS = 0;

    @SuppressLint("HandlerLeak")
    private class MainHandler extends Handler {
        private final WeakReference<SegmentRecordActivity2> mActivity;

        MainHandler(SegmentRecordActivity2 activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SegmentRecordActivity2 activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MSG_SAVE_SUCCESS:
                        if (activity.videoEditAdapter != null) {
                            VideoEditInfo info = (VideoEditInfo) msg.obj;
                            activity.videoEditAdapter.addItemVideoInfo(info);
                            activity.picCount++;
                            if (activity.picCount == activity.thumbnailsCount) {
                                List<VideoEditInfo> videoEditInfoList = videoEditAdapter.getVideoEditInfoList();
                                if (videoEditInfoList.isEmpty()) return;
                                Glide.with(SegmentRecordActivity2.this)
                                        .load("file://" + videoEditAdapter.getVideoEditInfoList().get(0).path)
                                        .into(mCurrentLeftPic);
                                Glide.with(SegmentRecordActivity2.this)
                                        .load("file://" + videoEditAdapter.getVideoEditInfoList().get(videoEditInfoList.size() - 1).path)
                                        .into(mCurrentRightPic);
                            }
                        }
                        break;
                }

            }
        }
    }

    /**
     * 根据录制时间戳绘制进度条
     */
    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mYfVodKit == null) return;
            mVideoProgressView.updateProgress(mYfVodKit.getCurrentRecordTimestamp());
            mUIHandler.postDelayed(this, 40);
        }
    };


    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_recorder_delete:
                    deleteSegment();
                    break;
                case R.id.tv_recorder_merge:
                    //该步骤包括片段视频的拼接、音频转码和音视频合成
                    changeUIAndFinishRecord();
                    break;
                case R.id.btn_video_cut:
                    Toast.makeText(SegmentRecordActivity2.this, "开始裁剪视频...", Toast.LENGTH_SHORT).show();
                    int cutStartTime = Integer.valueOf(mEtStart.getText().toString());
                    int cutEndTime = Integer.valueOf(mEtEnd.getText().toString());
                    mYfVodKit.splitMedia(mMuxPath, cutStartTime, cutEndTime);
                    break;
                case R.id.tv_select_start_time:
                    mTvSelectStartTime.setVisibility(View.GONE);
                    mTvSelectOK.setVisibility(View.VISIBLE);
                    if (mAudioCutView != null) mAudioCutView.setVisibility(View.VISIBLE);
                    break;
                case R.id.tv_select_ok:
                    mIsSelectAudioStartTime = true;
                    mTvSelectStartTime.setVisibility(View.VISIBLE);
                    mTvSelectOK.setVisibility(View.GONE);
                    if (mAudioCutView != null) mAudioCutView.setVisibility(View.INVISIBLE);
                    pauseMusic();
                    mYfCloudPlayer.seekTo(mAudioStartTime);
                    break;
            }
        }
    };

    private void changeUIAndFinishRecord() {
        if (mYfVodKit.getVideoList().size() <= 0) return;
        if (mYfVodKit.isRecording()) {
            mYfVodKit.pauseRecord();
        }
        int totalRecordTime = mYfVodKit.getCurrentRecordTimestamp();
        Log.d(TAG, "totalRecordTime: " + totalRecordTime);
        if (totalRecordTime < MIN_RECORD_TIME) {
            Toast.makeText(SegmentRecordActivity2.this, "录制时间过短", Toast.LENGTH_SHORT).show();
            return;
        }
        mRangeBar.setRangePinsByIndices(0, totalRecordTime / 1000);
        mRangeBar.setTickEnd((float) (totalRecordTime / 1000));
        mEtEnd.setText(String.valueOf(totalRecordTime / 1000));
        mBtnCut.setText("视频合成中，请稍候...");
        mBtnCut.setEnabled(false);
        mLlPlayer.setVisibility(View.INVISIBLE);
//        mYfVodKit.enableReverseVideo(mCbReverse.isChecked());
        mYfVodKit.finishRecord();
    }

    @Override
    public void onInfo(int what, double arg1, double arg2, Object obj) {
        super.onInfo(what, arg1, arg2, obj);
//        Log.d(TAG, "onInfo: " + what);
        switch (what) {
            case YfVodKit.INFO_MERGE_START:
                break;
            case YfVodKit.INFO_MERGE_END:
                break;
            case YfVodKit.INFO_TRANSFORM_START:
                break;
            case YfVodKit.INFO_TRANSFORM_END:
                break;
            case YfVodKit.INFO_REVERSE_START:
                break;
            case YfVodKit.INFO_REVERSE_END:
                break;
            case YfVodKit.INFO_MUX_START:
                break;
            case YfVodKit.INFO_MUX_END:
                break;
            case YfVodKit.INFO_SPLIT_START:
                break;
            case YfVodKit.INFO_SPLIT_END:
                showToast((String) obj);
                FilterPlayerActivity.startPlayerActivity(this, (String) obj, mInputBitrate);
                finish();
                break;
        }
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SegmentRecordActivity2.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*private void onDone() {
        //所有流程走完之后
        releasePlayer();
        initPlayer();
    }*/

    /**
     * 回删录制片段
     * 这里需要重置时间戳到回退的时间点
     */
    private void deleteSegment() {
        if (mYfVodKit.isRecording()) return;
        Log.d(TAG, "mRecordSegments.size(): " + mYfVodKit.getVideoList().size());
        if (mYfVodKit.getVideoList().size() > 0) {
            mYfVodKit.deleteVideo(mYfVodKit.getVideoList().getLast().getId());
            mVideoProgressView.setCurrentState(VideoProgressView.State.DELETE);
        } else {
            mVideoProgressView.clearTimeList();
        }
    }

    private VideoProgressView.RecordProgressListener mOnProgressListener
            = new VideoProgressView.RecordProgressListener() {
        @Override
        public void onProgress(int progress) {
            if (progress == 100) {//录制总时长已达到MAX_RECORD_TIME，强制停止
                Toast.makeText(SegmentRecordActivity2.this, "录制已达最大时长",
                        Toast.LENGTH_SHORT).show();
                changeUIAndFinishRecord();
                Log.d(TAG, "mTotalRecordTime: " + mYfVodKit.getCurrentRecordTimestamp());
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_encoder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_start) {
            if (!mYfVodKit.isRecording()) {
                if (!mIsSelectAudioStartTime) {
                    Toast.makeText(this, "先选取音乐开始时间", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (mYfVodKit.getCurrentRecordTimestamp() >= MAX_RECORD_TIME) {
                    Toast.makeText(this, "录制已达最大时长", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (!prepared) return false;

                actionbarLayout.setBackgroundColor(getResources().getColor(R.color.red));
                mTvSelectStartTime.setVisibility(View.GONE);
                mYfVodKit.startRecord(1 / mSpeed);
            }
        } else if (id == R.id.action_stop) {
            actionbarLayout.setBackgroundColor(getResources().getColor(R.color.green));
            mYfVodKit.pauseRecord();
        } else {
            super.onOptionsItemSelected(item);
        }
        return false;
    }

    private YfCloudPlayer mYfCloudPlayer;

    private void initPlayer() {
        resetTag();
        if (mYfCloudPlayer == null)
            mYfCloudPlayer = YfCloudPlayer.Factory.createPlayer(this, YfCloudPlayer.MODE_SOFT);
        mYfCloudPlayer.setSpeed(1.0f);
        mYfCloudPlayer.setOnPreparedListener(onPreparedListener);
        mYfCloudPlayer.setOnInfoListener(new YfCloudPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(YfCloudPlayer yfCloudPlayer, int i, int i1) {
                switch (i) {
                    case YfCloudPlayer.INFO_CODE_AUDIO_RENDERING_START:
                        //第一次录制开始计时
                        Log.d(TAG, "INFO_CODE_AUDIO_RENDERING_START");
                        mYfVodKit.onAudioRender();//为了音视频能同步，需通知编码器音频已经开始渲染，此时正式开始编码视频
                        break;
                }
                return false;
            }
        });
        mYfCloudPlayer.setDisplay(null);
        try {
            mYfCloudPlayer.setDataSource(mAudioPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mYfCloudPlayer.prepareAsync();
    }


    @Override
    public void onError(int err) {
        switch (err) {
            case YfVodKit.ERROR_MERGE_FAILED:
                mBtnCut.setText("视频拼接失败...");
                break;
            case YfVodKit.ERROR_REVERSE_FAILED:
                mBtnCut.setText("时光倒流失败...");
                break;
            case YfVodKit.ERROR_MUX_FAILED:
                mBtnCut.setText("时光倒流失败...");
                break;
            case YfVodKit.ERROR_SPLIT_FAILED:
                mBtnCut.setText("视频裁剪失败...");
                break;
            case YfVodKit.ERROR_TRANSFORM_FAILED:
                mBtnCut.setText("音频转码失败...");
                break;

        }
    }


    @Override
    public void onStartRecording() {
        mVideoProgressView.setCurrentState(VideoProgressView.State.START);
        mUIHandler.removeCallbacks(updateProgress);
        mUIHandler.postDelayed(updateProgress, 100);
    }


    @Override
    public void onRecordPaused() {
        Log.d(TAG, "onRecordPaused: " + mYfVodKit.getCurrentRecordTimestamp());
        actionbarLayout.setBackgroundColor(getResources().getColor(R.color.green));
        mVideoProgressView.setCurrentState(VideoProgressView.State.PAUSE);
        mVideoProgressView.putTimeList(mYfVodKit.getCurrentRecordTimestamp());
    }

    @Override
    public void onFinish(String path) {
        mMuxPath = path;
        FilterPlayerActivity.startPlayerActivity(this, mMuxPath, mInputBitrate);
        finish();
//        mRlSegmentRecord.setVisibility(View.GONE);
//        mVideoProgressView.setVisibility(View.GONE);
//        mBtnCut.setText("开始裁剪");
//        mBtnCut.setEnabled(true);
//        showFrameThumbnails();
    }


    private void playMusic() {
        if (prepared) {
            Log.d(TAG, "开始播放音乐~~~");
            //每次播放音乐时记录当前时间点
            mYfCloudPlayer.setSpeed(mSpeed);
            mYfCloudPlayer.start();
            musicIsPaused = false;
        }
    }

    private void resetTag() {
        prepared = false;
        musicIsPaused = false;
        picCount = 0;
    }

    private boolean prepared, musicIsPaused;
    private long mAudioStartTime;
    private AudioCutView.SelectStartTimeListener mOnSelectStartTimeListener = new AudioCutView.SelectStartTimeListener() {
        @Override
        public void onSelectStartTime(long startTime) {
            Log.d(TAG, "onSelectStartTime: " + startTime);
            mAudioStartTime = startTime;
            mYfCloudPlayer.seekTo(startTime);
            mYfCloudPlayer.start();
        }
    };
    private Runnable mAddAudioSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            mAudioCutView = new AudioCutView(SegmentRecordActivity2.this);
            mAudioCutView.setAudioDuration(mAudioDuration);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, 240);
//            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            lp.addRule(RelativeLayout.ABOVE,R.id.ll_select_audio_buttons);
            mAudioCutView.setLayoutParams(lp);
            mAudioCutView.setOnSelectStartTimeListener(mOnSelectStartTimeListener);
            mRlSegmentRecord.addView(mAudioCutView);
        }
    };
    private YfCloudPlayer.OnPreparedListener onPreparedListener = new YfCloudPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(YfCloudPlayer yfCloudPlayer) {
            mAudioDuration = yfCloudPlayer.getDuration();
            mUIHandler.post(mAddAudioSelectViewRunnable);
            prepared = true;

        }
    };

    private void pauseMusic() {
        if (mYfCloudPlayer != null && mYfCloudPlayer.isPlaying()) {
            mYfCloudPlayer.pause();
            musicIsPaused = true;
        }
        mUIHandler.removeCallbacks(updateProgress);
    }

    private void releasePlayer() {
        if (mYfCloudPlayer != null) {
            mYfCloudPlayer.reset();
            mYfCloudPlayer.release();
            mYfCloudPlayer = null;
        }
        mUIHandler.removeCallbacks(updateProgress);
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        mYfVodKit.release();
        mUIHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mYfVodKit.onStop();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mYfVodKit.onStop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mYfVodKit.onResume();
    }

    protected String getDefaultUrl() {
        return URL_VOD;
    }
}
