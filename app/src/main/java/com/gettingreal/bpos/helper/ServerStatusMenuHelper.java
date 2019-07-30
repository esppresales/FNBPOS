package com.gettingreal.bpos.helper;

import android.view.Menu;
import android.view.MenuItem;

import com.gettingreal.bpos.R;

/**
 * Created by ivanfoong on 11/8/14.
 */
public class ServerStatusMenuHelper {
    public static void updateServerStatusMenu(Menu aMenu, boolean aOnline) {
        if (aMenu != null) {
            MenuItem statusMenuItem = aMenu.findItem(R.id.action_status);

            if (aOnline) {
                statusMenuItem.setTitle("Online");
            }
            else {
                statusMenuItem.setTitle("Offline");
            }
        }
    }
}
