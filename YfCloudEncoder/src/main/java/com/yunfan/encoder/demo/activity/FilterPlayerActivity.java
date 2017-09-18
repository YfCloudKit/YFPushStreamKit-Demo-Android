package com.yunfan.encoder.demo.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.yfcloudencoder.R;
import com.kuaipai.fangyan.core.shooting.jni.RecorderJni;
import com.yunfan.encoder.demo.adapter.VideoEditAdapter;
import com.yunfan.encoder.demo.bean.VideoEditInfo;
import com.yunfan.encoder.demo.http.OkHttpHelper;
import com.yunfan.encoder.demo.http.Server;
import com.yunfan.encoder.demo.util.ExtractFrameWorkThread;
import com.yunfan.encoder.demo.util.ExtractVideoInfoUtil;
import com.yunfan.encoder.demo.util.Log;
import com.yunfan.encoder.demo.util.Util;
import com.yunfan.encoder.demo.widget.YfController;
import com.yunfan.encoder.demo.widget.YfPopupWindow;
import com.yunfan.encoder.filter.BaseFilter;
import com.yunfan.encoder.filter.FaceUnityFilter;
import com.yunfan.encoder.filter.YfAntiqueFilter;
import com.yunfan.encoder.filter.YfCrayonFilter;
import com.yunfan.encoder.filter.YfFilterFactory;
import com.yunfan.encoder.filter.YfGifFilter;
import com.yunfan.encoder.filter.YfSketchFilter;
import com.yunfan.encoder.filter.YfSoulDazzleFilter;
import com.yunfan.encoder.filter.YfWaterMarkFilter;
import com.yunfan.encoder.filter.YfWaveFilter;
import com.yunfan.encoder.filter.YfWhiteCatFilter;
import com.yunfan.encoder.filter.entity.TimeSection;
import com.yunfan.encoder.widget.YfGlSurfaceView;
import com.yunfan.encoder.widget.YfKitFactory;
import com.yunfan.encoder.widget.YfMediaEditor;
import com.yunfan.encoder.widget.YfMediaKit;
import com.yunfan.player.widget.YfCloudPlayer;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.yunfan.encoder.demo.activity.LiveRecorderActivity.CACHE_DIRS;


/**
 * Created by 37917 on 2017/6/19 0019.
 */

public class FilterPlayerActivity extends AppCompatActivity implements YfController.YfControl, View.OnClickListener {
    private final String TAG = "Yf_FilterPlayerActivity";
    private YfGlSurfaceView mYfGlSurfaceView;
    private YfCloudPlayer mYfCloudPlayer;
    private String mCurrentPath, mFlashbackPath, mNormalPath;
    public static final String VIDEO_PATH = "video-path";
    public static final String BITRATE = "bitrate";
    private YfWaterMarkFilter mWaterMarkFilter;
    private YfGifFilter mGifFilter;

