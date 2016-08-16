package com.yunfan.encoder.demo.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xjx-pc on 2016/4/8 0008.
 */
public class LogRecoder {
    private List<String> logs = new ArrayList<>();
    String[] tempString;
    public void writeLog(String content) {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss");
        String date = sDateFormat.format(new java.util.Date());
        logs.add(content + "\ntime:" + date);
    }

    public List<String> getLogsWithList() {
        return logs;
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

    public void saveToLocal() {

    }
}
