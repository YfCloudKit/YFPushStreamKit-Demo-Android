package com.yunfan.encoder.demo.activity;


import android.annotation.SuppressLint;
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
import com.yunfan.encoder.demo.bean.VideoSegment;
import com.yunfan.encoder.demo.util.ExtractFrameWorkThread;
import com.yunfan.encoder.demo.util.ExtractVideoInfoUtil;
import com.yunfan.encoder.demo.util.Util;
import com.yunfan.encoder.demo.widget.AudioCutView;
import com.yunfan.encoder.demo.widget.VideoProgressView;
import com.yunfan.encoder.entity.YfVideo;
import com.yunfan.encoder.widget.VideoPtsSource;
import com.yunfan.encoder.widget.YfEncoderKit;
import com.yunfan.encoder.widget.YfMediaKit;
import com.yunfan.player.widget.YfCloudPlayer;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * 多段变速录制步骤：
 * 0、拖动绿色滑块选择音频起始时间，并点击确定
 * 1、设置播放器播放速度
 * 2、准备音乐
 * 3、自定义视频时间戳计时方式，并将时间戳大小校准设置为CHECK_ALWAYS
 * 4、开始录制
 * 5、推流回调第一帧视频帧可用时，立即播放音乐
 * 6、在音乐开始播放第一帧音频帧时，通知编码器开始正式编码视频
 * 7、停止第一段录制、暂停音乐
 * 8、保存当前音频播放位置
 * <p>
 * 9、设置音频播放速度并开始第二段录制
 * 10、推流回调第一帧视频帧可用时，立即播放音乐，因为播放器从暂停状态到播放状态速度很快，需立即通知编码器开始正式编码
 * 11、停止第二段录制、暂停音乐
 * 12、保存当前音频播放位置
 * <p>
 * 13、删除第二段录制的视频
 * 14、音乐播放器seek回步骤8里的位置
 * 15、编码器重置时间戳为步骤8里的位置
 * <p>
 * 16、重复9~15直至最大录制时长
 * 17、拼接多段视频
 * 18、将音频文件转码为大多数设备支持的播放格式
 * 19、合并音视频文件
 * 20、裁剪合并后的文件
 */
