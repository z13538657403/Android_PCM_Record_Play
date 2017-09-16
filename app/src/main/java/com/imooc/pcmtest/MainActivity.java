package com.imooc.pcmtest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    private Button mStartRecordBtn;
    private Button mStopRecordBtn;
    private Button mPlayAudioBtn;
    private Button mStopAudioBtn;

    private AudioUtil mAudioUtil;
    private static final int BUFFER_SIZE = 1024 * 2;
    private byte[] mBuffer;
    private File mAudioFile;
    private ExecutorService mExecutorService;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartRecordBtn = (Button) findViewById(R.id.start_record_button);
        mStopRecordBtn = (Button) findViewById(R.id.stop_record_button);
        mPlayAudioBtn = (Button) findViewById(R.id.play_audio_button);
        mStopAudioBtn = (Button) findViewById(R.id.stop_audio_button);
        mBuffer = new byte[BUFFER_SIZE];
        mAudioFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/record/encode.pcm");
        mExecutorService = Executors.newSingleThreadExecutor();

        mAudioUtil = AudioUtil.getInstance();
        initEvent();
    }

    private void initEvent()
    {
        mStartRecordBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mAudioUtil.startRecord();
                mAudioUtil.recordData();
            }
        });
        mStopRecordBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mAudioUtil.stopRecord();
                mAudioUtil.convertWavFile();
            }
        });

        mPlayAudioBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (mAudioFile != null)
                {
                    mExecutorService.submit(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            playAudio(mAudioFile);
                        }
                    });
                }
            }
        });

        mStopAudioBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
            }
        });
    }

    private void playAudio(File audioFile)
    {
        Log.d("MainActivity" , "lu yin kaishi");
        int streamType = AudioManager.STREAM_MUSIC;
        int simpleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int mode = AudioTrack.MODE_STREAM;

        int minBufferSize = AudioTrack.getMinBufferSize(simpleRate , channelConfig , audioFormat);
        AudioTrack audioTrack = new AudioTrack(streamType , simpleRate , channelConfig , audioFormat ,
                Math.max(minBufferSize , BUFFER_SIZE) , mode);
        audioTrack.play();
        Log.d(TAG , minBufferSize + " is the min buffer size , " + BUFFER_SIZE + " is the read buffer size");

        FileInputStream inputStream = null;
        try
        {
            inputStream = new FileInputStream(audioFile);
            int read;
            while ((read = inputStream.read(mBuffer)) > 0)
            {
                Log.d("MainActivity" , "lu yin kaishi11111");

                audioTrack.write(mBuffer , 0 , read);
            }
        }
        catch (RuntimeException | IOException e)
        {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
}
