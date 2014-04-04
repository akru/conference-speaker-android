package com.b2kteam.csandroid.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.b2kteam.csandroid.app.Connector.Connector;
import com.b2kteam.csandroid.app.Connector.Discover;
import com.b2kteam.csandroid.app.Transmitter.Transmitter;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity {
    Thread discover = null;
    Thread connector = null;
    Thread transmitter = null;
    Handler connectorCmd = null;
    ArrayList<String> serverList = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    Bundle servers = new Bundle();

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

    protected InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            return;
        }
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        final Bundle userInfo = new Bundle();
        userInfo.putString("name", preferences.getString("prefName", "Unknown"));
        userInfo.putString("company", preferences.getString("prefCompany", "Unknown"));
        userInfo.putString("title", preferences.getString("prefTitle", "Unknown"));

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
                            setNotice("Connected");

                            // Enable buttons
                            ToggleButton btn = (ToggleButton) findViewById(R.id.record_button);
                            btn.setClickable(true);
                            Button vbtn = (Button) findViewById(R.id.vote_button);
                            vbtn.setClickable(true);
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
                    case Connector.VOTE_ACTION:
                        if (result.contains("success"))
                            toast(R.string.toast_vote_success);
                        else
                            toast(R.string.toast_vote_error);
                        break;
                }
            }
        };
        /** Defining the ArrayAdapter to set items to Spinner Widget */
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, serverList);
        /** Find server select spinner */
        Spinner spinner = (Spinner) findViewById(R.id.server_select);
        /** Setting the adapter to the ListView */
        spinner.setAdapter(adapter);
        /** Adding radio buttons for the spinner items */
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Interrupt translation
                if (transmitter != null)
                    transmitter.interrupt();

                // Down record button
                ToggleButton btn = (ToggleButton) findViewById(R.id.record_button);
                btn.setChecked(false);

                String serverName = (String) adapterView.getItemAtPosition(i);
                Bundle server = servers.getBundle(serverName);

                if (connector == null) {
                    // Create connector
                    Connector c = new Connector(connectorHandler);
                    // Create connector thread
                    connector = new Thread(c);
                    connector.start();
                    // Waiting for command handler creation
                    while (c.getCommandHandler() == null);
                    connectorCmd = c.getCommandHandler();
                }
                // Create registration request
                Bundle req = new Bundle();
                req.putInt("action", Connector.REGISTRATION_ACTION);
                req.putBundle("server", server);
                req.putBundle("user", userInfo);

                Message reqMsg = new Message();
                reqMsg.setData(req);
                connectorCmd.sendMessage(reqMsg);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // discover message handler
        Handler discoverHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // Get server info
                Bundle server = msg.getData();
                String serverName = server.getString("name");
                if (!serverList.contains(serverName))
                {
                    servers.putBundle(serverName, server);
                    serverList.add(serverName);
                    adapter.notifyDataSetChanged();
                }
            }
        };

        // start discover thread when not started
        try {
            if (discover == null) {
                discover = new Thread(new Discover(discoverHandler, getBroadcastAddress()));
                discover.start();
                toast(R.string.toast_discovering);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void onClick(View view) {
        ToggleButton btn = (ToggleButton) view;

        Bundle req = new Bundle();
        if (btn.isChecked()) {
            btn.setChecked(false);
            btn.setClickable(false);
            // Create channel request
            toast(R.string.toast_connecting);
            req.putInt("action", Connector.CHANNEL_ACTION);
            Message reqMsg = new Message();
            reqMsg.setData(req);
            connectorCmd.sendMessage(reqMsg);
        }
        else {
            if (transmitter != null)
                transmitter.interrupt();
            req.putInt("action", Connector.CHANNEL_CLOSE_ACTION);
            Message reqMsg = new Message();
            reqMsg.setData(req);
            connectorCmd.sendMessage(reqMsg);
        }
    }

    public void onClickVote(View view) {
        final Bundle req = new Bundle();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.vote_dialog_title);
        builder.setMessage(R.string.vote_dialog_content);

        builder.setPositiveButton(R.string.vote_dialog_yes,
                new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                Message reqMsg = new Message();
                req.putInt("action", Connector.VOTE_ACTION);
                req.putBoolean("type", true);
                reqMsg.setData(req);
                connectorCmd.sendMessage(reqMsg);
                dialog.dismiss();
            }

        });

        builder.setNegativeButton(R.string.vote_dialog_no,
                new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Message reqMsg = new Message();
                req.putInt("action", Connector.VOTE_ACTION);
                req.putBoolean("type", false);
                reqMsg.setData(req);
                connectorCmd.sendMessage(reqMsg);
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
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
        switch (id) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_OK);
                break;
        }
        return true;
    }
}
