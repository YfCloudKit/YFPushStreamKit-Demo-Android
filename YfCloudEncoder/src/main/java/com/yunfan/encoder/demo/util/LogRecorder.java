package com.yunfan.encoder.demo.util;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xjx-pc on 2016/4/8 0008.
 */
public class LogRecorder {
    private List<String> logs = new ArrayList<>();
    private String[] tempString;
    private Context mContext;
    public LogRecorder(Context context){
        mContext=context;
    }
    public void writeLog(String content,boolean showToast) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
        String date = sDateFormat.format(new java.util.Date());
        logs.add(content + "\ntime:" + date);
        if(showToast){
            Toast.makeText(mContext,content,Toast.LENGTH_SHORT).show();
        }
    }

    public String[] getLogs() {
        tempString = new String[logs.size()];
        for (int i = 0; i < logs.size(); i++) {
            tempString[i] = logs.get(i);
        }
        return tempString;
    }

    public void clearLogs() {
        logs.clear();
    }
}
