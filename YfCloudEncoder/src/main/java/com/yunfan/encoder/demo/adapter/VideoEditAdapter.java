package com.yunfan.encoder.demo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.example.yfcloudencoder.R;
import com.yunfan.encoder.demo.bean.VideoEditInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoEditAdapter extends RecyclerView.Adapter {
    private List<VideoEditInfo> lists = new ArrayList<>();
    private LayoutInflater inflater;

    private int itemW;
    private Context context;

    public VideoEditAdapter(Context context, int itemW) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.itemW = itemW;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new EditViewHolder(inflater.inflate(R.layout.video_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        EditViewHolder viewHolder = (EditViewHolder) holder;
        Glide.with(context)
                .load("file://" + lists.get(position).path)
                .into(viewHolder.img);
    }

    public   List<VideoEditInfo> getVideoEditInfoList(){
        return lists;
    }
    @Override
    public int getItemCount() {
        return lists.size();
    }

    private final class EditViewHolder extends RecyclerView.ViewHolder {
        public ImageView img;

        EditViewHolder(View itemView) {
            super(itemView);
            img = (ImageView) itemView.findViewById(R.id.id_image);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) img.getLayoutParams();
            layoutParams.width = itemW;
            img.setLayoutParams(layoutParams);
        }
    }

    public void addItemVideoInfo(VideoEditInfo info) {
        lists.add(info);
        notifyItemInserted(lists.size());
    }

    public void reverseItem(){
        Collections.reverse(lists);
        notifyDataSetChanged();
    }
}
