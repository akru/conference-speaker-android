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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.net.SocketException;
import java.net.UnknownHostException;

import com.b2kteam.csandroid.app.Connector.Connector;
import com.b2kteam.csandroid.app.Connector.Discover;
import com.b2kteam.csandroid.app.Transmitter.Transmitter;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {
    private Thread discover = null;
    private Thread connector = null;
    private Thread transmitter = null;
    private Handler connectorCmd = null;
    private String serverName;

    protected void toast(int message) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
            final Bundle userInfo = new Bundle();
            userInfo.putString("name", "akru");

            final Handler connectorHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    JSONObject response;
                    String result;
                    try {
                        response = new JSONObject(data.getString("response"));
                        result = response.getString("result");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }
                    switch (data.getInt("action")) {
                        case Connector.REGISTRATION_ACTION:
                            if (result.contains("success")) {
                                toast(R.string.toast_registration_success);
                                setNotice(serverName);
                            }
                            else
                                toast(R.string.toast_registration_error);
                            break;
                        case Connector.CHANNEL_ACTION:
                            // Activate button
                            ToggleButton btn = (ToggleButton) findViewById(R.id.record_button);
                            btn.setClickable(true);

                            if (result.contains("success")) {
                                toast(R.string.toast_channel_success);
                                try {
                                    JSONObject channel = response.getJSONObject("channel");
                                    String host = channel.getString("host");
                                    int port = channel.getInt("port");
                                    // Create transmitter thread
                                    transmitter = new Thread(new Transmitter(host, port));
                                    transmitter.start();
                                    // Mute notice
                                    setNotice(R.string.notice_mute);
                                    // button enabled
                                    btn.setChecked(true);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    return;
                                }
                            }
                            else {
                                toast(R.string.toast_channel_error);
                                // button disabled
                                btn.setChecked(false);
                            }
                            break;
                    }
                }
            };

            // discover message handler
            Handler discoverHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (connector == null) {
                        // Get server info
                        Bundle server = msg.getData();
                        // Create connector
                        Connector c = new Connector(connectorHandler);
                        // Create connector thread
                        connector = new Thread(c);
                        connector.start();
                        // Waiting for command handler creation
                        while (c.getCommandHandler() == null);
                        connectorCmd = c.getCommandHandler();
                        // Create registration request
                        Bundle req = new Bundle();
                        req.putInt("action", Connector.REGISTRATION_ACTION);
                        req.putBundle("server", server);
                        req.putBundle("user", userInfo);
                        // Store server name
                        serverName = server.getString("name");

                        Message reqMsg = new Message();
                        reqMsg.setData(req);
                        connectorCmd.sendMessage(reqMsg);
                    }
                }
            };

            // start discover thread when not started
            try {
                if (discover == null) {
                    discover = new Thread(new Discover(discoverHandler));
                    discover.start();
                    toast(R.string.toast_discovering);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public void onClick(View view) {
        ToggleButton btn = (ToggleButton) view;

        if (btn.isChecked()) {
            btn.setChecked(false);
            btn.setClickable(false);
            // Create channel request
            toast(R.string.toast_connecting);
            Bundle req = new Bundle();
            req.putInt("action", Connector.CHANNEL_ACTION);
            Message reqMsg = new Message();
            reqMsg.setData(req);
            connectorCmd.sendMessage(reqMsg);
        }
        else {
            if (transmitter != null)
                transmitter.interrupt();
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
