package codepig.videocompos;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.wysaid.myUtils.FileUtil;
import org.wysaid.myUtils.ImageUtil;
import org.wysaid.nativePort.CGEFFmpegNativeLibrary;
import org.wysaid.nativePort.CGEFrameRecorder;
import org.wysaid.nativePort.CGENativeLibrary;
import org.wysaid.view.CameraRecordGLSurfaceView;

import java.io.IOException;
import java.io.InputStream;

/**
 * 使用第三方库(android-gpuimage-plus-master)的滤镜效果。
 * drawable和assets内分别放用于合成的相框图片和对应的mask文件
 * Created by QZD on 2016/10/25.
 */

public class gpuImageFilter extends Activity{
    private Button cameraBtn,playBtn,imgBtn,musicBtn,videoBtn,sepiaBtn,grayBtn,sharpBtn,edgeBtn,switchCameraBtn,blendBtn,frameBtn;
    private ImageView imgPreview;
    private ProgressBar bufferIcon;
    //    private SurfaceView surfaceView;
    //播放器
    private MediaPlayer mPlayer;
    private MediaPlayer aPlayer;
    private SurfaceHolder sfHolder;
    private Uri imageUri;
    private Uri audioUri;
    private Uri videoUri;
    private String videoUrl="";
    private String imageUrl="";
    private String musicUrl="";
    private int file_type=0;
    private Bitmap imageBitmap;
    private Handler mHandler;
    private boolean isPlaying=false;
    private boolean isRecording = false;
    private String recordFilename="testVideo";
    private CameraRecordGLSurfaceView mCameraView;
    public static String lastVideoPathFileName = FileUtil.getPath() + "/lastVideoPath.txt";
    private int frameIndex=-1;

