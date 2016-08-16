/**
 * @版权 : 深圳云帆世纪科技有限公司
 * @作者 : 刘群山
 * @日期 : 2015年4月20日
 */
package com.yunfan.encoder.demo.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class DeviceUtil {

    private static Boolean sArmV7 = null;
    private static Boolean sNeon = null;
    private static Boolean sX86 = null;

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

    public static String getRecorderSoName() {
        if (sNeon == null) {
            initCpuAbi();
        }

        if (sNeon) {
            return "muxer-7neon";
        } else if (sArmV7) {
            return "muxer-7";
        } else if (sX86) {
            throw new RuntimeException("Not support x86 platform");
        } else {
            return "muxer-6";
        }
    }

    private static void initCpuAbi() {
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(
                    "getprop ro.product.cpu.abi");
            input = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));

            String abi = input.readLine();

            if (abi.equalsIgnoreCase("armeabi-v7a")) {
                sX86 = false;
                sArmV7 = true;
                sNeon = containFeature("neon");
            } else if (abi.contains("arm")) {
                sX86 = false;
                sArmV7 = false;
                sNeon = false;
            } else {
                sX86 = true;
                sArmV7 = false;
                sNeon = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	if (input != null) {
                try {
                	input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean containFeature(String feature) {
        try {
            String cpuInfo = getProperty("/proc/cpuinfo", "Features");
            if (cpuInfo != null && cpuInfo.contains(feature)) {
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    private static String getProperty(String file, String key) {
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(file));

            return p.getProperty(key);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
