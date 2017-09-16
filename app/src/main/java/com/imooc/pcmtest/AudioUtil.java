package com.imooc.pcmtest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by zhangtao on 17/8/27.
 */

public class AudioUtil
{
    private static AudioUtil mInstance;
    private AudioRecord recorder;
    //声音源
    private static int audioSource = MediaRecorder.AudioSource.MIC;
    //录音的采样频率
    private static int audioRate = 44100;
    //录音的声道，单声道
    private static int audioChannel = AudioFormat.CHANNEL_IN_MONO;
    //量化的精度
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //缓存的大小
    private static int bufferSize = AudioRecord.getMinBufferSize(audioRate , audioChannel , audioFormat);
    //记录播放状态
    private boolean isRecording = false;
    //数字信号数组
    private byte[] noteArray;
    //PCM文件
    private File pcmFile;
    //wav文件
    private File wavFile;
    //文件输出流
    private OutputStream os;
    //文件根目录
    private String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record/";
    //wav文件目录
    private String outFileName = basePath + "/encode.wav";
    //pcm文件目录
    private String inFileName = basePath + "/encode.pcm";

    private AudioUtil()
    {
        //创建文件
        createFile();
        recorder = new AudioRecord(audioSource , audioRate ,
                audioChannel , audioFormat , bufferSize);
    }

    //创建文件夹,首先创建目录，然后创建对应的文件
    private void createFile()
    {
        File baseFile = new File(basePath);
        if (!baseFile.exists())
            baseFile.mkdirs();
        pcmFile = new File(basePath + "/encode.pcm");
        wavFile = new File(basePath + "/encode.pcm");

        if (pcmFile.exists())
            pcmFile.delete();
        if (wavFile.exists())
            wavFile.delete();

        try
        {
            pcmFile.createNewFile();
            wavFile.createNewFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public synchronized static AudioUtil getInstance()
    {
        if (mInstance == null)
        {
            mInstance = new AudioUtil();
        }
        return mInstance;
    }

    //读取录音数字数据线程
    class WriteThread implements Runnable
    {
        @Override
        public void run()
        {
            writeData();
        }
    }

    //录音线程执行体
    private void writeData()
    {
        noteArray = new byte[bufferSize];
        //建立文件输出流
        try
        {
            os = new BufferedOutputStream(new FileOutputStream(pcmFile));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        while (isRecording)
        {
            int recordSize = recorder.read(noteArray , 0 , bufferSize);
            if (recordSize > 0)
            {
                try
                {
                    os.write(noteArray);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        if (os != null)
        {
            try
            {
                os.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    //开始录音
    public void startRecord()
    {
        isRecording = true;
        recorder.startRecording();
    }

    //记录数据
    public void recordData()
    {
        new Thread(new WriteThread()).start();
    }

    //停止录音
    public void stopRecord()
    {
        if (recorder != null)
        {
            isRecording = false;
            recorder.stop();
            recorder.release();
        }
    }

    //将pcm文件转换为wav文件
    public void convertWavFile()
    {
        FileInputStream in = null;
        FileOutputStream out = null;

        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = AudioUtil.audioRate;
        int channels = 1;
        long byteRate = 16 * AudioUtil.audioRate * channels / 8;
        byte[] data = new byte[bufferSize];

        try
        {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);
            totalAudioLen = in.getChannel().size();
            //由于不包括RIFF和WAV
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out , totalAudioLen , totalDataLen , longSampleRate , channels , byteRate);
            while (in.read(data) != -1)
            {
                out.write(data);
            }

            in.close();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen,
                                     long longSampleRate, int channels, long byteRate) throws IOException
    {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样位数/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        //数据块的调整数（按字节算的），其值为通道数×每样本的数据位值／8
        header[32] = (byte) (16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        //语音数据大小
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
