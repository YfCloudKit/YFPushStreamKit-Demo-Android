package com.yunfan.encoder.demo.http;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by xjx on 2016/6/2.
 * 带标记的Callback
 */
public class YfOKHttpCallBack implements Callback {
    private YfCallback myCallBack;
    private int index;
    private String content;
    private boolean tag1;
    private Object tag2;

    public YfOKHttpCallBack(int index,YfCallback myCallBack) {
        this.index = index;
        this.myCallBack = myCallBack;
    }

    public YfOKHttpCallBack(int index, String content, boolean tag1, Object tag2, YfCallback myCallBack) {
        this.tag1 = tag1;
        this.tag2 = tag2;
        this.index = index;
        this.content = content;
        this.myCallBack = myCallBack;
    }

    public interface YfCallback {
        void onFailure(Call call, IOException e, int index, String content, boolean tag1, Object tag2);

        void onResponse(Call call, Response response, int index, String content, boolean tag1, Object tag2) throws IOException;
    }


    @Override
    public void onFailure(Call call, IOException e) {
        myCallBack.onFailure(call, e, index, content, tag1, tag2);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        myCallBack.onResponse(call, response, index, content, tag1, tag2);
    }
}
