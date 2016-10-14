package codepig.videocompos;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class MainActivity extends Activity {
    private Button playBtn,imgBtn,musicBtn,videoBtn;
    private ImageView imgPreview;
    private ProgressBar bufferIcon;
    private SurfaceView surfaceView;
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

    private final int IMAGE_FILE=1;
    private final int MUSIC_FILE=2;
    private final int VIDEO_FILE=3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化各面板
        findView();
    }

    private void findView(){
        playBtn=(Button) findViewById(R.id.playBtn);
        imgBtn=(Button) findViewById(R.id.imgBtn);
        musicBtn=(Button) findViewById(R.id.musicBtn);
        videoBtn=(Button) findViewById(R.id.videoBtn);
        bufferIcon=(ProgressBar) findViewById(R.id.bufferIcon);
        surfaceView=(SurfaceView) findViewById(R.id.surfaceView);
        imgPreview=(ImageView) findViewById(R.id.imgPreview);
        bufferIcon.setVisibility(View.GONE);
        imgBtn.setOnClickListener(clickBtn);
        musicBtn.setOnClickListener(clickBtn);
        videoBtn.setOnClickListener(clickBtn);

        //初始化播放器
        mPlayer=new MediaPlayer();
        initSurfaceView();
        mHandler = new Handler() {  //初始化handler
            @Override
            public void handleMessage(Message msg) { //通过handleMessage()来处理传来的消息
                if (msg.what == 0)
                    imgPreview.setImageBitmap(imageBitmap);
            }
        };
    }

    /**
     * 初始化surfaceView
     */
    private void initSurfaceView(){
        sfHolder=surfaceView.getHolder();
        sfHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d("LOGCAT", "surfaceDestroyed");
            }

            //必须监听surfaceView的创建，创建完毕后才可以处理播放
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                playBtn.setOnClickListener(clickBtn);
                //把视频画面输出到SurfaceView
                mPlayer.setDisplay(sfHolder);
                Log.d("LOGCAT", "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d("LOGCAT", "surfaceChanged");
            }
        });
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
                Log.d("LOGCAT","retart");
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
                //播放器区域按钮
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
                default:
                    break;
            }
        }
    };

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