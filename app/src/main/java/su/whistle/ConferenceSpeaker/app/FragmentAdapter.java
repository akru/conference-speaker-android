package su.whistle.ConferenceSpeaker.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class FragmentAdapter extends FragmentStatePagerAdapter {
    public static final int MAIN_FRAGMENT   = 0;
    public static final int VOTE_FRAGMENT   = 1;
    public static final int SERVER_FRAGMENT = 2;

    public FragmentAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case MAIN_FRAGMENT:
                return new MainFragment();
            case VOTE_FRAGMENT:
                return new VoteFragment();
            case SERVER_FRAGMENT:
                return new ServerFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
