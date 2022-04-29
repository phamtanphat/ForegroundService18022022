package com.example.foregroundservice18022022;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MyService extends Service {

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private NotificationManager notificationManager;
    private Notification notification;
    private boolean isPlaying = true;

    private int RESUME_MUSIC_CODE = 0;
    private int PAUSE_MUSIC_CODE = 1;
    private OnListenDuration onListenDuration;


    private final class ServiceHandler extends Handler {
        private MediaPlayer mediaPlayer;
        private int currentTime = 0;
        private Handler handler;

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            switch (msg.what) {
                case -1:
                    int resourceMusic = bundle.getInt("mp3");
                    startMp3(resourceMusic);
                    break;
                case 0:
                    resumeMp3();
                    break;
                case 1:
                    pauseMp3();
                    break;
            }
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration()) {
                                onListenDuration.onCurrentDuration(mediaPlayer.getCurrentPosition());
                            }
                            if (isPlaying){
                                handler.postDelayed(this,1000);
                            }
                        }
                    }, 1000);
                }
            }
        }

        private void pauseMp3() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()){
                currentTime = mediaPlayer.getCurrentPosition();
                mediaPlayer.pause();
                isPlaying = false;
                notificationManager.notify(1, makeNotification("Music 1", "Singer A", false));
            }
        }

        private void resumeMp3() {
            if (mediaPlayer != null){
                mediaPlayer.seekTo(currentTime);
                mediaPlayer.start();
                isPlaying = true;
                notificationManager.notify(1, makeNotification("Music 1", "Singer A", true));
            }
        }

        private void startMp3(int resourceMusic) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = MediaPlayer.create(getApplicationContext(), resourceMusic);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    isPlaying = true;
                    mediaPlayer.start();
                }
            });
        }

    }


    private class MyBound extends Binder {

        MyService getService() {
            return MyService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBound();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // start foreground
        startForeground(1, makeNotification("Music 1", "Singer A", isPlaying));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int requestCode = intent.getIntExtra("requestCode", -1);
        Message msg = serviceHandler.obtainMessage();
        Bundle bundle = new Bundle();
        switch (requestCode) {
            case -1:
                bundle.putInt("mp3", R.raw.nhac);
                msg.what = -1;
                msg.setData(bundle);
                serviceHandler.sendMessage(msg);
                break;
            case 0:
                msg.what = 0;
                serviceHandler.sendMessage(msg);
                break;
            case 1:
                msg.what = 1;
                serviceHandler.sendMessage(msg);
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public Notification makeNotification(String title, String singer, boolean isPlaying) {
        Intent intentMusic = new Intent(this, MyService.class);
        intentMusic.putExtra("requestCode", isPlaying ? PAUSE_MUSIC_CODE : RESUME_MUSIC_CODE);

        PendingIntent pendingIntent = PendingIntent.getService(
                this,
                0,
                intentMusic,
                PendingIntent.FLAG_MUTABLE
        );

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext(), "CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setWhen(System.currentTimeMillis())  //When the event occurred, now, since noti are stored by time.
                .setContentTitle(title)   //Title message top row.
                .setContentText(singer);
        if (isPlaying) {
            notification.addAction(android.R.drawable.ic_media_pause, "Pause", pendingIntent);
        } else {
            notification.addAction(android.R.drawable.ic_media_play, "Play", pendingIntent);
        }

        return notification.build();
    }

    public void setOnListenDuration(OnListenDuration onListenDuration) {
        this.onListenDuration = onListenDuration;
    }

    interface OnListenDuration {
        void onCurrentDuration(long time);
    }
}
