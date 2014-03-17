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
    public Transmitter(String address, int port, Handler statusHandler) throws IOException {
        // store status handler
        handler = statusHandler;
        // open socket connection
        socket = new Socket(address, port);
        // open audio recorder
        recorder = new Recorder();
        // down stop flag
        stop = false;
        // emit success init
        emitStatus(TransmitterStatus.CONNECTED);
    }

    public void sendAudioBuffer(ByteBuffer buffer) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(buffer.array());
    }

    public void cancel() {
        stop = true;
    }

    @Override
    public void run() {
        // emit recording status
        emitStatus(TransmitterStatus.RECORDING);
        while (!stop) {
            try {
                sendAudioBuffer(recorder.getAudioData());
            } catch (IOException e) {
                emitStatus(TransmitterStatus.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void emitStatus(TransmitterStatus status) {
        Message msg = new Message();
        msg.obj = status;
        handler.sendMessage(msg);
    }

    private Handler handler;
    private Socket socket;
    private Recorder recorder;
    private volatile boolean stop;
}