    private final int IMAGE_FILE=1;
    private final int MUSIC_FILE=2;
    private final int VIDEO_FILE=3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        //初始化各面板
        findView();
    }

    private void findView(){
        playBtn=(Button) findViewById(R.id.playBtn);
        cameraBtn=(Button) findViewById(R.id.cameraBtn);
        frameBtn=(Button) findViewById(R.id.frameBtn);
        switchCameraBtn=(Button) findViewById(R.id.switchCameraBtn);
        imgBtn=(Button) findViewById(R.id.imgBtn);
        blendBtn=(Button) findViewById(R.id.blendBtn);
        musicBtn=(Button) findViewById(R.id.musicBtn);
        videoBtn=(Button) findViewById(R.id.videoBtn);
        sepiaBtn=(Button) findViewById(R.id.sepiaBtn);
        grayBtn=(Button) findViewById(R.id.grayBtn);
        sharpBtn=(Button) findViewById(R.id.sharpBtn);
        edgeBtn=(Button) findViewById(R.id.edgeBtn);
        bufferIcon=(ProgressBar) findViewById(R.id.bufferIcon);
//        surfaceView=(SurfaceView) findViewById(R.id.surfaceView);
        imgPreview=(ImageView) findViewById(R.id.imgPreview);

        mCameraView = (CameraRecordGLSurfaceView) findViewById(R.id.myGLSurfaceView);
        mCameraView.presetCameraForward(false);

        bufferIcon.setVisibility(View.GONE);
        imgBtn.setOnClickListener(clickBtn);
        switchCameraBtn.setOnClickListener(clickBtn);
        musicBtn.setOnClickListener(clickBtn);
        frameBtn.setOnClickListener(clickBtn);
        videoBtn.setOnClickListener(clickBtn);
        sepiaBtn.setOnClickListener(clickBtn);
        grayBtn.setOnClickListener(clickBtn);
        sharpBtn.setOnClickListener(clickBtn);
        blendBtn.setOnClickListener(clickBtn);
        edgeBtn.setOnClickListener(clickBtn);
        cameraBtn.setOnClickListener(clickBtn);

        blendBtn.setVisibility(View.GONE);

        //初始化播放器
        mPlayer=new MediaPlayer();
//        initSurfaceView();
        mHandler = new Handler() {  //初始化handler
            @Override
            public void handleMessage(Message msg) { //通过handleMessage()来处理传来的消息
                if (msg.what == 0)
                    imgPreview.setImageBitmap(imageBitmap);
            }
        };
    }

    /**
     * 播放视频
     * @param _url
     */
    public void playVideo(String _url){
        if(_url!=""){
            try {
                Log.d("LOGCAT", "play:" + _url);
                if(mPlayer==null){
                    Log.d("LOGCAT", "new player");
                    mPlayer=new MediaPlayer();
                }else{
                    Log.d("LOGCAT", "reset player");
                    mPlayer.reset();
                }
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                //设置需要播放的视频
//                    mPlayer.setDataSource("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");//test
                mPlayer.setDataSource(_url);
                mPlayer.prepareAsync();
                mPlayer.setOnBufferingUpdateListener(bufferingListener);
                mPlayer.setOnPreparedListener(preparedListener);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    /**
     * 播放音乐
     */
    private void playMusic(String _url){
        if(aPlayer==null){
            try {
                aPlayer = new MediaPlayer();
                aPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                aPlayer.setDataSource(_url);
                aPlayer.prepareAsync();
                Log.d("LOGCAT","set audioPlayer");
                aPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        // 装载完毕 开始播放流媒体
                        Log.d("LOGCAT","play audioPlayer");
                        aPlayer.start();
                    }
                });
            }catch (Exception e){
                Log.d("LOGCAT","err:"+e.toString());
            }
        }else{
            Log.d("LOGCAT","has audioPlayer");
            if(!aPlayer.isPlaying()) {
                Log.d("LOGCAT","reStart");
                aPlayer.start();
            }
        }
    }

    /**
     * 停止播放
     */
    private void stopPlayer(){
        if(mPlayer!=null && mPlayer.isPlaying()){
            mPlayer.stop();
        }
        if(aPlayer!=null && aPlayer.isPlaying()){
            aPlayer.stop();
        }
    }
    /**
     * 监听缓冲进度更新
     */
    MediaPlayer.OnBufferingUpdateListener bufferingListener=new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
        }
    };

    /**
     * prepare监听
     */
    MediaPlayer.OnPreparedListener preparedListener=new MediaPlayer.OnPreparedListener(){
        @Override
        public void onPrepared(MediaPlayer mp)
        {
            bufferIcon.setVisibility(View.GONE);
            //播放
            mPlayer.start();
        }
    };

    /**
     * 打开文件
     */
    private void chooseFile(){
//        try {
        Intent intent = new Intent();
        //使用ACTION_PICK时google原生5.1系统音频选择会报错，使用ACTION_GET_CONTENT时小米系统获得的是空指针
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Log.d("LOGCAT", "file type:" + file_type);
        switch (file_type) {
            case 1:
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                break;
            case 2:
                intent.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                intent.setType("audio/*");
                break;
            case 3:
                intent.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                intent.setType("video/*");
                break;
        }
        startActivityForResult(intent, 0x1);
//        }catch (Exception e){
//        }
    }

    /**
     * 按钮监听
     */
    private View.OnClickListener clickBtn = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.frameBtn:
                    frameFilter(0);
                    blendBtn.setVisibility(View.VISIBLE);
                    break;
                case R.id.playBtn:
                    if(isPlaying){
                        stopPlayer();
                        playBtn.setText("play");
                        isPlaying=false;
                    }else{
                        playVideo(videoUrl);
                        playBtn.setText("stop");
                        isPlaying=true;
                    }
                    break;
                case R.id.cameraBtn:
                    //录制
                    isRecording = !isRecording;
                    if(isRecording)
                    {
                        cameraBtn.setText("正在录制");
                        Log.i("LOGCAT", "Start recording...");
                        mCameraView.setClearColor(1.0f, 0.0f, 0.0f, 0.3f);
                        recordFilename = FileUtil.getPath() + "/rec_" + System.currentTimeMillis() + ".mp4";
                        mCameraView.startRecording(recordFilename, new CameraRecordGLSurfaceView.StartRecordingCallback() {
                            @Override
                            public void startRecordingOver(boolean success) {
                                if (success) {
                                    Log.i("LOGCAT", "启动录制成功");
                                } else {
                                    Log.i("LOGCAT", "启动录制失败");
                                }
                            }
                        });
                    }
                    else
                    {
                        Log.i("LOGCAT", "录制完毕， 存储为 " + recordFilename);
                        cameraBtn.setText("录制完毕");
                        Log.i("LOGCAT", "End recording...");
                        mCameraView.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                        mCameraView.endRecording(new CameraRecordGLSurfaceView.EndRecordingCallback() {
                            @Override
                            public void endRecordingOK() {
                                Log.i("LOGCAT", "End recording OK");
                            }
                        });
                    }
                    break;
                case R.id.imgBtn:
                    file_type=IMAGE_FILE;
                    chooseFile();
                    break;
                case R.id.musicBtn:
                    file_type=MUSIC_FILE;
                    chooseFile();
                    break;
                case R.id.videoBtn:
                    file_type=VIDEO_FILE;
                    chooseFile();
                    break;
                case R.id.switchCameraBtn:
                    mCameraView.switchCamera();
                    break;
                case R.id.sepiaBtn:
                    mCameraView.setFilterWithConfig(value.effectConfigs[0]);
                    break;
                case R.id.grayBtn:
                    mCameraView.setFilterWithConfig(value.effectConfigs[1]);
                    break;
                case R.id.sharpBtn:
                    mCameraView.setFilterWithConfig(value.effectConfigs[20]);
                    break;
                case R.id.edgeBtn:
                    mCameraView.setFilterWithConfig(value.effectConfigs[30]);
                    break;
                case R.id.blendBtn:
                    if(frameIndex>-1) {
                        blendVideo(frameIndex);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 合成相框
     */
    private void blendVideo(int _index){
        final String outputFilename = FileUtil.getPath() + "/blendVideo.mp4";
        final String inputFileName = recordFilename;
        if(inputFileName == null) {
            Log.e("LOGCAT", "no video is recorded");
            return;
        }
        Bitmap bmp;
        try {
            AssetManager am = getAssets();
            InputStream is;
            is = am.open("frame1.png");//使用assets里的图片做合成。遮罩图基于次图片制作。
            bmp = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e("LOGCAT", "Can not open blend image file!");
            bmp = null;
        }
        //bmp is used for watermark,
        //and ususally the blend mode is CGE_BLEND_ADDREV for watermarks.
        Log.d("LOGCAT", "start blend!");
        final Bitmap _bmp=bmp;
//        Runnable blendRun=new Runnable() {
//            @Override
//            public void run() {
                CGEFFmpegNativeLibrary.generateVideoWithFilter(outputFilename, inputFileName, "", 1.0f, _bmp, CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV, 1.0f, false);
                Log.d("LOGCAT", "Done! The file is generated at: " + outputFilename);
                Toast.makeText(this, "Done! The file is generated at: " + outputFilename, Toast.LENGTH_LONG).show();
//            }
//        };
//        ThreadPoolUtils.execute(blendRun);
    }

    /**
     * 相框遮罩
     */
    private void frameFilter(int _index){
        frameIndex=_index;
        boolean mIsUsingShape = false;
        Bitmap mBmp = BitmapFactory.decodeResource(getResources(), R.drawable.frame1);//使用遮罩图
        mIsUsingShape = !mIsUsingShape;
        if (mIsUsingShape) {
            if (mBmp != null)
                mCameraView.setMaskBitmap(mBmp, false, new CameraRecordGLSurfaceView.SetMaskBitmapCallback() {
                    @Override
                    public void setMaskOK(CGEFrameRecorder recorder) {
                        //flip mask
                        if(mCameraView.isUsingMask())
                            recorder.setMaskFlipScale(1.0f, -1.0f);
                    }
                });
        } else {
            mCameraView.setMaskBitmap(null, false);
        }
    }

    /**
     * 监听文件选择
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0x1 && resultCode == Activity.RESULT_OK && data!=null) {
            switch (file_type){
                case IMAGE_FILE://不同机型系统，得到的fileUri.getPath()值不同，所以以不同的方式获取地址
                    try{
                        imageUri = data.getData();
                        Log.d("LOGCAT", "uri path:"+imageUri.getPath()+"   "+imageUri.toString());
                        String[] pojo = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(imageUri, pojo, null, null, null);
                        if (cursor != null) {
                            /*这部分代码在ACTION_GET_CONTENT模式下为空，在ACTION_PICK模式下可以得到具体地址
                            int colunm_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            cursor.moveToFirst();
                            imageUrl = cursor.getString(colunm_index);
                            cursor.close();
                            //以上代码获取图片路径
                            Log.d("LOGCAT","path:"+imageUrl);
                            */
                            imgPreview.setImageURI(imageUri);
                        }else{
                            imageUrl=imageUri.getPath();
                            Log.d("LOGCAT","path:"+imageUrl);
                            Runnable bmpR=new Runnable() {
                                @Override
                                public void run() {
                                    imageBitmap = imageLoader.returnBitMapLocal(imageUrl, 300, 200);
                                    if (imageBitmap != null){
                                        Message msg = new Message();
                                        msg.what = 0;
                                        mHandler.sendMessage(msg);
                                    }
                                }
                            };
                            ThreadPoolUtils.execute(bmpR);
                        }
                    }catch (Exception e){
                        Log.d("LOGCAT",e.toString());
                    }
                    break;
                case MUSIC_FILE:
                    try{
                        audioUri = data.getData();
                        Log.d("LOGCAT", "uri path:"+audioUri.getPath()+"   "+audioUri.toString());
                        String[] pojo = {MediaStore.Audio.Media.DATA};
                        Cursor cursor = getContentResolver().query(audioUri, pojo, null, null, null);
                        if (cursor != null) {
                            /*这部分代码在ACTION_GET_CONTENT模式下为空，在ACTION_PICK模式下可以得到具体地址
                            int colunm_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            cursor.moveToFirst();
                            imageUrl = cursor.getString(colunm_index);
                            cursor.close();
                            */
                        }else{
                            musicUrl=audioUri.getPath();
                            Log.d("LOGCAT","path:"+musicUrl);
                            playMusic(musicUrl);
                        }
                    }catch (Exception e){
                        Log.d("LOGCAT",e.toString());
                    }
                    break;
                case VIDEO_FILE:
                    try{
                        videoUri = data.getData();
                        Log.d("LOGCAT", "uri path:"+videoUri.getPath()+"   "+videoUri.toString());
                        String[] pojo = {MediaStore.Video.Media.DATA};
                        Cursor cursor = getContentResolver().query(videoUri, pojo, null, null, null);
                        if (cursor != null) {
                            /*这部分代码在ACTION_GET_CONTENT模式下为空，在ACTION_PICK模式下可以得到具体地址
                            int colunm_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            cursor.moveToFirst();
                            imageUrl = cursor.getString(colunm_index);
                            cursor.close();
                            */
                        }else{
                            videoUrl=videoUri.getPath();
                            Log.d("LOGCAT","path:"+videoUrl);
                            playVideo(videoUrl);
                        }
                    }catch (Exception e){
                        Log.d("LOGCAT",e.toString());
                    }
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
