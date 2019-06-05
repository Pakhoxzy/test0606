package com.example.blank700.musicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.example.blank700.data.MusicList;

import java.util.Random;

public class MusicService extends Service {
    // 播放控制命令，标识操作
    public static final int COMMAND_UNKNOWN = -1;
    public static final int COMMAND_PLAY = 0;
    public static final int COMMAND_PAUSE = 1;
    public static final int COMMAND_STOP = 2;
    public static final int COMMAND_RESUME = 3;
    public static final int COMMAND_PREVIOUS = 4;
    public static final int COMMAND_NEXT = 5;
    public static final int COMMAND_CHECK_IS_PLAYING = 6;
    public static final int COMMAND_SEEK_TO = 7;
    public static final int COMMAND_RANDOM = 8;
    public static final int COMMAND_REPLAY = 9;

    // 播放器状态
    public static final int STATUS_PLAYING = 0;
    public static final int STATUS_PAUSED = 1;
    public static final int STATUS_STOPPED = 2;
    public static final int STATUS_COMPLETED = 3;
    // 广播标识
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";

    //当前歌曲的序号，下标从0开始
    private int number = 0;

    // 播放状态
    private int status;

    //媒体播放类
    private MediaPlayer player = new MediaPlayer();

    //广播接收器
    private CommandReceiver receiver;
    private boolean phone=false;

    @Override
    public void onCreate() {
        super.onCreate();
        //绑定广播接收器，可以接收广播
        bindCommandReceiver();
        status = MusicService.STATUS_STOPPED;

        TelephonyManager telephonyManager=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new MyPhoneListener(),PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void bindCommandReceiver() {
        receiver = new CommandReceiver();
        IntentFilter filter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL);
        registerReceiver(receiver, filter);
    }

    class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //获取命令
            int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
            //执行命令
            switch (command) {
                case COMMAND_PLAY:
                    int newnumber = intent.getIntExtra("number", 0);
                    if (status == MusicService.STATUS_PAUSED) {
                        if (number == newnumber) {
                            resume();
                        } else {
                            number = newnumber;//这句话要写在play()的前面，因为play()里有sendBroadcastOnStatusChanged()方法要发送当前的number值；如果把这句话写在后面会出现点击item选择音乐时，音乐标题出现的是上一首音乐的标题
                            play(newnumber);
                        }
                    } else if (status == MusicService.STATUS_PLAYING) {
                        if (number != newnumber) {
                            number = newnumber;//同上
                            play(newnumber);
                        }
                    } else {
                        number = newnumber;//同上
                        play(newnumber);
                    }
                    break;
                case COMMAND_PREVIOUS:
                    moveNumberToPrevious();
                    break;
                case COMMAND_NEXT:
                    moveNumberToNext();
                    break;
                case COMMAND_PAUSE:
                    pause();
                    break;
                case COMMAND_STOP:
                    stop();
                    break;
                case COMMAND_RESUME:
                    resume();
                case COMMAND_CHECK_IS_PLAYING:
                    if (player != null && player.isPlaying()) {
                        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
                    }
                    break;
                case COMMAND_SEEK_TO:
                    seekTo(intent.getIntExtra("time",0));
                    break;
                case COMMAND_RANDOM:
                    Random random=new Random();
                    int i;
                    do {
                        i=random.nextInt(MusicList.getMusicList().size());//因为random.nextInt是随机产生一个大于等于0，小于MusicList.getMusicList().size()的整形数，如果在MusicList.getMusicList().size()后面减1了，那就不能够产生最后一个数，随机播放也就无法播放最后一首音乐
                    }while (i==number);
                    number=i;
                    play(number);
                    break;
                case COMMAND_REPLAY:
                    play(number);
                    break;
                case COMMAND_UNKNOWN:
                    break;
                default:
                    break;
            }
        }
    }

    //发送广播，提醒状态改变了
    private void sendBroadcastOnStatusChanged(int status) {
        Intent intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        intent.putExtra("status", status);
        if (status!=STATUS_STOPPED){
            intent.putExtra("time",player.getCurrentPosition());
            intent.putExtra("duration",player.getDuration());
            intent.putExtra("number",number);
            intent.putExtra("musicName",MusicList.getMusicList().get(number).getMusicName());
            intent.putExtra("musicArtist",MusicList.getMusicList().get(number).getMusicArtist());
        }
        sendBroadcast(intent);
    }

    //读取音乐文件
    private void load(int number) {
        try {
            player.reset();
            player.setDataSource(MusicList.getMusicList().get(number).getMusicPath());
            player.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //注册监听器
        player.setOnCompletionListener(completionListener);
    }

    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            if (player.isLooping()) {
                replay();
            } else {
                sendBroadcastOnStatusChanged(MusicService.STATUS_COMPLETED);
            }
        }
    };

    //播放音乐
    private void play(int number) {
        if (player != null && player.isPlaying()) {
            player.stop();
        }
        load(number);
        player.start();
        status = MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
    }

    //暂停音乐
    private void pause() {
        if (player.isPlaying()) {
            player.pause();
            status = MusicService.STATUS_PAUSED;
            sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
        }
    }

    //停止播放
    private void stop() {
        if (status != MusicService.STATUS_STOPPED) {
            player.stop();
            status = MusicService.STATUS_STOPPED;
            sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
        }
    }

    //恢复播放（暂停之后）
    private void resume() {
        player.start();
        status = MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
    }

    //重新播放（播放完成之后）
    private void replay() {
        player.start();
        status = MusicService.STATUS_PLAYING;
        sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
    }

    private void moveNumberToNext() {
        //判断是否有到达列表底端
        if ((number) == MusicList.getMusicList().size() - 1) {
            Toast.makeText(MusicService.this, MusicService.this.getString(R.string.tip_reach_bottom),
                    Toast.LENGTH_SHORT).show();
        } else {
            ++number;
            play(number);
        }
    }

    private void moveNumberToPrevious() {
        //判断是否有到达列表顶端
        if (number == 0) {
            Toast.makeText(MusicService.this, MusicService.this.getString(R.string.tip_reach_top),
                    Toast.LENGTH_SHORT).show();
        } else {
            --number;
            play(number);
        }
    }

    //电话来电处理
    private  final class MyPhoneListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state){
                case TelephonyManager.CALL_STATE_RINGING:
                    if (status==MusicService.STATUS_PLAYING){
                        pause();
                        phone=true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (phone==true){
                        resume();
                        phone=false;
                    }
                    break;
            }
        }
    }

    private void seekTo(int time){
        player.seekTo(time);
    }
}
