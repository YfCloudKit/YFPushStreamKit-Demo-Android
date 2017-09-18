package com.yunfan.encoder.demo.bean;

/**
 * Created by yunfan on 2017/5/16.
 */

public class VideoSegment {
    private String mPath;
    private long mDuring;

    public VideoSegment(long during, String path) {
        super();
        this.mPath = path;
        this.mDuring = during;
    }


    public long getDuring() {
        return mDuring;
    }

    public VideoSegment setDuring(long during) {
        this.mDuring = during;
        return this;
    }

    public String getAbsolutePath() {
        return mPath;
    }

    public VideoSegment setPath(String path) {
        this.mPath = path;
        return this;
    }
}
