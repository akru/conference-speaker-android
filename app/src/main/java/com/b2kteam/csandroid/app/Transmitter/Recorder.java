package com.b2kteam.csandroid.app.Transmitter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.nio.ByteBuffer;

/**
 * Created by akru on 13.03.14.
 */
public class Recorder {

    public Recorder() {
        // get minimal buffer size
        bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        // open microphone recorder
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        // start recording
        recorder.startRecording();
    }

    public ByteBuffer getAudioData() {
        // allocate new buffer
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        // read audio data
        recorder.read(buffer, bufferSize);
        // return buffer
        return buffer;
    }

    private int bufferSize;
    private AudioRecord recorder;
}
