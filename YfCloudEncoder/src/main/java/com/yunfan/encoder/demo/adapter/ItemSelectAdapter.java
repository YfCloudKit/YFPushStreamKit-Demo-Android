package com.yunfan.encoder.demo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.util.Log;
import com.yunfan.encoder.demo.widget.YfPopupWindow;

public class ItemSelectAdapter extends RecyclerView.Adapter<ItemSelectAdapter.SelectViewHolder> {
    private final String TAG = "ItemSelectAdapter";
    Context mContext;
    private String[] mItemNames;
    private boolean mSingleSelection;

    public ItemSelectAdapter(Context context, String[] itemNames, boolean singleSelection) {
        mContext = context;
        mItemNames = itemNames;
        mSingleSelection = singleSelection;
    }


    @Override
    public ItemSelectAdapter.SelectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflate = LayoutInflater.from(mContext).inflate(R.layout.item_select, null);
        SelectViewHolder viewHolder = new SelectViewHolder(inflate);
        viewHolder.mButton = (Button) inflate.findViewById(R.id.btn_faceu_item);
        viewHolder.mNames = (TextView) inflate.findViewById(R.id.txt);
        return viewHolder;
    }


    @Override
    public void onBindViewHolder(final ItemSelectAdapter.SelectViewHolder holder, final int position) {
        holder.mButton.setText(String.valueOf(position));
        holder.mNames.setText(mItemNames[position]);
        if (mSelectedListener != null){
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSingleSelection) {
                        holder.mButton.setSelected(!holder.mButton.isSelected());
//                     holder.mButton.setBackground(mContext.getResources().getDrawable(R.drawable.select));
                        int lastSelected = mCurrentSelectPosition;
                        if (lastSelected != position)
                            notifyItemChanged(lastSelected);
                        Log.d(TAG, "position: " + mCurrentSelectPosition + "," + holder.mButton.isSelected());
                        mCurrentSelectPosition = position;
//                    notifyItemChanged(position);
                    } else {
                        holder.mButton.setSelected(!holder.mButton.isSelected());
                    }

                    if (mSelectedListener != null) {
                        mSelectedListener.onSelected(position);
                    }

                }
            });


            Log.d(TAG, "on bind view holder:" + position + "," + mCurrentSelectPosition + "," + holder.mButton.isSelected());
            if (mSingleSelection)
                holder.mButton.setSelected(position == mCurrentSelectPosition ? holder.mButton.isSelected() : false);
        }
        if(mOnPressedListener!=null){
            holder.mButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            mOnPressedListener.onPressed(position,true);
                            holder.mButton.setSelected(true);
                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            mOnPressedListener.onPressed(position,false);
                            holder.mButton.setSelected(false);
                            break;

                    }
                    return false;
                }
            });
        }
    }
    public void setOnSelectedListener(YfPopupWindow.OnSelectedListener listener){
        mSelectedListener=listener;
    }

    public void setOnPressedListener(YfPopupWindow.OnPressedListener listener){
        mOnPressedListener=listener;
    }
    @Override
    public int getItemCount() {
        return mItemNames.length;
    }

    private int mCurrentSelectPosition = 0;

    class SelectViewHolder extends RecyclerView.ViewHolder {
        Button mButton;
        TextView mNames;

        SelectViewHolder(View itemView) {
            super(itemView);
        }
    }

    YfPopupWindow.OnSelectedListener mSelectedListener;
    YfPopupWindow.OnPressedListener mOnPressedListener;


}