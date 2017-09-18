package com.yunfan.encoder.demo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


import java.util.LinkedList;


public class VideoProgressView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final String TAG = "Yf_VideoProgress";
    private float mTotalTime;
    private int mTotalProgress;
    private float mSpeed = 1f;
    private int mCurrentRecordTime;

    private float DEFAULT_RECORD_TIME = 15 * 1000f;
    private float mRight;

    public VideoProgressView(Context context) {
        super(context);
        init(context);
    }

    public VideoProgressView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init(paramContext);

    }

    public VideoProgressView(Context paramContext, AttributeSet paramAttributeSet, int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init(paramContext);
    }

    private void init(Context paramContext) {
        this.setZOrderOnTop(true);
        this.setZOrderMediaOverlay(true);
        displayMetrics = getResources().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;


        progressPaint = new Paint();
        flashPaint = new Paint();
        minTimePaint = new Paint();
        breakPaint = new Paint();
        rollbackPaint = new Paint();
        backgroundPaint = new Paint();

        //setBackgroundColor(Color.parseColor("#222222"));
        //setBackgroundColor(Color.parseColor("#4db288"));
        //
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(Color.parseColor("#222222"));

        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(Color.parseColor("#4db288"));

        flashPaint.setStyle(Paint.Style.FILL);
        flashPaint.setColor(Color.parseColor("#FFFF00"));

//		minTimePaint.setStyle(Paint.Style.FILL);
//		minTimePaint.setColor(Color.parseColor("#ff0000"));

        breakPaint.setStyle(Paint.Style.FILL);
        breakPaint.setColor(Color.parseColor("#000000"));

        rollbackPaint.setStyle(Paint.Style.FILL);
        //        rollbackPaint.setColor(Color.parseColor("#FF3030"));
        rollbackPaint.setColor(Color.parseColor("#f15369"));

        holder = getHolder();
        holder.addCallback(this);
    }

    public void setMaxDuration(float durationMs){
        DEFAULT_RECORD_TIME=durationMs;
        perWidth = screenWidth / DEFAULT_RECORD_TIME;
    }
    public void updateProgress(int currentRecordTime) {
        if (currentRecordTime != 0)
            mCurrentRecordTime = currentRecordTime;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        drawing = visibility == VISIBLE;
    }


    /**
     * 根据录制时间戳绘制
     */
    private void myDrawByTime() {
        canvas = holder.lockCanvas();
        progressHeight = getMeasuredHeight();

        if (canvas != null) {
            canvas.drawRect(0, 0, screenWidth, progressHeight, backgroundPaint);
        }

        long curSystemTime = System.currentTimeMillis();
        countWidth = 0;
        if (!timeList.isEmpty()) {
            long preTime = 0;
            long curTime = 0;
            for (Integer aTimeList : timeList) {
//                Log.d(TAG,"aTimeList: " + aTimeList);
                lastStartTime = preTime;
                curTime = aTimeList;
                lastEndTime = curTime;
                float left = countWidth;
                countWidth += (curTime - preTime) * perWidth;
                //绘制breakPaint的位置时候需要减去breakPaint的宽度
                countWidth -= breakWidth;
                if (canvas != null) {
                    canvas.drawRect(left, 0, countWidth, progressHeight, progressPaint);
                    canvas.drawRect(countWidth, 0, countWidth + breakWidth, progressHeight, breakPaint);
                }
                countWidth += breakWidth;
                preTime = curTime;
            }
        }
//		if (timeList.isEmpty() || (!timeList.isEmpty() && timeList.getLast() <= SegmentRecordActivity.MIN_RECORD_TIME)) {
//			float left = perWidth * SegmentRecordActivity.MIN_RECORD_TIME;
//			if (canvas != null) {
//				canvas.drawRect(left, 0, left + minTimeWidth, progressHeight, minTimePaint);
//			}
//		}
//        if (currentState == State.BACKSPACE) {
//            float left = countWidth - (lastEndTime - lastStartTime) * perWidth;
//            float right = countWidth;
//            if (canvas != null) {
//                canvas.drawRect(left, 0, right, progressHeight, rollbackPaint);
//            }
//        }
        // 手指按下时，绘制新进度条，
        if (currentState == State.START) {
//            perProgress += perWidth * mSpeed * (curSystemTime - initTime);
//            float right = (countWidth + perProgress) >= screenWidth ? screenWidth : (countWidth + perProgress);

            //根据录制时间戳绘制进度
            mRight = mCurrentRecordTime / DEFAULT_RECORD_TIME * screenWidth >= screenWidth ?
                    screenWidth : mCurrentRecordTime / DEFAULT_RECORD_TIME * screenWidth;
            //去掉breakWidth所占宽度
//            if (!timeList.isEmpty()) mRight -= timeList.size() * breakWidth;
            //绘制的进度
            int progress = (int) (mRight * 100 / screenWidth);
            if (mTotalProgress <= 100) {
                mHandler.obtainMessage(0, progress, 0).sendToTarget();
            }
            if (canvas != null) {
                canvas.drawRect(countWidth, 0, mRight, progressHeight, progressPaint);
            }
        }
        //flash光标闪烁间隔
        if (drawFlashTime == 0 || curSystemTime - drawFlashTime >= 500) {
            isVisible = !isVisible;
            drawFlashTime = System.currentTimeMillis();
        }
        if (isVisible) {
            if (currentState == State.START) {
                if (canvas != null) {
                    canvas.drawRect(mRight, 0, mRight + flashWidth, progressHeight,
                            flashPaint);
                }
            } else {
                if (canvas != null) {
                    canvas.drawRect(countWidth, 0, countWidth + flashWidth, progressHeight, flashPaint);
                }
            }
        }
        initTime = System.currentTimeMillis();
        if (canvas != null) {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private volatile State currentState = State.PAUSE;

    private boolean isVisible = true;
    private float countWidth = 0;
    private float perProgress = 0;
    private long initTime;
    private long drawFlashTime = 0;

    private long lastStartTime = 0;
    private long lastEndTime = 0;

    private volatile boolean drawing = false;

    private DisplayMetrics displayMetrics;
    private int screenWidth, progressHeight;
    private Paint backgroundPaint, progressPaint, flashPaint, minTimePaint, breakPaint, rollbackPaint;
    /**
     * 每秒绘制宽度
     */
    private float perWidth;
    private float flashWidth = 3f;
    private float minTimeWidth = 5f;
    private float breakWidth = 2f;

    private LinkedList<Integer> timeList = new LinkedList<>();

    private Canvas canvas = null; //定义画布
    private SurfaceHolder holder = null;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (mRecordProgressListener != null) {
//                        Log.d(TAG, "msg.arg1:" + msg.arg1);
                        if (msg.arg1 < 100) {
                            mRecordProgressListener.onProgress(msg.arg1);
                        } else if (msg.arg1 == 100 && mTotalProgress != 100) {
                            mRecordProgressListener.onProgress(msg.arg1);
                            mTotalProgress = msg.arg1;
                        }
                    }
                    break;
            }
            return false;
        }
    });

    private void myDraw() {
        canvas = holder.lockCanvas();
        progressHeight = getMeasuredHeight();

        if (canvas != null) {
            canvas.drawRect(0, 0, screenWidth, progressHeight, backgroundPaint);
        }

        long curSystemTime = System.currentTimeMillis();
        countWidth = 0;
        if (!timeList.isEmpty()) {
            long preTime = 0;
            long curTime = 0;
            for (Integer aTimeList : timeList) {
//                Log.d(TAG,"aTimeList: " + aTimeList);
                lastStartTime = preTime;
                curTime = aTimeList;
                lastEndTime = curTime;
                float left = countWidth;
                countWidth += (curTime - preTime) * perWidth;
                if (canvas != null) {
                    canvas.drawRect(left, 0, countWidth, progressHeight, progressPaint);
                    canvas.drawRect(countWidth, 0, countWidth + breakWidth, progressHeight, breakPaint);
                }
                countWidth += breakWidth;
                preTime = curTime;
            }
        }
//		if (timeList.isEmpty() || (!timeList.isEmpty() && timeList.getLast() <= SegmentRecordActivity.MIN_RECORD_TIME)) {
//			float left = perWidth * SegmentRecordActivity.MIN_RECORD_TIME;
//			if (canvas != null) {
//				canvas.drawRect(left, 0, left + minTimeWidth, progressHeight, minTimePaint);
//			}
//		}
//        if (currentState == State.BACKSPACE) {
//            float left = countWidth - (lastEndTime - lastStartTime) * perWidth;
//            float right = countWidth;
//            if (canvas != null) {
//                canvas.drawRect(left, 0, right, progressHeight, rollbackPaint);
//            }
//        }
        // 手指按下时，绘制新进度条，
        if (currentState == State.START) {
            perProgress += perWidth * mSpeed * (curSystemTime - initTime);
            float right = (countWidth + perProgress) >= screenWidth ? screenWidth : (countWidth + perProgress);
            //绘制的进度
            int progress = (int) (right * 100 / screenWidth);


            if (mTotalProgress <= 100) {
                mHandler.obtainMessage(0, progress, 0).sendToTarget();
            }
            if (canvas != null) {
                canvas.drawRect(countWidth, 0, right, progressHeight, progressPaint);
            }
        }
        //flash光标闪烁间隔
        if (drawFlashTime == 0 || curSystemTime - drawFlashTime >= 500) {
            isVisible = !isVisible;
            drawFlashTime = System.currentTimeMillis();
        }
        if (isVisible) {
            if (currentState == State.START) {
                if (canvas != null) {
                    canvas.drawRect(countWidth + perProgress, 0, countWidth + flashWidth + perProgress, progressHeight,
                            flashPaint);
                }
            } else {
                if (canvas != null) {
                    canvas.drawRect(countWidth, 0, countWidth + flashWidth, progressHeight, flashPaint);
                }
            }
        }
        initTime = System.currentTimeMillis();
        if (canvas != null) {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void run() {
        while (drawing) {
            try {
//                myDraw();
                myDrawByTime();
                Thread.sleep(40);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setOnProgressListener(RecordProgressListener onProgressListener) {
        mRecordProgressListener = onProgressListener;
    }

    public RecordProgressListener mRecordProgressListener;

    public void setSpeed(float speed) {
        mSpeed = speed;
    }


    public interface RecordProgressListener {
        void onProgress(int progress);
    }

    public void putTimeList(int time) {
        Log.d(TAG, "putTimeList: " + time);
        timeList.add(time);
    }

    public void clearTimeList() {
        timeList.clear();
    }

    public int getLastTime() {
        if ((timeList != null) && (!timeList.isEmpty())) {
            return timeList.getLast();
        }
        return 0;
    }

    public boolean isTimeListEmpty() {
        return timeList.isEmpty();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Thread thread = new Thread(this);
        drawing = true;
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawing = false;
    }

    public void setCurrentState(State state) {
        currentState = state;
//        if (state != State.START) {//
//            perProgress = perWidth ;
//        }
        if (state == State.PAUSE) {//
            perProgress = perWidth * mSpeed;
        }
        if (state == State.DELETE) {
            if ((timeList != null) && (!timeList.isEmpty())) {
                //回退之前的进度
                if (timeList.size() == 1) {
                    mCurrentRecordTime = 0;
                } else if (timeList.size() > 1) {
                    mCurrentRecordTime = timeList.get(timeList.size() - 2);
                }

                Integer integer = timeList.removeLast();
                Log.d(TAG, "timeList size " + timeList.size() + " lastTime: " + integer);
                Log.d(TAG, "mCurrentRecordTime: " + mCurrentRecordTime);
                mTotalProgress = -1;
            }
        }
    }


    public enum State {

        START(0x1), PAUSE(0x2),/* BACKSPACE(0x3),*/ DELETE(0x4);

        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return PAUSE;
        }

        private int mIntValue;

        State(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
    }
}