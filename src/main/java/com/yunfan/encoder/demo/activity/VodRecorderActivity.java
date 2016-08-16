/**
 * @版权 : 深圳云帆世纪科技有限公司
 * @作者 : 刘群山
 * @日期 : 2015年4月23日
 */
package com.yunfan.encoder.demo.activity;


import com.yunfan.encoder.widget.YfEncoderKit;

public class VodRecorderActivity extends LiveRecorderActivity {

    protected String getDefaultUrl() {
        return URL_VOD;
    }

    protected void startRecorder() {
        yfEncoderKit.changeMode(YfEncoderKit.MODE_VOD,HARD_ENCODER,VIDEO_BITRATE,VIDEO_WIDTH,VIDEO_HEIGHT);
        yfEncoderKit.setVodSaveName(mUrl.getText().toString());
        yfEncoderKit.startRecord();
    }

}
