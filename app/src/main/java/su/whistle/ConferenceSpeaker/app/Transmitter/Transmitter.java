package su.whistle.ConferenceSpeaker.app.Transmitter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by akru on 16.03.14.
 */
public class Transmitter implements Runnable {
    public Transmitter(String serverAddress, int serverPort) {
        // open audio recorder
        recorder = new Recorder();
        // Get server address and port
        address = serverAddress;
        port = serverPort;
    }

    public void sendAudioBuffer(byte [] buffer) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.send(packet);
    }

    @Override
    public void run() {
        // start recorder
        recorder.start();
        try {
            socket = new DatagramSocket();
            socket.connect(InetAddress.getByName(address), port);
            while (!Thread.currentThread().isInterrupted()) {
                sendAudioBuffer(recorder.getAudioData());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            recorder.stop();
        }
    }

    private String address;
    private int port;
    private DatagramSocket socket;
    private Recorder recorder;
}
