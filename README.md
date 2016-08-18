# 云帆Android直播推流SDK 

### 简介

---



YfEncoderKit SDK是云帆加速推出的Android平台用于推流的软件开发工具包（SDK)，为您提供简单、便捷的开发接口，助您在基于 4.1 及以上版本的移动设备上实现直播推流功能。

- [YfEncoderKit SDK下载地址](https://github.com/yfcloudStreamEngine/YfEncoderKit_Android_DEMO)

### 功能特点
---
- 支持主流RTMP服务器(simple-rtmp-server、crtmpserver、FMS 等)
- 支持所有的RTMP协议及变种（RTMP、RTMPT、RTMPE、RTMPS、RTMPTE、RTMPTS 等)
- 支持本地视频录制
- 支持动态码率
- 支持纯视频推流
- 支持后台推流
- 支持截图、闪光灯、摄像头切换、自动对焦等实用功能
- 支持视频软编/硬编
- 支持横屏/竖屏推流
- 支持低延时直播，控制直播延时2秒内。配合云帆加速P2P，全程直播稳定，控制延时4s内
- 支持推送主流分辨率视频，支持手动设置码率、帧率、缓存大小等配置
- 支持推流异常后自动重连
- 支持实时美颜

### 运行环境
---

模式 | Android版本| 备注
---|---|---
 禁止滤镜模式 | Android 4.1及以上版本| 支持后台推流、软编/硬编
 允许滤镜模式  | Android 4.3及以上版本| 支持后台音频推流、硬编

### 下载并使用SDK
---
### 1. 从github下载工程

    SDK内容说明(推流SDK的完整下载包包含SDK、DOC、Demo三部分）：
    - SDK目录：YfEncoderKit.jar、libffmpeg.so、libmuxer.so
    - DOC目录：YFEncoderKit帮助文档.pdf、README.MD
    - Demo:集成了推流sdk的所有功能示范

### 2、鉴权
获取SDK使用许可的Token,在app启动的时候调用全局静态鉴权方法。

```
    Authentication.AuthCallBack authCallBack = new Authentication.AuthCallBack() {
        @Override
        public void onAuthenticateSuccess() {
            Log.d(TAG, "鉴权成功~！");
        }

        @Override
        public void onAuthenticateError(int errorCode) {
            Log.d(TAG, "鉴权失败啦：" + errorCode);
        }
    };
Authentication.Authenticate(TOKEN, authCallBack);
```
为了防止在鉴权的时候因为网络异常而鉴权失败，可以在恢复网络的时候确认一下是否鉴权成功，如果没有则再次发起鉴权。
    
```
if(!Authentication.isAuthenticateSucceed()){
            //retry
        }
```
#### 注意事项
github或官网上下载的sdk仅支持使用测试域名进行推流，正式使用请与我们联系获取正式token并设置推流域名。

### 3、在APP上集成SDK
#### 直播推流

1、实现回调接口YfEncoderKit.RecordMonitor
    
```
    public interface RecordMonitor {
        /**
         * 成功连接上服务器
         */
        void onServerConnected();
        /**
         * 错误回调
         *
         * @param mode
         * @param camId
         * @param err
         * @param msg
         */
        void onError(int mode, int camId, int err, String msg);

        /**
         * 录制状态发生变化
         *
         * @param mode
         * @param camId
         * @param oldState
         * @param newState
         */
        void onStateChanged(int mode, int camId, int oldState,
                            int newState);

        /**
         * 生成了新的录制片段
         *
         * @param mode
         * @param camId
         * @param fragPath
         */
        void onFragment(int mode, int camId, String fragPath);

        /**
         * 录制时间刷新
         *
         * @param time
         */
        void onTimeUpdate(int time);

        /**
         * 截图结果
         *
         * @param path 截图成功时为图片路径，失败时为null
         */
        void onCapturedResult(String path);

        /**
         * buffer大小及相关策略的回调
         *
         * @param currentBitrate 当前码率
         * @param bufferMs       当前缓存大小
         * @param event          事件
         */
        void onBufferHandleCallback(int currentBitrate, int bufferMs, int event);
    }
```


2、初始化编码器并配置摄像头及预览界面

```
 //context、截图/录制视频等文件保存的根目录、允许开启滤镜、摄像头输出宽度、摄像头输出高度
        YfEncoderKit yfEncorderKit = new YfEncoderKit(this, CACHE_DIRS, mEnableFilter, PREVIEW_WIDTH, PREVIEW_HEIGHT, VIDEO_FRAME_RATE);
        yfEncorderKit.setContinuousFocus()//设置连续自动对焦
                .setLandscape(mLandscape)//设置是否横屏模式（默认竖屏）
                .setRecordMonitor(this)//设置回调
                .setDefaultCamera(true)//设置默认打开前置摄像头---不设置也默认打开后置摄像头
                .openCamera(s);//设置预览窗口GlSurfaceView
```

3、配置编码参数并开始录制

```
//设置编码参数：直播/录制、是否硬解、码率、帧率、宽、高
        yfEncorderKit.changeMode(YfEncoderKit.MODE_LIVE, HARD_ENCODER, VIDEO_BITRATE, VIDEO_WIDTH, VIDEO_HEIGHT);
        yfEncorderKit.setMaxReconnectCount(5);//自动重连次数，0代表不自动重连
        yfEncorderKit.setAdjustQualityAuto(true, 300);//打开码率自适应，每5秒统计一次上传速度
        yfEncorderKit.setBufferSizeBySec(1);//最多缓存1秒的数据，超过1秒则丢帧
        yfEncorderKit.setLiveUrl(mUrl.getText().toString());
        yfEncorderKit.startRecord();
```

4、停止推流
    
```
yfEncorderKit.stopRecord();
```

   更多功能的使用请参考Demo。
   
   

#### 本地视频录制
基本方法与直播推流无异，只是在配置编码器的时候设置为MODE_VOD并设置存放名字。

```
        yfEncorderKit.changeMode(YfEncoderKit.MODE_VOD,HARD_ENCODER,VIDEO_BITRATE,VIDEO_WIDTH,VIDEO_HEIGHT);
        yfEncorderKit.setVodSaveName("测试视频");
        yfEncorderKit.startRecord();
```



#### 注意事项
SDK区分允许滤镜（美颜）模式及不允许滤镜（美颜）模式，允许滤镜模式（需Android 4.3及以上版本）下可以开启和关闭滤镜，但后台推流仅支持音频推流；不允许滤镜模式下支持后台继续推送音视频。

##### 两种模式下摄像头输出宽高及编码宽高的注意事项

模式\宽高 | 摄像头输出宽高| 编码宽高模式| 编码宽高
---|---|---|---|---
允许滤镜模式 | 摄像头支持的宽高| 缩放| 必须保持与摄像头宽高比例一致
不允许滤镜模式 | 摄像头支持的宽高| 裁剪| 必须保证小于等于摄像头输出宽高

例如：在希望输出640x360分辨率的视频的情况下，允许滤镜模式应将摄像头输出宽高设置为1280x720，或其他缩放比例最小的同比例宽高（如华为荣耀3c的摄像头支持最小的16:9的分辨率是2560x1440），然后才能【缩放】至目标分辨率;不允许滤镜的模式下应将摄像头输出宽高设置为640x480，然后才能【裁剪】至目标分辨率。


### 接口说明
---
参考DOC目录里的YFEncoderKit帮助文档

### 反馈和建议
---
主页：云帆加速






 