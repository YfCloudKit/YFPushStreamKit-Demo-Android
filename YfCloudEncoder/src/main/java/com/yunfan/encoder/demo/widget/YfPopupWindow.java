package com.yunfan.encoder.demo.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.adapter.ItemSelectAdapter;

public class YfPopupWindow extends PopupWindow {

    private final Context mContext;
    private final String[] mItemNames;
    private final boolean mSingleSelection;

    public YfPopupWindow(Context context, String[] itemNames, boolean landscape, boolean singleSelection) {
        super(context);
        mContext = context;
        mSingleSelection = singleSelection;
        mItemNames = itemNames;
        // 设置可以获得焦点
        this.setFocusable(true);
        // 设置弹窗内可点击
        setTouchable(true);
        // 设置弹窗外可点击
        setOutsideTouchable(true);
        // 设置弹窗的宽度和高度
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        setWidth(displayMetrics.widthPixels);

        setHeight(landscape ? displayMetrics.heightPixels / 6 : displayMetrics.heightPixels / 4);

        //this.setBackgroundDrawable(new ColorDrawable(mContext.getResources().getColor(R.color.half_transparent)));
        // 设置弹窗的布局界面
        View view = LayoutInflater.from(context).inflate(R.layout.popup_faceu_item, null);
        setContentView(view);
        initView(view);

    }
    ItemSelectAdapter mAdapter;
    private void initView(View view) {
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_faceu);
        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(mContext);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new ItemSelectAdapter(mContext, mItemNames, mSingleSelection);
        recyclerView.setAdapter(mAdapter);
    }

    public void setOnSelectedListener(OnSelectedListener listener){
        mAdapter.setOnSelectedListener(listener);
    }

    public void setOnPressedListener(OnPressedListener listener){
        mAdapter.setOnPressedListener(listener);
    }
    public interface OnSelectedListener {
        void onSelected(int position);
    }
    public interface OnPressedListener {
        void onPressed(int position,boolean pressed);
    }
}