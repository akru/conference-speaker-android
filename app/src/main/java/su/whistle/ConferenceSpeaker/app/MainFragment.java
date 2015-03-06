package su.whistle.ConferenceSpeaker.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

enum ConnectedState {
    DISCONNECTED,
    CONNECTED,
    HAND_UP,
    VOICE
}

interface StateChangedListener {
    void onStateChanged(ConnectedState s);
}

interface MainInteraction {
    ConnectedState getConnectedState();
    void setStateChangedListener(StateChangedListener l);
    void channelRequest();
    void channelClose();
}

public class MainFragment extends Fragment implements View.OnClickListener, StateChangedListener {
    MainInteraction interaction;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        interaction = (MainInteraction) activity;
        interaction.setStateChangedListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    void setNotice(int message) {
        TextView notice = (TextView) getView().findViewById(R.id.hint_text);
        notice.setText(message);
    }

    void updateViews() {
        ImageView recordButton = (ImageView) getView().findViewById(R.id.record_button);
        ImageView waveImage = (ImageView) getView().findViewById(R.id.wave_image);
        recordButton.setOnClickListener(this);

        switch (interaction.getConnectedState()) {
            case DISCONNECTED:
                setNotice(R.string.notice_disconnected);
                recordButton.setImageResource(R.drawable.mic_on);
                waveImage.setImageResource(R.drawable.waves_red);
                break;
            case CONNECTED:
                setNotice(R.string.notice_connected);
                recordButton.setImageResource(R.drawable.mic_on);
                waveImage.setImageResource(R.drawable.waves_red);
                break;
            case HAND_UP:
                setNotice(R.string.notice_hand_up);
                recordButton.setImageResource(R.drawable.hand_up);
                waveImage.setImageResource(R.drawable.waves_blue);
                break;
            case VOICE:
                setNotice(R.string.notice_mute);
                recordButton.setImageResource(R.drawable.mic_off);
                waveImage.setImageResource(R.drawable.waves_green);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateViews();
    }

    @Override
    public void onClick(View v) {
        switch (interaction.getConnectedState()) {
            case CONNECTED:
                interaction.channelRequest();
                break;

            case HAND_UP: // Cancel channel request
            case VOICE:   // Close channel request
                interaction.channelClose();
                break;
        }
    }

    @Override
    public void onStateChanged(ConnectedState s) {
        Log.i("new state", "reached");
        if (getView() != null)
            updateViews();
    }
}