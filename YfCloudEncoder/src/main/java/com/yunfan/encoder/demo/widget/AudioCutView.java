package com.yunfan.encoder.demo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.yunfan.encoder.demo.activity.SegmentRecordActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 音频选取View
 */

public class AudioCutView extends View {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 120;
    private static final String TAG = AudioCutView.class.getSimpleName();
    private int deltaX, deltaY;//点击位置和图形边界的偏移量
    private Paint mRectPaint = new Paint();
    private Paint mRectBackgroundPaint = new Paint();
    private Paint mTextPaint = new Paint();
    private Paint mTextTitlePaint = new Paint();
    private Rect mRect;
    /**
     * 音乐时长
     */
    private long mDuration;
    private int mRectWidth;
    private int mScreenWidth;
    private Rect mRectBackground;
    private SelectStartTimeListener mOnSelectStartTimeListener;
    private int mLastPosition;

    public AudioCutView(Context context) {
        this(context, null);
    }

    public AudioCutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;

        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#88666666"));
        setBackground(colorDrawable);
        mRectPaint.setColor(Color.GREEN);
        mRectPaint.setAntiAlias(true);
        mRectPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mRectPaint.setStyle(Paint.Style.FILL);

        mRectBackgroundPaint.setColor(Color.parseColor("#88222222"));
        mRectBackgroundPaint.setAntiAlias(true);
        mRectBackgroundPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mRectBackgroundPaint.setStyle(Paint.Style.FILL);

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextSize(32f);

        mTextTitlePaint.setColor(Color.WHITE);
        mTextTitlePaint.setAntiAlias(true);
        mTextTitlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextTitlePaint.setTextSize(36f);
    }

    /**
     * 设置总时长
     *
     * @param duration 音频总时长，单位毫秒
     */
    public void setAudioDuration(long duration) {
        mDuration = duration;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mRect == null) {
            //计算截取片段所占宽度
            mRectWidth = (int) (SegmentRecordActivity.MAX_RECORD_TIME / (float) mDuration
                    * getWidth());
            mRect = new Rect(0, 0, mRectWidth, 100);
            mRectBackground = new Rect(0, 0, mScreenWidth, 100);
        }
        canvas.drawRect(mRect, mRectPaint);
        canvas.drawRect(mRectBackground, mRectBackgroundPaint);
        long cutStartTime = (long) ((float) mRect.left / (float) getWidth() * mDuration);
        long cutEndTime = (long) ((float) mRect.right / (float) getWidth() * mDuration);
        String formatTimeStart = getFormatTime(cutStartTime);
        String formatTimeEnd = getFormatTime((long) (cutStartTime + SegmentRecordActivity.MAX_RECORD_TIME));
        canvas.drawText("start: " + formatTimeStart, 10, mRect.height() + 40, mTextPaint);
        canvas.drawText("end: " + formatTimeEnd, 10, mRect.height() + 100, mTextPaint);


        canvas.drawText("拖动绿色滑块选取音乐开始时间", mScreenWidth / 4, 220, mTextTitlePaint);
    }

    private String getFormatTime(long cutStartTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
        Date date = new Date(cutStartTime - 8 * 60 * 60 * 1000);
        return simpleDateFormat.format(date);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mRect.contains(x, y)) return false;//没有在矩形上点击，不处理触摸消息
                deltaX = x - mRect.left;
//                deltaY = y - mRect.top;
                break;
            case MotionEvent.ACTION_MOVE:
                Rect old = new Rect(mRect);
                //更新矩形的位置
                mRect.left = x - deltaX;
                if (mRect.left < 0) mRect.left = 0;//滑动到了最左边
                if (mRect.left > getWidth() - mRectWidth) mRect.left = getWidth() - mRectWidth;
//                mRect.top = y - deltaY;
                mRect.right = mRect.left + mRectWidth;

//                mRect.bottom = mRect.top + HEIGHT;
                old.union(mRect);//要刷新的区域，求新矩形区域与旧矩形区域的并集
                invalidate(old);//出于效率考虑，设定脏区域，只进行局部刷新，不是刷新整个view
                break;
            case MotionEvent.ACTION_UP:
                if (mOnSelectStartTimeListener != null &&
                        (mRect.contains(x, y) || mLastPosition != mRect.left)) {
                    long starTime = (long) ((float) mRect.left / (float) mScreenWidth * mDuration);
                    mOnSelectStartTimeListener.onSelectStartTime(starTime);
                }
                mLastPosition = mRect.left;
                break;
            default:
                break;

        }
        return true;
    }

    public void setOnSelectStartTimeListener(SelectStartTimeListener onSelectStartTimeListener) {
        mOnSelectStartTimeListener = onSelectStartTimeListener;
    }

    public interface SelectStartTimeListener {
        void onSelectStartTime(long startTime);
    }
}