    private FaceUnityFilter mFaceUnityFilter;
    private YfCrayonFilter mYfCrayonFilter;
    private YfSketchFilter mYfSketchFilter;
    private RecyclerView mRecyclerView;
    private ExtractVideoInfoUtil mExtractVideoInfoUtil;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private VideoEditAdapter videoEditAdapter;
    private Button mStartBtn;
    private YfController mController;
    private Button mBeautyBtn;
    private Button mArBtn;
    private Button mLogoBtn;
    private String[] mArItems, mArNames, mFilterItems, mFilterNames;
    private YfPopupWindow mArPopupWindow, mFilterPopupWindow;
    private TextView mOutputBtn;
    private ImageView mPreview;
    private YfWhiteCatFilter mYfWhiteCatFilter;
    private YfAntiqueFilter mYfAntiqueFilter;
    private YfWaveFilter mYfWaveFilter;
    private YfSoulDazzleFilter mYfShakeFilter;
    private long mDuration;
    private LinearLayout mLlBottom;
    private final String mFilterVideoPath = "/sdcard/yunfanencoder/Record/testVideo-"+Util.getFormatTime()+".mp4" ;
    private String mInputTrailerPath = "/sdcard/yunfanencoder/Record/inputTrailer.mp4";
    private int mBitrate;
    YfMediaEditor mYfEditor;
    private YfFilterFactory.Factory mYfFilterFactory;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_player);

        mYfGlSurfaceView = (YfGlSurfaceView) findViewById(R.id.yf_surface);
        mRecyclerView = (RecyclerView) findViewById(R.id.id_rv);
        mLlBottom = (LinearLayout) findViewById(R.id.recorder_bottom);
        mStartBtn = (Button) findViewById(R.id.start_pause);
        mOutputBtn = (TextView) findViewById(R.id.btn_output);
        mBeautyBtn = (Button) findViewById(R.id.btn_filter);
        mArBtn = (Button) findViewById(R.id.btn_face_u);
        mLogoBtn = (Button) findViewById(R.id.btn_logo);
        mPreview = (ImageView) findViewById(R.id.preview);
        mBeautyBtn.setOnClickListener(this);
        mArBtn.setOnClickListener(this);
        mLogoBtn.setOnClickListener(this);
        mOutputBtn.setOnClickListener(this);


        mController = (YfController) findViewById(R.id.controller);
        mController.setPlayer(this);
        mController.show(0);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mRecyclerView.setLayoutManager(linearLayoutManager);
        int picWidth = (int) (Util.getDisplayWidth(this) / /*(MAX_RECORD_TIME / 1000 / 2)*/15);
        videoEditAdapter = new VideoEditAdapter(this, picWidth);
        mRecyclerView.setAdapter(videoEditAdapter);

        mLandscape = false;
        mYfGlSurfaceView.init();
        mYfGlSurfaceView.setYfRenderCallback(mRenderCallback);
        mYfGlSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startPause(mStartBtn);
                }
                return false;
            }
        });
        RecorderJni.getInstance();
        mNormalPath = mCurrentPath = getIntent().getStringExtra(VIDEO_PATH);
        if (mNormalPath == null)
            mNormalPath = mCurrentPath = "/sdcard/yunfanencoder/07_28_10_15_58_mux.mp4";
        mBitrate = 1500;//默认输出3M大小的视频
        mFlashbackPath = mCurrentPath.replace(".mp4", "_reverse.mp4");
        showFrameThumbnails(mCurrentPath);
        initMediaKit();
        if (!new File(mFlashbackPath).exists())
            reverseMedia();
        if (!new File(mInputTrailerPath).exists())//预先复制片尾到SDcard
            Util.CopyAssets(FilterPlayerActivity.this, "368-640.mp4", mInputTrailerPath);

        try {
            mYfFilterFactory= new YfFilterFactory.Factory(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void initMediaKit() {
        if (mMediaKit == null) mMediaKit = new YfMediaKit(mMediaKitCallback);
    }


    public static void startPlayerActivity(Context context, String videoPath, int bitrate) {
        Intent i = new Intent(context, FilterPlayerActivity.class);
        i.putExtra(VIDEO_PATH, videoPath);
        i.putExtra(BITRATE, bitrate);
        context.startActivity(i);
    }

    private void showFrameThumbnails(String path) {
        if (!new File(path).exists()) {
            Log.d(TAG, "视频文件不存在:" + path);
            return;
        }
        if (mExtractVideoInfoUtil == null)
            mExtractVideoInfoUtil = new ExtractVideoInfoUtil(path);
        long endPosition = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());
        long startPosition = 0;
        thumbnailsCount =/*(int) (MAX_RECORD_TIME / 1000 / 2)*/15;//2秒截取一帧
        int extractW = (Util.getDisplayWidth(this)) / 4;
        int extractH = Util.dip2px(this, 55);
        mExtractFrameWorkThread = new ExtractFrameWorkThread(
                extractW, extractH, mUIHandler, path,
                CACHE_DIRS + "/Thumbnails", startPosition, endPosition, thumbnailsCount);
        mExtractFrameWorkThread.start();
    }

    private final MainHandler mUIHandler = new MainHandler(this);
    private int thumbnailsCount, picCount;
    public static final int SHOW_IMG_SUCCESS = 0;
    public static final int UPLOAD_PROGRESS = 1;

    @Override
    public int getDuration() {
        return mYfCloudPlayer == null ? 0 : (int) mYfCloudPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mYfCloudPlayer == null ? 0 : (int) mYfCloudPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        if (mYfCloudPlayer != null) mYfCloudPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mYfCloudPlayer != null && mYfCloudPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_filter:
                showFilterPopupWindow();
                break;
            case R.id.btn_face_u:
                showArPopupWindow();
                break;
            case R.id.btn_logo:
                showMoreMenu();
                break;
            case R.id.btn_output:
                hideControlUI(true);
                Toast.makeText(this, "开始合成...", Toast.LENGTH_SHORT).show();
                try {
                    mYfEditor =   new YfKitFactory.Factory(FilterPlayerActivity.this).buildYfMediaEditor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                List<BaseFilter> configFilterList = new ArrayList<>();
                for (BaseFilter filter : mPreviewFilters) {
                    BaseFilter configFilter = getConfigFilter(filter.getIndex());
                    configFilter.setRenderSections(filter.getRenderSections());
                    configFilterList.add(configFilter);
                }
                if (mAddGif) {
                    configFilterList.add(createGifFilter(mYfCloudPlayer.getVideoWidth()));
                }
                if (mAddLogo) {
                    configFilterList.add(createWaterMarkFilter(mYfCloudPlayer.getVideoWidth()));
                }
                mYfEditor.addFilters(configFilterList);
                mYfEditor.setSize(mYfCloudPlayer.getVideoWidth(), mYfCloudPlayer.getVideoHeight());
                mYfEditor.setBitrate(mBitrate);

//                editor.encodeHEVC(true);//输出H265视频
                mYfEditor.setSaveProgressListener(mDuration, mProgressListener);
                try {
                    mYfEditor.startEdit(this, mCurrentPath, mFilterVideoPath);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    private YfMediaEditor.SaveProgressListener mProgressListener
            = new YfMediaEditor.SaveProgressListener() {
        @Override
        public void onSaveProgress(int progress) {
            mOutputBtn.setText("合成中" + progress + "%...");
            if (progress == 100) {
                mOutputBtn.setEnabled(true);
                mOutputBtn.setText("上传");
                mOutputBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setEnabled(false);
                        Server.POST_UPLOAD_FILE(new File(mFilterVideoPath), mFilterVideoPath.substring(mFilterVideoPath.lastIndexOf("/") + 1), mUploadCallback, mUploadListener);
                    }
                });

            }
        }
    };

    Callback mUploadCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Log.d(TAG, "onFailure:" + call.toString() + ",io exception:" + e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FilterPlayerActivity.this,"上传失败",Toast.LENGTH_SHORT).show();
                    onDone();
                }
            });
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            Log.d(TAG, "onResponse:" + response.toString() + ",response.body.toString:" + response.body().string() + ",response.message:" + response.message());
        }
    };
    OkHttpHelper.ProgressListener mUploadListener = new OkHttpHelper.ProgressListener() {
        @Override
        public void onProgress(long totalBytes, long remainingBytes, boolean done) {

            int percent = (int) ((totalBytes - remainingBytes) * 100 / totalBytes);
            if (percent % 5 == 0) {
                Message msg = mUIHandler.obtainMessage(UPLOAD_PROGRESS);
                msg.arg1 = percent;
                mUIHandler.sendMessage(msg);
            }
        }
    };

    private void onDone() {
        hideControlUI(false);
        mPreviewFilters.clear();
        if (mAddGif) {
            mYfGlSurfaceView.removeFilter(GIF_INDEX);
            mAddGif = false;
        }
        if (mAddLogo) {
            mYfGlSurfaceView.removeFilter(LOGO_INDEX);
            mAddLogo = false;
        }
        openVideo(mFilterVideoPath);
    }

    private final int TRAILER_INDEX = 888;


    private YfMediaKit.MediaKitCallback mMediaKitCallback = new YfMediaKit.MediaKitCallback() {
        @Override
        public void onMediaHandledFinish(int id, int result, final String path) {
            switch (id) {
                case TRAILER_INDEX:


                    break;
                case REVERSE_INDEX:
                    mReversing = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mFlashback) {
                                isPlayingFlashBack = true;
                                openVideo(path);
                                videoEditAdapter.reverseItem();
                                Toast.makeText(FilterPlayerActivity.this, "时光倒流成功，开始播放", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    break;
            }
        }
    };

    private void hideControlUI(boolean hide) {
        if (hide) {
            mOutputBtn.setText("准备中...");
            mOutputBtn.setEnabled(false);
        } else {
            mOutputBtn.setVisibility(View.GONE);
            mOutputBtn.setText("保存");
            mController.clear();
            mPreviewFilters.clear();
        }
        mLlBottom.setVisibility(View.GONE);
    }

    AlertDialog.Builder mMoreMenu;
    private boolean mAddLogo, /*mAddTrailer,*/
            mFlashback, mAddGif;
    private boolean isPlayingFlashBack, isAddedLogo, isAddedGif;

    private void showMoreMenu() {
        if (mMoreMenu == null) {
            mMoreMenu = new AlertDialog.Builder(this)
                    .setTitle("更多编辑")
                    .setMultiChoiceItems(new String[]{"水印", /*"添加片尾",*/ "时光倒流", "添加动图"},
                            new boolean[]{mAddLogo, /*mAddTrailer, */mFlashback, mAddGif},
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                    switch (which) {
                                        case 0:
                                            mAddLogo = isChecked;
                                            break;
                                        case 1:
                                            mFlashback = isChecked;
                                            break;
                                        case 2:
                                            mAddGif = isChecked;
                                            break;

                                    }
                                }
                            })
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!isAddedLogo && mAddLogo) {//添加水印
                                addLogo();
                                isAddedLogo = true;
                            } else if (isAddedLogo && !mAddLogo) {
                                mYfGlSurfaceView.removeFilter(LOGO_INDEX);
                                isAddedLogo = false;
                            }

                            if (!isAddedGif && mAddGif) {//添加gif
                                addGif();
                                isAddedGif = true;
                            } else if (isAddedGif && !mAddGif) {
                                mYfGlSurfaceView.removeFilter(GIF_INDEX);
                                isAddedGif = false;
                            }

                            if (mFlashback && !isPlayingFlashBack) {//时光倒流
                                if (new File(mFlashbackPath).exists()) {
                                    isPlayingFlashBack = true;
                                    videoEditAdapter.reverseItem();
                                    openVideo(mFlashbackPath);
                                } else {
                                    reverseMedia();
                                    Toast.makeText(FilterPlayerActivity.this, "开始时光倒流，倒流成功会立即播放，请稍候", Toast.LENGTH_SHORT).show();
                                }
                            } else if (!mFlashback && isPlayingFlashBack) {
                                isPlayingFlashBack = false;
                                openVideo(mNormalPath);
                                videoEditAdapter.reverseItem();
                            }


                        }
                    });

        }

        mMoreMenu.show();

    }

    YfMediaKit mMediaKit;
    private static final int REVERSE_INDEX = 101;
    private boolean mReversing;

    private void reverseMedia() {
        if (!mReversing) {
            mReversing = true;
            mMediaKit.reverseMedia(mNormalPath, mFlashbackPath, REVERSE_INDEX);
        }
    }

    private void showArPopupWindow() {
        mArItems = getResources().getStringArray(R.array.ar);
        mArNames = getResources().getStringArray(R.array.ar_name);
        if (mArPopupWindow == null) {
            mArPopupWindow = new YfPopupWindow(this, mArNames, mLandscape, true);
            mArPopupWindow.setOnSelectedListener(mOnArChangeListener);
        }
        mArPopupWindow.showAtLocation(mArBtn, Gravity.BOTTOM, 0, 0);
    }

    private void showFilterPopupWindow() {
        mFilterItems = getResources().getStringArray(R.array.filter_name);
        if (mFilterPopupWindow == null) {
            mFilterPopupWindow = new YfPopupWindow(this, mFilterItems, mLandscape, true);
            mFilterPopupWindow.setOnPressedListener(mOnFilterChangeListener);
        }
        mFilterPopupWindow.showAtLocation(mArBtn, Gravity.BOTTOM, 0, 0);
    }

    private int mArItemPosition;
    private YfPopupWindow.OnSelectedListener mOnArChangeListener
            = new YfPopupWindow.OnSelectedListener() {
        @Override
        public void onSelected(int position) {
            if (mArItemPosition == position && setAr) {
                mYfGlSurfaceView.removeFilter(FACE_INDEX);
                setAr = false;
                return;
            }
            mArItemPosition = position;
            Log.d(TAG, "onArChanged: " + position + "," + mArItems[position]);
            if (!setAr)
                addAR(mArItems[position]);
            else
                mFaceUnityFilter.setEffect(mArItems[position], false);
        }
    };

    private int[] colors = new int[]{R.color.red, R.color.green, R.color.blue, R.color.black, R.color.colorEditUnderLine, R.color.colorDotYellow};
    private YfPopupWindow.OnPressedListener mOnFilterChangeListener = new YfPopupWindow.OnPressedListener() {
        @Override
        public void onPressed(int position, boolean pressed) {
            //这里使各滤镜的index刚好等于对应的position
            Log.d(TAG, "onFilterChanged: " + position + "," + pressed);
            if (pressed) {
                addFilter(getPressedFilter(position), position);
                mController.startDrawSectionBackground(position, colors[position]);
            } else {
                generateConfig(getPressedFilter(position));
                removeFilter(position);
                mController.stopDrawSectionBackground(position);
            }
        }
    };

    List<BaseFilter> mPreviewFilters = new ArrayList<>();

    private void generateConfig(BaseFilter filter) {
        filter.addRenderSectionPts((long) filter.getTag(), mYfCloudPlayer.getFrameTimestamp());
        if (!mPreviewFilters.contains(filter)) {
            mPreviewFilters.add(filter);
        }
        Log.d(TAG, "filters size:" + mPreviewFilters);
    }


    private boolean mLandscape;
    private int mCurrentFilterIndex = -1;
    private BaseFilter mCurrentFilter;
    private int sectionIndex;
    private final int BEAUTY_INDEX = 111, LOGO_INDEX = 222, FACE_INDEX = 333, GIF_INDEX = 444,
            CRAYON_INDEX = 0, SKETCH_INDEX = 1, WHITE_CAT_INDEX = 2, ANTIQUE_INDEX = 3, WAVE_INDEX = 4, SHAKE_INDEX = 5;
    private boolean setBeauty, setLogo, setAr, setCrayon, setSketch;

    private void addFilter(BaseFilter filter, int index) {
        filter.setIndex(index);
        filter.setTag(mYfCloudPlayer.getFrameTimestamp());//记录起始时间
        mYfGlSurfaceView.addFilter(filter);
        mCurrentFilterIndex = index;
        mCurrentFilter = filter;
    }

    private void addGif() {
        if (mGifFilter == null) {
            mGifFilter = createGifFilter(mYfGlSurfaceView.getMeasuredWidth());
        }
        mYfGlSurfaceView.addFilter(mGifFilter);
    }

    private void addLogo() {
        if (mWaterMarkFilter == null) {
            mWaterMarkFilter = createWaterMarkFilter(mYfGlSurfaceView.getMeasuredWidth());
        }
        mYfGlSurfaceView.addFilter(mWaterMarkFilter);
    }

    /**
     * 初始化预览界面用的、长按生效的滤镜
     *
     * @param filterIndex
     */
    private BaseFilter getPressedFilter(int filterIndex) {
        switch (filterIndex) {
            case ANTIQUE_INDEX:
                if (mYfAntiqueFilter == null) {
                    mYfAntiqueFilter = createAntiqueFilter(filterIndex);
                }
                return mYfAntiqueFilter;
            case CRAYON_INDEX:
                if (mYfCrayonFilter == null)
                    mYfCrayonFilter = createCrayonFilter(filterIndex);
                return mYfCrayonFilter;
            case SKETCH_INDEX:
                if (mYfSketchFilter == null)
                    mYfSketchFilter = createSketchFilter(filterIndex);
                return mYfSketchFilter;
            case WHITE_CAT_INDEX:
                if (mYfWhiteCatFilter == null)
                    mYfWhiteCatFilter = createWhiteCatFilter(filterIndex);
                return mYfWhiteCatFilter;
            case WAVE_INDEX:
                if (mYfWaveFilter == null)
                    mYfWaveFilter = createWaveFilter(filterIndex);
                return mYfWaveFilter;
            case SHAKE_INDEX:
                if (mYfShakeFilter == null)
                    mYfShakeFilter = createShakeFilter(filterIndex);
                return mYfShakeFilter;
        }
        return new BaseFilter();
    }

    /**
     * 初始化最后处理用的filter
     *
     * @param filterIndex
     * @return
     */
    private BaseFilter getConfigFilter(int filterIndex) {
        switch (filterIndex) {
            case ANTIQUE_INDEX:
                return createAntiqueFilter(filterIndex);
            case CRAYON_INDEX:
                return createCrayonFilter(filterIndex);
            case SKETCH_INDEX:
                return createSketchFilter(filterIndex);
            case WHITE_CAT_INDEX:
                return createWhiteCatFilter(filterIndex);
            case WAVE_INDEX:
                return createWaveFilter(filterIndex);
            case SHAKE_INDEX:
                return createShakeFilter(filterIndex);
        }
        return new BaseFilter();
    }


    private YfCrayonFilter createCrayonFilter(int filterIndex) {
        YfCrayonFilter temp = mYfFilterFactory.createCrayonFilter();
        temp.setIndex(filterIndex);
        temp.setRenderInSpecificPts(true);
        return temp;
    }

    private YfWhiteCatFilter createWhiteCatFilter(int filterIndex) {
        YfWhiteCatFilter temp = mYfFilterFactory.createWhiteCatFilter();
        temp.setIndex(filterIndex);
        temp.setRenderInSpecificPts(true);
        return temp;
    }

    private YfSketchFilter createSketchFilter(int filterIndex) {
        YfSketchFilter temp = mYfFilterFactory.createSketchFilter();
        temp.setIndex(filterIndex);
        temp.setRenderInSpecificPts(true);
        return temp;
    }

    private YfAntiqueFilter createAntiqueFilter(int filterIndex) {
        YfAntiqueFilter temp = mYfFilterFactory.createAntiqueFilter();
        temp.setIndex(filterIndex);
        temp.setRenderInSpecificPts(true);
        return temp;
    }

    private YfWaveFilter createWaveFilter(int filterIndex) {
        YfWaveFilter yfWaveFilter = mYfFilterFactory.createWaveFilter();
        yfWaveFilter.setIndex(filterIndex);
        yfWaveFilter.setAutoMotion(0.5f);
        yfWaveFilter.setRenderInSpecificPts(true);
        return yfWaveFilter;
    }

    private YfSoulDazzleFilter createShakeFilter(int filterIndex) {
        YfSoulDazzleFilter yfShakeFilter = mYfFilterFactory.createSoulDazzleFilter();
        yfShakeFilter.setIndex(filterIndex);
        yfShakeFilter.setAutoMotion(0.04f);
        yfShakeFilter.setRenderInSpecificPts(true);
        return yfShakeFilter;
    }

    private YfGifFilter createGifFilter(int displayWidth) {
        YfGifFilter gifFilter = mYfFilterFactory.createGifFilter();
        gifFilter.setIndex(GIF_INDEX);
        try {
            gifFilter.setGifSource(getAssets().open("TestGif.gif"));
            int percent = 30;
            int padding = displayWidth * 10 / 100;
            float logoWidth = 51, logoHeight = 52;
            int width = displayWidth * percent / 100;
            int height = (int) (width * logoHeight / logoWidth);
            gifFilter.setPosition(padding, padding, width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gifFilter;
    }

    private YfWaterMarkFilter createWaterMarkFilter(int displayWidth) {
        YfWaterMarkFilter waterMarkFilter = mYfFilterFactory.createWaterMarkFilter();
        waterMarkFilter.setIndex(LOGO_INDEX);
        waterMarkFilter.setWaterMark(BitmapFactory.decodeResource(getResources(), R.mipmap.logo));
        int percent = 50;
        int padding = 50;
        float logoWidth = 454, logoHeight = 160;
        int width = displayWidth * percent / 100;
        int height = (int) (width * logoHeight / logoWidth);
        waterMarkFilter.setPosition(displayWidth - width - padding, padding, width, height);
        return waterMarkFilter;
    }

    private void removeFilter(final int index) {
        mYfGlSurfaceView.removeFilter(index);
        mCurrentFilterIndex = -1;
        mCurrentFilter = null;
    }

    private void addAR(String itemName) {
        if (mFaceUnityFilter == null) {
            mFaceUnityFilter = new FaceUnityFilter(this);
            mFaceUnityFilter.setIndex(FACE_INDEX);
        }
        mFaceUnityFilter.setEffect(itemName, false);
        mYfGlSurfaceView.addFilter(mFaceUnityFilter);
        setAr = true;

    }

    private void openVideo(String path) {
        mCurrentPath = path;
        if (mYfCloudPlayer != null) {
            mYfCloudPlayer.release();
            mYfCloudPlayer = null;
        }
        mYfCloudPlayer = YfCloudPlayer.Factory.createPlayer(this, YfCloudPlayer.MODE_SOFT);
        mYfCloudPlayer.setOnPreparedListener(onPreparedListener);
        mYfCloudPlayer.setFrameCallback();
        mYfCloudPlayer.setOnNativeVideoDecodedListener(new YfCloudPlayer.OnNativeVideoDataDecoded() {
            @Override
            public void onVideoDataDecoded(YfCloudPlayer yfCloudPlayer, byte[] bytes, int i, int i1, long currentPts) {
//                Log.d(TAG,"onVideoDataDecoded:"+bytes.length+","+i+","+i1);

                if (mFaceUnityFilter != null)
                    mFaceUnityFilter.setCurrentFrameRGBA(bytes, i, i1);
                for (BaseFilter filter : mPreviewFilters) {
                    if (filter == mCurrentFilter || !filter.isRenderInSpecificPts())
                        return;
                    boolean inSection = false;
                    for (TimeSection timeSection : filter.getRenderSections()) {
                        if (currentPts >= timeSection.getStart() && currentPts <= timeSection.getEnd()) {
                            inSection = true;
                            break;
                        }
                    }
                    if (!filter.isAdded() && inSection) {
                        mYfGlSurfaceView.addFilter(filter);
                        Log.d(TAG, "in filter area:" + currentPts + "," + filter.getClass());
                        mCurrentFilterIndex = filter.getIndex();
                    } else if (filter.isAdded() && !inSection) {
                        Log.d(TAG, "not in filter area:" + currentPts + "," + filter.getClass());
                        mYfGlSurfaceView.removeFilter(filter.getIndex());
                    }
//
//                        if (!config.filter.isInitialized() && l >= config.startPts && l <= config.endPts) {
//                            Log.d(TAG, "in filter area:" + l + "," + config.startPts + "," + config.endPts);
//                            mYfGlSurfaceView.addFilter(config.filter);
//                            mCurrentFilterIndex = config.filter.getIndex();
//                        } else if (config.filter.isInitialized() && (l < config.startPts || l > config.endPts)) {
//                            Log.d(TAG, "not in filter area:" + l + "," + config.startPts + "," + config.endPts);
//                            mYfGlSurfaceView.removeFilter(config.filter.getIndex());
//                        }
                }

            }
        });
        mYfCloudPlayer.setOnCompletionListener(new YfCloudPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(YfCloudPlayer yfCloudPlayer) {
                mStartBtn.setVisibility(View.VISIBLE);
            }
        });
        mYfCloudPlayer.setSurface(mYfGlSurfaceView.getSurface());

        try {
            mYfCloudPlayer.setDataSource(path);
            mYfCloudPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean prepared, surfaceCreated;
    private YfCloudPlayer.OnPreparedListener onPreparedListener = new YfCloudPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(YfCloudPlayer yfCloudPlayer) {
            mDuration = yfCloudPlayer.getDuration();
            prepared = true;
            if (startPlayback) {
                startPause(mStartBtn);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause();
        mStartBtn.setVisibility(View.VISIBLE);
    }

    private void startPlayback() {
        startPlayback = true;
        if (surfaceCreated && prepared) {
            if (mYfCloudPlayer != null)
                mYfCloudPlayer.start();
        }
    }

    private void pause() {
        startPlayback = false;
        mYfCloudPlayer.pause();
    }

    private boolean startPlayback;

    public void startPause(View v) {
        if (mYfCloudPlayer == null)
            return;
        mPreview.setVisibility(View.GONE);
        if (mYfCloudPlayer.isPlaying()) {
            pause();
            v.setVisibility(View.VISIBLE);
        } else {
            startPlayback();
            mController.show(0);
            v.setVisibility(View.GONE);
        }
    }

    private YfGlSurfaceView.YfRenderCallback mRenderCallback = new YfGlSurfaceView.YfRenderCallback() {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            surfaceCreated = true;
            Log.d(TAG, "onSurfaceCreated~" + mYfCloudPlayer);
            if (mYfCloudPlayer == null) {
                openVideo(mCurrentPath);
            } else {
                mYfCloudPlayer.setSurface(mYfGlSurfaceView.getSurface());//从后台或其他界面恢复，重新设置display
                mYfCloudPlayer.start();
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d(TAG, "onSurfaceChanged~" + mYfCloudPlayer);

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }
    };

    @Override
    protected void onDestroy() {
        if (mYfCloudPlayer != null) {
            mYfCloudPlayer.stop();
            mYfCloudPlayer.release();
        }
        if(mYfEditor!=null){
            mYfEditor.release();
        }
        for (VideoEditInfo videoEditInfo : videoEditAdapter.getVideoEditInfoList()) {
            boolean b = Util.deleteFile(videoEditInfo.path);
        }
        Util.deleteFile(mFlashbackPath);
        super.onDestroy();
    }

    @SuppressLint("HandlerLeak")
    private class MainHandler extends Handler {
        private final WeakReference<FilterPlayerActivity> mActivity;

        MainHandler(FilterPlayerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            FilterPlayerActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case SHOW_IMG_SUCCESS:
                        if (activity.videoEditAdapter != null) {
                            VideoEditInfo info = (VideoEditInfo) msg.obj;
                            activity.videoEditAdapter.addItemVideoInfo(info);
                            if (activity.picCount == 0) {
                                Log.d(TAG, "load preview img：" + info.path);
                                Glide.with(FilterPlayerActivity.this)
                                        .load("file://" + info.path)
                                        .into(mPreview);

                            }
                            activity.picCount++;

                        }
                        break;
                    case UPLOAD_PROGRESS:
                        activity.mOutputBtn.setText("上传中..." + msg.arg1 + "%");
                        if (msg.arg1 == 100) {
                            activity.onDone();
                        }
                        break;
                }

            }
        }
    }


}
