package com.yunfan.encoder.demo.http;


import com.yunfan.encoder.utils.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.Callback;

/**
 * Created by xjx on 2016/6/1.
 */
public class Server {
    private static final String TAG = "Server";
    public static final String UPLOAD_USER = "yunfanlive";
    public static final String UPLOAD_TOKEN = "413adcb0059f071ba03d967ac158fbae";
    public static final String SAVED_DIR = "yflive/2017/";
//    private final static String UPLOAD_FILE_URL = "http://upload.fileinject.yunfancdn.com/file?user=UPLOAD_USER&token=UPLOAD_TOKEN&key=UPLOAD_KEY";
    private final static String UPLOAD_FILE_URL = "http://upload.fileinject.yunfancdn.com/file/create?user=UPLOAD_USER&token=UPLOAD_TOKEN&key=UPLOAD_KEY";



    /**
     * 上传文件
     *
     * @param f        目标文件
     * @param savedName 在服务器保存的路径
     * @param callback 服务器连接回调
     * @param listener 进度回调
     */
    public static void POST_UPLOAD_FILE(File f, String savedName,Callback callback, OkHttpHelper.ProgressListener listener) {
        final String requestUrl = UPLOAD_FILE_URL.replace("UPLOAD_USER",UPLOAD_USER).replace("UPLOAD_TOKEN",UPLOAD_TOKEN).replace("UPLOAD_KEY",SAVED_DIR+savedName);
        Log.d(TAG, "final url:" + requestUrl);
        OkHttpHelper.POSTFile(requestUrl, f, callback, listener);
    }


}
