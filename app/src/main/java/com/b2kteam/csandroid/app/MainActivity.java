package com.b2kteam.csandroid.app;

import android.os.Message;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.b2kteam.csandroid.app.Connector.Connector;
import com.b2kteam.csandroid.app.Connector.Discover;
import com.b2kteam.csandroid.app.Connector.ServerInfo;
import com.b2kteam.csandroid.app.Connector.UserInfo;
import com.b2kteam.csandroid.app.Transmitter.Transmitter;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {
    private Thread discover = null;
    private UserInfo userInfo = new UserInfo("akru");
    private Connector connector = new Connector();
    private Thread transmitter = null;

    protected void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    protected void setNotice(String message) {
        TextView notice = (TextView) findViewById(R.id.hint_text);
        notice.setText(message);
    }

    protected void setNotice(int message) {
        TextView notice = (TextView) findViewById(R.id.hint_text);
        notice.setText(message);
    }

    protected boolean startTransmit() {
        if (!connector.isConnected()) {
            toast("No server connected");
            return false;
        }

        try {
            JSONObject chan = connector.doChannelRequest();
            int port = chan.getJSONObject("channel").getInt("port");
            String host = chan.getJSONObject("channel").getString("host");

            Transmitter t = new Transmitter();
            t.setChannel(host, port);

            transmitter = new Thread(t);
            transmitter.start();

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected void stopTransmit() {
        if (transmitter.isAlive())
            transmitter.interrupt();
        toast("Translation terminated");
    }

    protected void connectToServer(final ServerInfo serverInfo) {
        setNotice(R.string.connecting);

        final Handler toastHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle msgData = msg.getData();
                toast(msgData.getString("toast"));
                if (msgData.getBoolean("result")) {
                    setNotice(serverInfo.getName());
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Message msg = new Message();
                    Bundle data = new Bundle();

                    connector.setServer(serverInfo);
                    JSONObject res = connector.doRegistrationRequest(userInfo);
                    String result = res.getString("result");

                    if (result == "error") {
                        data.putString("toast", res.getString("message"));
                        data.putBoolean("result", false);
                    }
                    else {
                        data.putString("toast","Registration successful on " + serverInfo.getName());
                        data.putBoolean("result", true);
                    }
                    msg.setData(data);
                    toastHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();


            // discover message handler
            Handler discoverHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (!connector.isConnected()) {
                        Bundle server = msg.getData();
                        ServerInfo serverInfo = new ServerInfo(server.getString("name"),
                                server.getString("address"), server.getInt("port"));
                        connectToServer(serverInfo);
                    }
                }
            };

            // start discover thread when not started
            try {
                if (discover == null) {
                    discover = new Thread(new Discover(discoverHandler));
                    discover.start();
                    toast("Discovering the server...");
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }

    }

    public void onClick(View view) {
        final ToggleButton btn = (ToggleButton) view;

        final Handler resHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                if (data.getBoolean("result"))
                    setNotice(R.string.mute_notice);
                else
                    btn.setChecked(false);
            }
        };

        if (btn.isChecked()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Message msg = new Message();
                    Bundle data = new Bundle();

                    if (!startTransmit())
                        data.putBoolean("result", false);
                    else
                        data.putBoolean("result", true);

                    msg.setData(data);
                    resHandler.sendMessage(msg);
                }
            }).start();
        }
        else {
            stopTransmit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_microphone, container, false);
            return rootView;
        }
    }
}
