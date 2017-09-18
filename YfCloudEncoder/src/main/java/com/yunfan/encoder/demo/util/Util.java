package com.yunfan.encoder.demo.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;


public class Util {
    public static final String TAG = "Yf_Util";

    public static String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "."
                + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }
    public static  String getFormatTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM_dd_HH_mm_ss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }
    public static int getScreenWidth(Context context) {
        final Point p = new Point();
        final Display d = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        d.getSize(p);
        if (p.x > p.y) {
            return p.y;
        } else {
            return p.x;
        }
    }

    public static int getDisplayWidth(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    public static int dip2px(Context context, int dip) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((float) dip * scale + 0.5F);
    }

    public static int getScreenHeight(Context context) {
        final Point p = new Point();
        final Display d = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        d.getSize(p);
        if (p.x > p.y) {
            return p.x;
        } else {
            return p.y;
        }
    }

    public static boolean deleteFile(String path) {
        if (path != null) {
            File file = new File(path);
            boolean exists = file.exists();
            if (exists) {
                return file.delete();
            }
        }
        return false;
    }

    public static void CopyAssets(Context context, String oldPath, String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);// 获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {// 如果是目录
                File file = new File(newPath);
                file.mkdirs();// 如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    CopyAssets(context, oldPath + "/" + fileName, newPath + "/" + fileName);
                }
            } else {// 如果是文件
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                    // buffer字节
                    fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                }
                fos.flush();// 刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static boolean copyAssetsFileToSD(Context context, String path, String[] filenames,String[] savedNames) {
        android.util.Log.d(TAG, "copyAssetsFileToSD: " + path + Arrays.toString(filenames));
        long startTime = System.currentTimeMillis();
        File yunfanPath = new File(path);
        if (!yunfanPath.exists()) {
            yunfanPath.mkdirs();
        }
        for (int i=0;i<filenames.length;i++) {
            AssetManager am = context.getAssets();
            try {
                InputStream inputStream = am.open(filenames[i]);
                File file = new File(path, savedNames[i]);
                if(file.exists()){
                    continue;
                }
                FileOutputStream fos = new FileOutputStream(file);
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        long copyTime = System.currentTimeMillis() - startTime;
        android.util.Log.d(TAG, "copyAssetsFileToSD copyTime: " + copyTime);
        return true;
    }

    public static final String ping(String ip) {
        Log.d(TAG, "start to ping in thread:" + ip);
        String result = null;
        try {

//            String ip = "www.baidu.com";// 除非百度挂了，否则用这个应该没问题~

            Process p = Runtime.getRuntime().exec("ping -c 3 -w 100 " + ip);//ping3次
// 读取ping的内容，可不加。
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer stringBuffer = new StringBuffer();
            String content = "";
            while ((content = in.readLine()) != null) {
                stringBuffer.append(content);
            }
            Log.e(TAG, "result content : " + stringBuffer.toString());
// PING的状态
            int status = p.waitFor();
            if (status == 0) {
                result = "successful~";
                return result;
            } else {
                result = "failed~ cannot reach the IP address";
            }
        } catch (IOException e) {
            result = "failed~ IOException";
        } catch (InterruptedException e) {
            result = "failed~ InterruptedException";
        } finally {
            Log.e(TAG, "result = " + result);
        }
        return result;

    }

    static StringBuilder mFormatBuilder = new StringBuilder();
    static Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

    public static String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

}
