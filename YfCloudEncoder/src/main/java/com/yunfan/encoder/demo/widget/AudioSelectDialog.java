package com.yunfan.encoder.demo.widget;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.yfcloudencoder.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yunfan on 2017/5/15.
 */

public class AudioSelectDialog extends AlertDialog {

    private Context mContext;
    private List<String> mAudioPaths = new ArrayList<>();
    private List<String> mAudioNames = new ArrayList<>();
    private List<String> mVideoPaths = new ArrayList<>();
    private List<String> mVideoNames = new ArrayList<>();
    private ItemClickListener mItemClickListener;

    public AudioSelectDialog(@NonNull Context context) {
        super(context);
        mContext = context;
        initAudioPathsAndNames();
    }

    public AudioSelectDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        mContext = context;
        initAudioPathsAndNames();
    }

    protected AudioSelectDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        mContext = context;
        initAudioPathsAndNames();
    }

    private void initAudioPathsAndNames() {
//        ContentResolver contentResolver = mContext.getContentResolver();
//        //音频文件
//        String[] projectionAudio = new String[]{MediaStore.Audio.Media.DATA};
//        Cursor cursorAudio = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projectionAudio,
//                null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
//        int fileNumAudio;
//        if (cursorAudio != null) {
//            cursorAudio.moveToFirst();
//            fileNumAudio = cursorAudio.getCount();
//            for (int counter = 0; counter < fileNumAudio; counter++) {
//                mAudioPaths.add(cursorAudio.getString(cursorAudio.getColumnIndex(MediaStore.Audio.Media.DATA)));
//                cursorAudio.moveToNext();
//            }
//            cursorAudio.close();
//        }
        File dir = new File("/sdcard/yunfanencoder/audio");
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }
        File files[] = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                mAudioPaths.add(file.getAbsolutePath());
            }
        }
        //----------------------
        String audioName;
        for (int i = 0; i < mAudioPaths.size(); i++) {
            audioName = mAudioPaths.get(i).substring(mAudioPaths.get(i).lastIndexOf("/") + 1);
            mAudioNames.add(audioName);
        }


//        //视频文件
//        String[] projectionVideo = new String[]{MediaStore.Video.Media.DATA};
//        Cursor cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projectionVideo,
//                null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
//        int fileNum;
//        if (cursor != null) {
//            cursor.moveToFirst();
//            fileNum = cursor.getCount();
//            for (int counter = 0; counter < fileNum; counter++) {
//                mVideoPaths.add(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
//                cursor.moveToNext();
//            }
//            cursor.close();
//        }
//        //----------------------
//        String videoName;
//        for (int i = 0; i < mVideoPaths.size(); i++) {
//            videoName = mVideoPaths.get(i).substring(mVideoPaths.get(i).lastIndexOf("/") + 1);
//            mVideoNames.add(videoName);
//        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_audio_select);
        ListView listViewAudio = (ListView) findViewById(R.id.list_view_audio);
        ListView listViewVideo = (ListView) findViewById(R.id.list_view_video);
        MediaListAdapter audioListAdapter = new MediaListAdapter(mAudioNames, mContext, 0);
        listViewAudio.setAdapter(audioListAdapter);

//        MediaListAdapter videoListAdapter = new MediaListAdapter(mVideoNames, mContext, 1);
//        listViewVideo.setAdapter(videoListAdapter);

    }

    public void setOnItemClickListener(ItemClickListener itemClickListener) {
        mItemClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClickListener(String path);
    }


    private class MediaListAdapter extends BaseAdapter {
        private List<String> mItemList;
        private Context context;
        private int mType;

        MediaListAdapter(List<String> itemList, Context context, int type) {
            this.mItemList = itemList;
            this.context = context;
            this.mType = type;
        }

        @Override
        public int getCount() {
            return mItemList.size();
        }

        @Override
        public String getItem(int i) {
            return mItemList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        @SuppressWarnings("deprecation")
        public View getView(final int position, View view, ViewGroup viewGroup) {
            final ViewHolder holder;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.dialog_file_list_item, viewGroup, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            final String item = mItemList.get(position);
            holder.mNameTextView.setText(item);
            holder.mNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null)
                        mItemClickListener.onItemClickListener(
                                mType == 0 ? mAudioPaths.get(position) : mVideoPaths.get(position));
                }
            });
            return view;
        }

        private class ViewHolder {
            TextView mNameTextView;

            ViewHolder(View itemView) {
                mNameTextView = (TextView) itemView.findViewById(R.id.tv_audio_name);
            }
        }
    }


}