public class SegmentRecordActivity extends LiveRecorderActivity {
    public static String AUDIO_PATH = "AUDIO_PATH";
    private static final String TAG = "Yf_SegmentActivity";
    public static final float MAX_RECORD_TIME = 15 * 1000f;  //设置录制的最大时间.
    public static final float MIN_RECORD_TIME = 5 * 1000f;   //录制的最小时间
    private float mSpeed = 1f;
    private VideoProgressView mVideoProgressView;
    private TextView mTvMerge;
    private TextView mTvDelete;
    private Button mBtnCut;
    private RangeBar mRangeBar;
    private int mTotalRecordTime;
    private RecyclerView mRecyclerView;
    private ImageView mCurrentLeftPic, mCurrentRightPic;
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private VideoEditAdapter videoEditAdapter;
    private LinkedList<VideoSegment> mRecordSegments = new LinkedList<>();
    private LinkedList<Long> mMusicPositionList = new LinkedList<>();

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
            mVideoProgressView.setSpeed(mSpeed);
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
            Glide.with(SegmentRecordActivity.this)
                    .load("file://" + videoEditAdapter.getVideoEditInfoList().get(leftPicIndex).path)
                    .into(mCurrentLeftPic);
            Glide.with(SegmentRecordActivity.this)
                    .load("file://" + videoEditAdapter.getVideoEditInfoList().get(rightPicIndex).path)
                    .into(mCurrentRightPic);
        }
    };

    private EditText mEtStart;
    private EditText mEtEnd;
    private long mSeekToPosition = -1;
    private FrameLayout mFlSegmentRecord;
    private long mAudioDuration;
    private TextView mTvSelectStartTime;
    private TextView mTvSelectOK;
    private long mSelectStartTime;
    private AudioCutView mAudioCutView;
    private boolean mIsSelectAudioStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    private void initConfig() {
        VIDEO_WIDTH = 960;
        VIDEO_HEIGHT = 540;
        VIDEO_FRAME_RATE = 60;

    }

    private String mAudioPath, mMergedPath, mMuxPath, mSplitPath, mTranscodedAudioPath;

    private void initMediaKitAndPath() {
        mYfMediaKit = new YfMediaKit(mediaCallback);
        mAudioPath = getIntent().getStringExtra(AUDIO_PATH);//原始音频文件路径
        mTranscodedAudioPath = CACHE_DIRS + "/" + mAudioPath.substring(
                mAudioPath.lastIndexOf("/") + 1, mAudioPath.lastIndexOf(".")) + ".mp4";//转码后的音频文件保存路径

        final String mediaPath = CACHE_DIRS + "/" + mUrl.getText().toString() + getFormatTime();
        mMuxPath = mediaPath + "_mux.mp4";//音视频合成后的文件保存路径
        mMergedPath = mediaPath + "_merge.mp4";//视频拼接后的文件保存路径
        mSplitPath = mediaPath + "_split.mp4";//视频裁剪后的文件保存路径
    }

    @Override
    protected void initView() {
        super.initView();
        //录制
        mFlSegmentRecord = (FrameLayout) findViewById(R.id.fl_segment_record);
        mFlSegmentRecord.setVisibility(View.VISIBLE);
        mRlSegmentRecord = (RelativeLayout) findViewById(R.id.rl_segment_record);
        mVideoProgressView = (VideoProgressView) findViewById(R.id.recorder_progress);
        mVideoProgressView.setOnProgressListener(mOnProgressListener);

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

        initConfig();
        initMediaKitAndPath();
        initPlayer();
        Log.d(TAG, "mAudioPath: " + mAudioPath);
    }

    private YfMediaKit mYfMediaKit;
    private final int SUCCESS = YfMediaKit.RESULT_SUCCESS;
    private final int MERGE_INDEX = 10086, MUX_INDEX = 10000, SPLIT_INDEX = 10010, TRANSCODE_INDEX = 11111;//用于标识任务ID，必须定义为不一样的数值以作区分
    private YfMediaKit.MediaKitCallback mediaCallback = new YfMediaKit.MediaKitCallback() {

        @Override
        public void onMediaHandledFinish(final int id, final int result, String path) {
            //拼接视频、音视频合成、裁剪
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, String.format("id: %d result: %d", id, result));
                    switch (id) {
                        case MERGE_INDEX:
                            onMergeFinish(result);
                            break;
                        case TRANSCODE_INDEX:
                            onTransCodeFinish(result);
                            break;
                        case MUX_INDEX:
                            onMuxFinish(result);
                            break;
                        case SPLIT_INDEX:
                            onSplitFinish(result);
                            break;
                    }
                }
            });

        }
    };

    private void onMergeFinish(int result) {
        if (result == SUCCESS) {
            if (transforming) {
                return;
            } else if (transformSuccess) {
                //音频转码
                onTransCodeFinish(SUCCESS);
            } else {
                startTransformMusic();
            }
        } else {
            mBtnCut.setText("视频合成失败...");
        }
    }


    /**
     * 在音频准备好之后就开始对音频进行转码；如果转码失败，则在最后音视频mux之前再转码一次。
     * 这里需要使用一些标志位去同步过程。
     */
    private void startTransformMusic() {
        if (new File(mTranscodedAudioPath).exists()) {
            transformSuccess = true;
            return;
        }
        transforming = true;
        transformSuccess = false;
        mYfMediaKit.transcodeMedia(mAudioPath, mTranscodedAudioPath, TRANSCODE_INDEX);
        Log.d(TAG, "startTransformMusic: " + mAudioPath);
    }

    private boolean transformSuccess, transforming;

    private void onTransCodeFinish(int result) {
        transforming = false;
        if (result == SUCCESS) {
            transformSuccess = true;
            if (new File(mMergedPath).exists())
                mYfMediaKit.muxMedia(mTranscodedAudioPath, mMergedPath, mMuxPath, mSelectStartTime / 1000d, MUX_INDEX);
        } else {
            transformSuccess = false;
            mBtnCut.setText("音频转码失败...");
        }
    }

    private void onMuxFinish(int result) {
        if (result == SUCCESS) {
            mRlSegmentRecord.setVisibility(View.GONE);
            mVideoProgressView.setVisibility(View.GONE);
            mBtnCut.setText("开始裁剪");
            mBtnCut.setEnabled(true);
            showFrameThumbnails();
        } else {
            mBtnCut.setText("混流失败...");
        }
    }

    private void onSplitFinish(int result) {
        if (result == SUCCESS) {
            Toast.makeText(SegmentRecordActivity.this, "视频保存成功", Toast.LENGTH_SHORT).show();
            onDone();
        } else {
            Toast.makeText(SegmentRecordActivity.this, "视频保存失败", Toast.LENGTH_SHORT).show();
        }
    }


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
    public static final int SHOW_IMG_SUCCESS = 0;

    @SuppressLint("HandlerLeak")
    private class MainHandler extends Handler {
        private final WeakReference<SegmentRecordActivity> mActivity;

        MainHandler(SegmentRecordActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SegmentRecordActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case SHOW_IMG_SUCCESS:
                        if (activity.videoEditAdapter != null) {
                            VideoEditInfo info = (VideoEditInfo) msg.obj;
                            activity.videoEditAdapter.addItemVideoInfo(info);
                            activity.picCount++;
                            if (activity.picCount == activity.thumbnailsCount) {
                                List<VideoEditInfo> videoEditInfoList = videoEditAdapter.getVideoEditInfoList();
                                if (videoEditInfoList.isEmpty()) return;
                                Glide.with(SegmentRecordActivity.this)
                                        .load("file://" + videoEditAdapter.getVideoEditInfoList().get(0).path)
                                        .into(mCurrentLeftPic);
                                Glide.with(SegmentRecordActivity.this)
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
//            Log.d(TAG, "mSeekToPosition: " + mSeekToPosition + " PTS: " + (int) (yfEncoderKit.getFramePTS() / 1000));
            if (yfEncoderKit == null) return;
            mVideoProgressView.updateProgress((int) (yfEncoderKit.getFramePTS() / 1000) - (int) mSelectStartTime);
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
                    if (mRecordSegments.isEmpty()) return;
                    if (yfEncoderKit.isRecording()) {
                        stopRecorder();
                        pauseMusic();
                    }
                    if (mTotalRecordTime < MIN_RECORD_TIME) {
                        Toast.makeText(SegmentRecordActivity.this, "录制时间过短", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mRangeBar.setRangePinsByIndices(0, mTotalRecordTime / 1000);
                    mRangeBar.setTickEnd((float) (mTotalRecordTime / 1000));
                    mEtEnd.setText(String.valueOf(mTotalRecordTime / 1000));
                    mBtnCut.setText("视频合成中，请稍候...");
                    mBtnCut.setEnabled(false);
                    mLlPlayer.setVisibility(View.VISIBLE);

                    List<YfVideo> list = new ArrayList<>();
                    for (VideoSegment segment : mRecordSegments) {
                        final YfVideo temp = new YfVideo();
                        temp.setPath(segment.getAbsolutePath());
                        list.add(temp);
                    }

                    Toast.makeText(SegmentRecordActivity.this, "视频合成：" + mMergedPath, Toast.LENGTH_SHORT).show();
                    mYfMediaKit.mergeMedia(list, mMergedPath, 0,MERGE_INDEX);
                    break;
                case R.id.btn_video_cut:
                    Toast.makeText(SegmentRecordActivity.this, "开始裁剪视频...", Toast.LENGTH_SHORT).show();
                    int cutStartTime = Integer.valueOf(mEtStart.getText().toString());
                    int cutEndTime = Integer.valueOf(mEtEnd.getText().toString());

                    Toast.makeText(SegmentRecordActivity.this, "视频合成：" + mSplitPath, Toast.LENGTH_SHORT).show();
                    mYfMediaKit.splitMedia(mMuxPath, cutStartTime, cutEndTime, mSplitPath, SPLIT_INDEX);
                    break;
                case R.id.tv_select_start_time:
                    mTvSelectOK.setVisibility(View.VISIBLE);
                    if (mAudioCutView != null) mAudioCutView.setVisibility(View.VISIBLE);
                    break;
                case R.id.tv_select_ok:
                    mIsSelectAudioStartTime = true;
                    mTvSelectOK.setVisibility(View.GONE);
                    if (mAudioCutView != null) mAudioCutView.setVisibility(View.INVISIBLE);
                    pauseMusic();
                    Log.d(TAG, "start time:" + mSelectStartTime / 1000d);
                    mYfCloudPlayer.seekTo(mSelectStartTime);
                    break;
            }
        }
    };

    private void onDone() {
        //所有流程走完之后
        releasePlayer();
        initPlayer();
    }

    /**
     * 回删录制片段
     * 这里需要重置时间戳到回退的时间点
     */
    private void deleteSegment() {
        if (yfEncoderKit.isRecording()) return;
        Log.d(TAG, "mRecordSegments.size(): " + mRecordSegments.size());
        if (mRecordSegments.size() > 0) {
            VideoSegment deleteSegment = mRecordSegments.removeLast();
            mTotalRecordTime -= deleteSegment.getDuring();
            mVideoProgressView.setCurrentState(VideoProgressView.State.DELETE);
            boolean b = Util.deleteFile(deleteSegment.getAbsolutePath());
            Log.d(TAG, "delete: " + deleteSegment.getAbsolutePath() + " result: " + b);
            mSeekToPosition = mMusicPositionList.getLast();
            Log.d(TAG, "seekTo: " + mSeekToPosition);
            mYfCloudPlayer.seekTo(mSeekToPosition);
            yfEncoderKit.resetPTS(mSeekToPosition * 1000);//重置时间戳到回退的时间点
            if (mMusicPositionList.size() > 0) mMusicPositionList.removeLast();
        } else {
            mVideoProgressView.clearTimeList();
        }
    }

    private VideoProgressView.RecordProgressListener mOnProgressListener
            = new VideoProgressView.RecordProgressListener() {
        @Override
        public void onProgress(int progress) {
            if (progress == 100) {//录制总时长已达到MAX_RECORD_TIME，强制停止
                Toast.makeText(SegmentRecordActivity.this, "录制已达最大时长",
                        Toast.LENGTH_SHORT).show();
                stopRecorder();
                pauseMusic();
                Log.d(TAG, "mTotalRecordTime: " + mTotalRecordTime);
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
            if (!yfEncoderKit.isRecording()) {
                if (!mIsSelectAudioStartTime) {
                    Toast.makeText(this, "先选取音乐开始时间", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (mTotalRecordTime >= MAX_RECORD_TIME) {
                    Toast.makeText(this, "录制已达最大时长", Toast.LENGTH_SHORT).show();
                    return false;
                }
                startRecorder(true);
            }
        } else if (id == R.id.action_stop) {
            if (yfEncoderKit.isRecording()) {
                pauseMusic();
                stopRecorder();
            }
        } else {
            super.onOptionsItemSelected(item);
        }
        return false;
    }

    private YfCloudPlayer mYfCloudPlayer;

    private void initPlayer() {
        resetTag();
        if (mYfCloudPlayer == null)
            mYfCloudPlayer = YfCloudPlayer.Factory.createPlayer(this, 1);
        mYfCloudPlayer.setSpeed(1.0f);
        mYfCloudPlayer.setOnPreparedListener(onPreparedListener);
        mYfCloudPlayer.setOnInfoListener(new YfCloudPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(YfCloudPlayer yfCloudPlayer, int i, int i1) {
                switch (i) {
                    case YfCloudPlayer.INFO_CODE_AUDIO_RENDERING_START:
                        //第一次录制开始计时
                        Log.d(TAG, "INFO_CODE_AUDIO_RENDERING_START");
                        yfEncoderKit.notifyInputAudioStartRender();//为了音视频能同步，需通知编码器音频已经开始渲染，此时正式开始编码视频
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
    public void onInfo(int what, double arg1, double arg2, Object obj) {
        switch (what) {
            case YfEncoderKit.INFO_FIRST_VIDEO_FRAME_AVAILABLE:
                mVideoProgressView.setCurrentState(VideoProgressView.State.START);
                Log.d(TAG, "INFO_FIRST_VIDEO_FRAME_AVAILABLE: ");
                if (musicIsPaused) {
                    //音乐是从暂停状态切换到播放状态的话，播放速度很快且不会有INFO_CODE_AUDIO_RENDERING_START回调，因此直接通知编码器立即开始编码视频
                    yfEncoderKit.notifyInputAudioStartRender();
                }
                playMusic();//第一帧视频帧开始采集的同时开始播放音乐。
                mUIHandler.removeCallbacks(updateProgress);
                mUIHandler.postDelayed(updateProgress, 100);
                return;
            case YfEncoderKit.INFO_STOP_VIDEO_FRAME:
                updateTotalTime((int) arg1);
                break;
        }
        super.onInfo(what, arg1, arg2, obj);
    }

    @Override
    public void onStateChanged(int mode, int oldState, int newState) {
        super.onStateChanged(mode, oldState, newState);
    }

    @Override
    public void onFragment(int mode, String fragPath, boolean success) {
        super.onFragment(mode, fragPath, success);
    }

    private void playMusic() {
        startPlayMusic = true;
        if (prepared) {
            Log.d(TAG, "开始播放音乐~~~");
            //每次播放音乐时记录当前时间点
            mMusicPositionList.add(mYfCloudPlayer.getCurrentPosition());
            mYfCloudPlayer.setSpeed(mSpeed);
            mYfCloudPlayer.start();
            musicIsPaused = false;
        }
    }

    private void resetTag() {
        prepared = false;
        startPlayMusic = false;
        musicIsPaused = false;
        picCount = 0;
        mMusicPositionList.clear();

    }

    private boolean prepared, startPlayMusic, musicIsPaused;
    private AudioCutView.SelectStartTimeListener mOnSelectStartTimeListener = new AudioCutView.SelectStartTimeListener() {
        @Override
        public void onSelectStartTime(long startTime) {
            Log.d(TAG, "onSelectStartTime: " + startTime);
            mSelectStartTime = startTime;
            mYfCloudPlayer.seekTo(startTime);
            mYfCloudPlayer.start();
        }
    };
    private Runnable mAddAudioSelectViewRunnable = new Runnable() {
        @Override
        public void run() {
            mAudioCutView = new AudioCutView(SegmentRecordActivity.this);
            mAudioCutView.setAudioDuration(mAudioDuration);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, 240);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
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
            if (startPlayMusic) {
                playMusic();
            }
            startTransformMusic();

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
        super.onDestroy();
        mUIHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "clearCacheFiles: " + videoEditAdapter.getVideoEditInfoList().size());
        for (VideoEditInfo videoEditInfo : videoEditAdapter.getVideoEditInfoList()) {
            boolean b = Util.deleteFile(videoEditInfo.path);
            Log.d(TAG, b + "clearCacheFiles: " + videoEditInfo.path);
        }
        releasePlayer();
    }


    private void updateTotalTime(int totalTime) {
        Log.d(TAG, "updateTotalTime: " + totalTime);
        Log.d(TAG, "getFramePTS: " + yfEncoderKit.getFramePTS());
        totalTime -= mSelectStartTime;
        if (mRecordSegments.size() > 0) {
//            int currentSegmentTime = (int) (System.currentTimeMillis() - mStartTime);
//            int changeCurrentSegmentTime = (int) (currentSegmentTime * mSpeed);
            if (mRecordSegments.size() == 1) {
                mRecordSegments.getLast().setDuring(totalTime);//第一次录制时的片段时间就是dts
            } else {
                long during = mRecordSegments.get(mRecordSegments.size() - 2).getDuring();//第二次录制及以后的片段时间计算
                mRecordSegments.getLast().setDuring(totalTime - during);
            }
            mTotalRecordTime = totalTime;
            mVideoProgressView.setCurrentState(VideoProgressView.State.PAUSE);
            mVideoProgressView.putTimeList(mTotalRecordTime);
        }
    }


    @Override
    protected void onStop() {
        if (mYfCloudPlayer != null) pauseMusic();
        stopRecorder();
        needAutoRecord = false;
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mYfCloudPlayer != null && musicIsPaused)
            mYfCloudPlayer.seekTo(yfEncoderKit.getFramePTS() / 1000);//onStop()方法有一定延迟，因此在恢复的时候要同步到视频最后一次获取的时间戳上。
    }

    protected String getDefaultUrl() {
        return URL_VOD;
    }

    protected void startRecorder(boolean changeUI) {
        startRecorderInternal();
    }


    private String createNewSegment() {
        String fileName = mUrl.getText().toString() + getFormatTime();
        VideoSegment videoSegment = new VideoSegment(0,
                String.format("%s/Record/%s.mp4", CACHE_DIRS, fileName));
        mRecordSegments.add(videoSegment);
        return fileName;
    }

    protected void startRecorderInternal() {
        if (!prepared) {
            return;
        }
        mTvSelectStartTime.setVisibility(View.GONE);
        String fileName = createNewSegment();
        yfEncoderKit.setStreamType(YfEncoderKit.STREAM_TYPE_VIDEO_ONLY);//不需要录制音频，录制纯视频后再进行音视频合成
        yfEncoderKit.setVideoPtsSource(new VideoPtsSource() {
            @Override
            public int getContinuityCheck() {
                return VideoPtsSource.CHECK_ALWAYS;
            }

            @Override
            public long getVideoPts() {
                return mYfCloudPlayer.getCurrentPosition() * 1000;//将视频同步到音频的进度上，单位为微秒。
            }
        });


        yfEncoderKit.changeMode(YfEncoderKit.MODE_VOD, (int) (mInputBitrate * mSpeed));
        yfEncoderKit.setIFrameInternal(1 / mSpeed);//I帧间隔默认为1s，为了提高split精度，要根据播放速度去设置I帧间隔。
        if (mSpeed > 1)
            yfEncoderKit.setFrameRate(60);//默认20帧，当快录慢播时应提高帧率以提高视频流畅度。
        else
            yfEncoderKit.setFrameRate(20);
        yfEncoderKit.setVodSaveName(fileName);
        yfEncoderKit.startRecord();

    }


    private String getFormatTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }
}
