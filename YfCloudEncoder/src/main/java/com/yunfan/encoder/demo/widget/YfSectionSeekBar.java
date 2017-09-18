package com.yunfan.encoder.demo.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.util.SparseIntArray;

import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.util.Log;

import java.util.LinkedList;

/**
 * Created by 37917 on 2017/7/10 0010.
 */

public class YfSectionSeekBar extends AppCompatSeekBar {
    private static final String TAG = "YfSectionSeekBar";

    private static final int COLOR = 100;
    private static final int START_POSITION = 101;
    private static final int END_POSITION = 102;
    private static final int NOT_END = -10086;
    private LinkedList<SparseIntArray> mSectionBackground = new LinkedList<>();

    public YfSectionSeekBar(Context context) {
        super(context);
    }

    public YfSectionSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public YfSectionSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    private int[] colors = new int[]{R.color.blue, R.color.green, R.color.red, R.color.black, R.color.white};
//    private int colorIndex = -1;

    public void startDrawBackground(int sign,int color) {
        SparseIntArray array = new SparseIntArray();
//        colorIndex++;
//        if (colorIndex == colors.length) {
//            colorIndex = 0;
//        }
        array.put(COLOR,color);
        array.put(START_POSITION, getProgress());
        array.put(END_POSITION, NOT_END);
        mSectionBackground.add(array);
        Log.d(TAG,"startDrawSectionBackground:"+getProgress()+","+mSectionBackground.size());
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
        }
    }

    public void stopDrawBackground(int sign) {
        mSectionBackground.getLast().put(END_POSITION, getProgress());
        Log.d(TAG,"stopDrawSectionBackground:"+ getProgress()+","+mSectionBackground.size());
    }

    private Paint mPaint;
    private int sectionEndProgress, sectionStartPosition, sectionEndPosition;

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (SparseIntArray array : mSectionBackground) {
            mPaint.setColor(getResources().getColor(array.get(COLOR)));
            sectionEndProgress = array.get(END_POSITION) == NOT_END ? getProgress() : array.get(END_POSITION);
            canvas.drawRect((getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) / (float) getMax() * array.get(START_POSITION) + getPaddingRight(), getMeasuredHeight()/4, (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) / (float) getMax() * sectionEndProgress + getPaddingRight(), getMeasuredHeight()*3/4, mPaint);
        }
    }

    public void clear() {
        mSectionBackground.clear();
    }
}
