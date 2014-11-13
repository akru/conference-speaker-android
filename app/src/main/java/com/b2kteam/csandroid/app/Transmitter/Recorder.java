package com.b2kteam.csandroid.app.Transmitter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by akru on 13.03.14.
 */
public class Recorder {

    public Recorder() {
        // get minimal buffer size
        bufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        // open microphone recorder
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        // allocate new buffer
        buffer = new byte[bufferSize];
    }

    public void start() {
        // start recording
        recorder.startRecording();
    }

    public void stop() {
        recorder.stop();
        recorder.release();
    }

    public byte [] getAudioData() {
        // read audio data
        recorder.read(buffer, 0, bufferSize);
        // dummy resampler 44100 -> 22050
        byte [] b = new byte[bufferSize / 2];
        for (int i = 0; i < bufferSize; i = i + 4) {
            b[i / 2]     = buffer[i];
            b[i / 2 + 1] = buffer[i+1];
        }
        // return buffer
        return b;
    }

    private byte [] buffer;
    private int bufferSize;
    private AudioRecord recorder;
}
