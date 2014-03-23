package com.b2kteam.csandroid.app.Transmitter;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import static java.lang.Thread.currentThread;

/**
 * Created by akru on 16.03.14.
 */
public class Transmitter implements Runnable {
    public Transmitter() {
        // open audio recorder
        recorder = new Recorder();
    }

    public void setChannel(String address, int port) throws IOException {
        // open socket connection
        socket = new Socket(address, port);
    }

    public void sendAudioBuffer(byte [] buffer) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(buffer);
    }

    @Override
    public void run() {
        // start recorder
        recorder.start();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                sendAudioBuffer(recorder.getAudioData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Socket socket;
    private Recorder recorder;
}
