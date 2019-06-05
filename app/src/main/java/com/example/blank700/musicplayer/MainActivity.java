package com.example.blank700.musicplayer;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blank700.data.Music;
import com.example.blank700.data.MusicList;
import com.example.blank700.model.PropertyBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //显示组件
    private ImageButton imgBtn_PlayMode;
    private ImageButton imgBtn_Previous;
    private ImageButton imgBtn_PlayOrPause;
    private ImageButton imgBtn_Stop;
    private ImageButton imgBtn_Next;
    private ListView list;

    //歌曲列表
    private ArrayList<Music> musicArrayList;

    //当前歌曲的序号，下标从0开始
    private int number = 0;

    // 播放状态
    private int status;

    //广播接收器
    private StatusChangedReceiver receiver;
    private RelativeLayout root_layout;

    private TextView textView_musicname;//进度条下方显示的歌曲名称

    private SeekBar seekBar;
    private TextView text_Current;
    private TextView text_Duration;
    private Handler seekBarHandler;

    //当前歌曲的持续时间和当前位置，作用于进度条
    private int duration;
    private int time;

    //进度条控制常量
    private static final int PROGRESS_INCREASE = 0;
    private static final int PROGRESS_PAUSE = 1;
    private static final int PROGRESS_RESET = 2;

    //播放模式常量
    private static final int MODE_LIST_SEQUENCE = 0;
    private static final int MODE_SINGLE_CYCLE = 1;
    private static final int MODE_LIST_CYCLE = 2;
    private static final int MODE_LIST_RANDOM = 3;
    private int playmode;


    //退出标记
    private static boolean isExit = false;

    //音量控制
    private TextView textView_vol;
    private SeekBar seekBar_vol;

    //睡眠模式相关组件及常量
    private ImageView imageView_sleep;
    private Timer timer_sleep;
    private static final boolean NOTSLEEP = false;
    private static final boolean ISSLEEP = true;
    //默认睡眠时间
    private int sleepminute = 10;
    //标记是否打开睡眠模式
    private static boolean sleepmode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findView();
        registerListeners();
        initMusicList();
        initListView();
        checkMusicfile();

        duration = 0;
        time = 0;

        //绑定广播接收器，可以接收广播
        bindStatusChangedReceiver();

        initSeekBarHandler();

        startService(new Intent(this, MusicService.class));
        status = MusicService.COMMAND_STOP;

        //默认播放模式是顺序播放
        playmode = MainActivity.MODE_LIST_SEQUENCE;

        //默认睡眠模式为关闭
        sleepmode = MainActivity.NOTSLEEP;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);

        PropertyBean propertyBean = new PropertyBean(MainActivity.this);
        String theme = propertyBean.getTheme();
        //设置activity的主题
        setTheme(theme);
        audio_Control();
        //睡眠模式打开时显示图标，关闭时隐藏图标
        if (sleepmode == MainActivity.ISSLEEP) {
            imageView_sleep.setVisibility(View.VISIBLE);
        } else {
            imageView_sleep.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        if (status == MusicService.STATUS_STOPPED) {
            stopService(new Intent(this, MusicService.class));
        }
        super.onDestroy();
    }

    /*获取显示组件*/
    private void findView() {
        imgBtn_PlayMode = (ImageButton) findViewById(R.id.imageButton_playmode);
        imgBtn_Previous = (ImageButton) findViewById(R.id.imageButton1);
        imgBtn_PlayOrPause = (ImageButton) findViewById(R.id.imageButton2);
        imgBtn_Stop = (ImageButton) findViewById(R.id.imageButton3);
        imgBtn_Next = (ImageButton) findViewById(R.id.imageButton4);
        list = (ListView) findViewById(R.id.listView1);
        root_layout = (RelativeLayout) findViewById(R.id.relativeLayout1);

        textView_musicname = (TextView) findViewById(R.id.textView_musicname);

        //进度条和歌曲时间
        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        text_Current = (TextView) findViewById(R.id.textView1);
        text_Duration = (TextView) findViewById(R.id.textView2);

        //音量控制
        textView_vol = (TextView) findViewById(R.id.main_tv_volumeText);
        seekBar_vol = (SeekBar) findViewById(R.id.main_sb_volumebar);

        //睡眠模式
        imageView_sleep = (ImageView) findViewById(R.id.main_imageview_sleep);

    }

    /*为显示组件注册监听器*/
    private void registerListeners() {
        imgBtn_PlayMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playmode == MainActivity.MODE_LIST_SEQUENCE) {
                    playmode = MainActivity.MODE_SINGLE_CYCLE;
                    imgBtn_PlayMode.setBackgroundResource(R.drawable.playmode_singlecycle);
                } else if (playmode == MainActivity.MODE_SINGLE_CYCLE) {
                    playmode = MainActivity.MODE_LIST_CYCLE;
                    imgBtn_PlayMode.setBackgroundResource(R.drawable.playmode_listcycle);
                } else if (playmode == MainActivity.MODE_LIST_CYCLE) {
                    playmode = MainActivity.MODE_LIST_RANDOM;
                    imgBtn_PlayMode.setBackgroundResource(R.drawable.playmode_listrandom);
                } else if (playmode == MainActivity.MODE_LIST_RANDOM) {
                    playmode = MainActivity.MODE_LIST_SEQUENCE;
                    imgBtn_PlayMode.setBackgroundResource(R.drawable.playmode_sequence);
                }

            }
        });

        imgBtn_Previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playmode) {
                    case MainActivity.MODE_LIST_RANDOM:
                        sendBroadcastOnCommand(MusicService.COMMAND_RANDOM);
                        break;
                    default:
                        sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
                }
            }
        });
        imgBtn_PlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    case MusicService.STATUS_PLAYING:
                        sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                        break;
                    case MusicService.STATUS_PAUSED:
                        sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                        break;
                    case MusicService.COMMAND_STOP:
                        sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                    default:
                        break;
                }
            }
        });
        imgBtn_Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
            }
        });
        imgBtn_Next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playmode) {
                    case MainActivity.MODE_LIST_RANDOM:
                        sendBroadcastOnCommand(MusicService.COMMAND_RANDOM);
                        break;
                    case MainActivity.MODE_LIST_CYCLE:
                        if (number == musicArrayList.size() - 1) {
                            number = 0;
                            sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        } else {
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                        }
                        break;
                    default:
                        sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                }
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                number = position;
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //进度条暂停移动
                seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (status != MusicService.STATUS_STOPPED) {
                    time = seekBar.getProgress();
                    //更新文本
                    text_Current.setText(formatTime(time));
                    //发送广播给MusicServerz，执行跳转
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                }
                if (status == MusicService.STATUS_PLAYING) {
                    //发送广播给MusicServer，执行跳转
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                    //进度条恢复移动
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                }
            }
        });
    }

    private void initMusicList() {
        musicArrayList = MusicList.getMusicList();
        if (musicArrayList.isEmpty()) {
            Cursor mMusicCursor = this.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.DURATION,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.DISPLAY_NAME
                    }, null, null, MediaStore.Audio.AudioColumns.TITLE);

            if (mMusicCursor != null) {
                //标题
                int indexTitle = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
                //艺术家
                int indexArtist = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
                //路径
                int indexPath = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
                //总时长
                int indexTotalTime = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);

                for (mMusicCursor.moveToFirst(); !mMusicCursor.isAfterLast(); mMusicCursor.moveToNext()) {
                    String strTitle = mMusicCursor.getString(indexTitle);
                    String strArtist = mMusicCursor.getString(indexArtist);
                    String strPath = mMusicCursor.getString(indexPath);
                    String strTotalTime = mMusicCursor.getString(indexTotalTime);
                    if (strArtist.equals("<unknown>"))
                        strArtist = "无艺术家";
                    Music music = new Music(strTitle, strArtist, strPath, strTotalTime);
                    musicArrayList.add(music);
                }
            }
        }
    }

    private void initListView() {
        List<Map<String, String>> list_map = new ArrayList<Map<String, String>>();
        HashMap<String, String> map;
        SimpleAdapter simpleAdapter;
        for (Music music : musicArrayList) {
            map = new HashMap<String, String>();
            map.put("musicName", music.getMusicName());
            map.put("musicArtist", music.getMusicArtist());
            list_map.add(map);
        }

        String[] from = new String[]{"musicName", "musicArtist"};
        int[] to = {R.id.listview_tv_title_item, R.id.listview_tv_artist_item};

        simpleAdapter = new SimpleAdapter(this, list_map, R.layout.listview, from, to);
        list.setAdapter(simpleAdapter);
    }

    private void checkMusicfile() {
        if (musicArrayList.isEmpty()) {
            imgBtn_Previous.setEnabled(false);
            imgBtn_PlayOrPause.setEnabled(false);
            imgBtn_Stop.setEnabled(false);
            imgBtn_Next.setEnabled(false);
            Toast.makeText(getApplicationContext(), "当前没有歌曲文件", Toast.LENGTH_SHORT).show();
        } else {
            imgBtn_Previous.setEnabled(true);
            imgBtn_PlayOrPause.setEnabled(true);
            imgBtn_Stop.setEnabled(true);
            imgBtn_Next.setEnabled(true);
        }
    }

    //绑定广播接收器
    private void bindStatusChangedReceiver() {
        receiver = new StatusChangedReceiver();
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver, filter);
    }

    //发送命令，控制音乐播放
    private void sendBroadcastOnCommand(int command) {
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", command);
        //根据不同命令，封装不同的数据
        switch (command) {
            case MusicService.COMMAND_PLAY:
                intent.putExtra("number", number);
                break;
            case MusicService.COMMAND_SEEK_TO:
                intent.putExtra("time", time);
                break;
            case MusicService.COMMAND_PREVIOUS:
                break;
            case MusicService.COMMAND_NEXT:
                break;
            case MusicService.COMMAND_PAUSE:
                break;
            case MusicService.COMMAND_STOP:
                break;
            case MusicService.COMMAND_RESUME:
                break;
            case MusicService.COMMAND_REPLAY:
                break;
            default:
                break;
        }
        sendBroadcast(intent);
    }

    //用于播放器状态更新的接收广播
    class StatusChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String musicName = intent.getStringExtra("musicName");
            String musicArtist = intent.getStringExtra("musicArtist");
            //获取播放器状态
            status = intent.getIntExtra("status", -1);
            switch (status) {
                case MusicService.STATUS_PLAYING:
                    imgBtn_PlayOrPause.setBackgroundResource(R.drawable.pause);

                    textView_musicname.setText(musicName + "-" + musicArtist);

                    seekBarHandler.removeMessages(PROGRESS_INCREASE);
                    time = intent.getIntExtra("time", 0);
                    duration = intent.getIntExtra("duration", 0);
                    number = intent.getIntExtra("number", 0);
                    list.setSelection(number);
                    seekBar.setProgress(time);
                    seekBar.setMax(duration);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                    text_Duration.setText(formatTime(duration));
                    break;
                case MusicService.STATUS_PAUSED:
                    imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);

                    textView_musicname.setText(musicName + "-" + musicArtist);

                    seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
                    break;
                case MusicService.STATUS_STOPPED:
                    imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);

                    textView_musicname.setText("");

                    time = 0;
                    duration = 0;
                    text_Current.setText(formatTime(time));
                    text_Duration.setText(formatTime(duration));
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    break;
                case MusicService.STATUS_COMPLETED:
                    number = intent.getIntExtra("number", 0);
                    if (playmode == MainActivity.MODE_LIST_SEQUENCE) {
                        if (number == MusicList.getMusicList().size() - 1) {
                            sendBroadcastOnCommand(MusicService.STATUS_STOPPED);
                            imgBtn_PlayOrPause.setBackgroundResource(R.drawable.play);
                        } else {
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                        }
                    } else if (playmode == MainActivity.MODE_SINGLE_CYCLE) {
                        sendBroadcastOnCommand(MusicService.COMMAND_REPLAY);
                    } else if (playmode == MainActivity.MODE_LIST_CYCLE) {
                        if (number == MusicList.getMusicList().size() - 1) {
                            number = 0;
                            sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                        } else {
                            sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                        }
                    } else if (playmode == MODE_LIST_RANDOM) {
                        sendBroadcastOnCommand(MusicService.COMMAND_RANDOM);
                        break;
                    }
                    seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
                    MainActivity.this.setTitle("");
                    break;
                default:
                    break;
            }
        }
    }

    //设置Activity的主题
    private void setTheme(String theme) {
        if ("彩色".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.bg_color);
        } else if ("花朵".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.bg_digit_flower);
        } else if ("群山".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.bg_mountain);
        } else if ("小狗".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.bg_running_dog);
        } else if ("冰雪".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.bg_snow);
        } else if ("女孩".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.bg_music_girl);
        } else if ("朦胧".equals(theme)) {
            root_layout.setBackgroundResource(R.drawable.bg_blur);
        }
    }

    //创建菜单
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //处理菜单点击事件

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_theme:
                new AlertDialog.Builder(this).setTitle("请选择主题").setItems(R.array.theme,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String theme = PropertyBean.THEMES[which];
                                MainActivity.this.setTheme(theme);
                                PropertyBean propertyBean = new PropertyBean(MainActivity.this);
                                propertyBean.setAndSaveTheme(theme);
                            }
                        }).show();
                break;
            case R.id.menu_about:
                new AlertDialog.Builder(MainActivity.this).setTitle("提示")
                        .setMessage(R.string.about2).show();
                break;
            case R.id.menu_quit:
                new AlertDialog.Builder(this).setTitle("提示").setPositiveButton("确定"
                        , new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        }).setNegativeButton("取消"
                        , new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
                break;
            case R.id.menu_playmode:
                String[] mode = new String[]{"顺序模式", "单曲循环", "列表循环", "随机播放"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("播放模式");
                builder.setSingleChoiceItems(mode, playmode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        playmode = which;
                    }
                });
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (playmode) {
                            case 0:
                                playmode = MainActivity.MODE_LIST_SEQUENCE;
                                Toast.makeText(getApplicationContext(), R.string.sequence, Toast.LENGTH_SHORT).show();
                                break;
                            case 1:
                                playmode = MainActivity.MODE_SINGLE_CYCLE;
                                Toast.makeText(getApplicationContext(), R.string.singlecycle, Toast.LENGTH_SHORT).show();
                                break;
                            case 2:
                                playmode = MainActivity.MODE_LIST_CYCLE;
                                Toast.makeText(getApplicationContext(), R.string.listcycle, Toast.LENGTH_SHORT).show();
                                break;
                            case 3:
                                playmode = MainActivity.MODE_LIST_RANDOM;
                                Toast.makeText(getApplicationContext(), R.string.listrandom, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                    }
                });
                builder.create().show();
                break;
            case R.id.menu_sleep:
                showSleepDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private String formatTime(int msec) {
        int minute = msec / 1000 / 60;
        int second = msec / 1000 % 60;
        String minuteString;
        String secondString;
        if (minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = "" + minute;
        }
        if (second < 10) {
            secondString = "0" + second;
        } else {
            secondString = "" + second;
        }
        return minuteString + ":" + secondString;
    }

    private void initSeekBarHandler() {
        seekBarHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case PROGRESS_INCREASE:
                        if (seekBar.getProgress() < duration) {
                            //进度条前进一秒
                            seekBar.setProgress(time);
                            //seekBar.incrementProgressBy(1000);
                            seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                            //修改显示当前进度的文本
                            text_Current.setText(formatTime(time));
                            time += 1000;
                        }
                        break;
                    case PROGRESS_PAUSE:
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        break;
                    case PROGRESS_RESET:
                        //重置进度条画面
                        seekBarHandler.removeMessages(PROGRESS_INCREASE);
                        seekBar.setProgress(0);
                        text_Current.setText("00:00");
                        break;
                }
            }
        };
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int progress;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                exitByDoubleClick();
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                progress = seekBar_vol.getProgress();
                if (progress != 0) {
                    seekBar_vol.setProgress(progress - 1);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                progress = seekBar_vol.getProgress();
                if (progress != seekBar_vol.getMax()) {
                    seekBar_vol.setProgress(progress + 1);
                }
                return true;
        }
        return false;
    }

    private void exitByDoubleClick() {
        Timer timer = null;
        if (isExit == false) {
            isExit = true;
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            System.exit(0);
        }
    }

    private void audio_Control() {
        //获取音量管理器
        final AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        //设置当前调整音量大小只是针对媒体音乐
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //设置滑动条最大值
        final int max_progress = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekBar_vol.setMax(max_progress);
        //获取当前音量
        int progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBar_vol.setProgress(progress);
        textView_vol.setText("音量：" + (progress * 100 / max_progress) + "%");
        seekBar_vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView_vol.setText("音量：" + (progress * 100) / (max_progress) + "%");
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void showSleepDialog() {
        //先用getLayoutInflater().inflate方法获取布局，用来初始化一个View类对象
        final View userview = this.getLayoutInflater().inflate(R.layout.dialog, null);

        //通过View类的findViewById方法获取到组件对象
        final TextView textView_minute = (TextView) userview.findViewById(R.id.dialog_textview);
        final Switch switch_1 = (Switch) userview.findViewById(R.id.dialog_switch);
        final SeekBar seekBar = (SeekBar) userview.findViewById(R.id.dialog_seekbar);

        textView_minute.setText("睡眠于：" + sleepminute + "分钟");
        //根据当前的睡眠状态来确定Switch的状态
        if (sleepmode == MainActivity.ISSLEEP) {
            switch_1.setChecked(true);
        }
        seekBar.setMax(60);
        seekBar.setProgress(sleepminute);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sleepminute = progress;
                textView_minute.setText("睡眠于" + sleepminute + "分钟");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        switch_1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sleepmode = isChecked;
            }
        });
        //定义定时器任务
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.exit(0);
            }
        };
        //定义对话框以及初始化
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("选择睡眠时间（0-60分钟）");
        //设置布局
        dialog.setView(userview);
        //设置取消按钮响应事件
        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //设置重置按钮响应事件
        dialog.setNeutralButton("重置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (sleepmode == MainActivity.ISSLEEP) {
                    timerTask.cancel();
                    timer_sleep.cancel();
                }
                sleepmode = MainActivity.NOTSLEEP;
                sleepminute = 20;
                imageView_sleep.setVisibility(View.INVISIBLE);
            }
        });
        //设置确定按钮响应事件
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (sleepmode == MainActivity.ISSLEEP) {
                    timer_sleep = new Timer();
                    int sleeptime = seekBar.getProgress();//这里不能用全局变量time，所以要重新定义一个局部变量
                    //启动任务，time*60*1000毫秒后执行
                    timer_sleep.schedule(timerTask, sleeptime * 60 * 1000);
                    imageView_sleep.setVisibility(View.VISIBLE);
                } else {
                    //取消任务
                    timerTask.cancel();
                    if (timer_sleep != null) {
                        timer_sleep.cancel();
                    }
                    dialog.dismiss();
                    imageView_sleep.setVisibility(View.INVISIBLE);
                }
            }
        });
        dialog.show();
    }
}
