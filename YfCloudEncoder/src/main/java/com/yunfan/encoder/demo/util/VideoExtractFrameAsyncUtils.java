package com.yunfan.encoder.demo.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Message;
import android.util.*;

import com.yunfan.encoder.demo.activity.FilterPlayerActivity;
import com.yunfan.encoder.demo.activity.SegmentRecordActivity;
import com.yunfan.encoder.demo.bean.VideoEditInfo;


public class VideoExtractFrameAsyncUtils {
    private static final String TAG = VideoExtractFrameAsyncUtils.class.getSimpleName();
    private Handler mHandler;
    private int extractW;
    private int extractH;

    public VideoExtractFrameAsyncUtils(int extractW, int extractH, Handler mHandler) {
        this.mHandler = mHandler;
        this.extractW = extractW;
        this.extractH = extractH;
    }

    public void getVideoThumbnailsInfoForEdit(String videoPath, String OutPutFileDirPath, long startPosition, long endPosition, int thumbnailsCount) {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(videoPath);
        long interval = (endPosition - startPosition) / (thumbnailsCount - 1);
        Log.d(TAG, "getVideoThumbnailsInfoForEdit: " + interval);
        for (int i = 0; i < thumbnailsCount; i++) {
            if (stop) {
                metadataRetriever.release();
                break;
            }
            long time = startPosition + interval * i;
            if (i == thumbnailsCount - 1) {
                if (interval > 1000) {
                    String path = extractFrame(metadataRetriever, endPosition - 800, OutPutFileDirPath);
                    sendAPic(path, endPosition - 800);
                } else {
                    String path = extractFrame(metadataRetriever, endPosition, OutPutFileDirPath);
                    sendAPic(path, endPosition);
                }
            } else {
                String path = extractFrame(metadataRetriever, time, OutPutFileDirPath);
                sendAPic(path, time);
            }
        }
        metadataRetriever.release();
    }

    /**
     * 成功一张add一张
     *
     * @param path path
     * @param time time
     */
    private void sendAPic(String path, long time) {
        VideoEditInfo info = new VideoEditInfo();
        info.path = path;
        info.time = time;
        Message msg = mHandler.obtainMessage(FilterPlayerActivity.SHOW_IMG_SUCCESS);
        msg.obj = info;
        mHandler.sendMessage(msg);
    }

    private String extractFrame(MediaMetadataRetriever metadataRetriever, long time, String OutPutFileDirPath) {
        Bitmap bitmap = metadataRetriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
        if (bitmap != null) {
            Bitmap bitmapNew = /*scaleImage(bitmap)*/bitmap;
            String path = PictureUtils.saveImageToSDForEdit(bitmapNew, OutPutFileDirPath, System.currentTimeMillis() + "_" + time + PictureUtils.POSTFIX);
            if (bitmapNew != null && !bitmapNew.isRecycled()) {
                bitmapNew.recycle();
                bitmapNew = null;
            }
            return path;
        }
        return null;
    }

    /**
     * 设置固定的宽度，高度随之变化，使图片不会变形
     *
     * @param bm Bitmap
     * @return Bitmap
     */
    private Bitmap scaleImage(Bitmap bm) {
        if (bm == null) {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = extractW * 1.0f / width;
//        float scaleHeight =extractH*1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        Bitmap newBm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        if (!bm.isRecycled()) {
            bm.recycle();
            bm = null;
        }
        return newBm;
    }


    private volatile boolean stop;

    public void stopExtract() {
        stop = true;
    }
}
