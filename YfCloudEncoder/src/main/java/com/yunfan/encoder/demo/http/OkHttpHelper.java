package com.yunfan.encoder.demo.http;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by xjx on 2016/5/31.
 */
public class OkHttpHelper {
    private final static OkHttpClient mOkHttpClient = new OkHttpClient();
    public static void GETRequest(String url, Callback callback) {
        //创建一个Request
        final Request request = new Request.Builder()
                .url(url)
//                .header("contentType","application/x-www-form-urlencoded")
                .build();
        enqueueCall(request, callback);
    }

    public static void GETRequest(String url, String accessToken, Callback callback) {
        //创建一个Request
        final Request request = new Request.Builder()
                .url(url)
//                .header("contentType","application/x-www-form-urlencoded")
                .addHeader("AccessToken", accessToken)
                .build();
        enqueueCall(request, callback);
    }

    public static void POSTRequest(String url, String bodyJson, String accessToken, Callback callback) {
        RequestBody body = RequestBody.create(null, bodyJson);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("contentType", "application/x-www-form-urlencoded")
                .addHeader("AccessToken", accessToken)
                .post(body)
                .build();
        enqueueCall(request, callback);
    }


    public static void POSTFile(String url, final File file, Callback callback, final ProgressListener listener) {
        RequestBody body = new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.parse("application/octet-stream");
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source;
                try {
                    source = Okio.source(file);
                    Buffer buf = new Buffer();
                    Long remaining = contentLength();
                    for (long readCount; (readCount = source.read(buf, 2048)) != -1; ) {
                        sink.write(buf, readCount);
                        listener.onProgress(contentLength(), remaining -= readCount, remaining == 0);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        enqueueCall(request, callback);
    }


    public interface ProgressListener{
        void onProgress(long totalBytes, long remainingBytes, boolean done);
    }

    private static void enqueueCall(Request request, Callback callback) {
        //new call
        Call call = mOkHttpClient.newCall(request);
        //请求加入调度
        call.enqueue(callback);
    }


}
