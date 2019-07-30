package com.gettingreal.bpos;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ivanfoong on 1/4/14.
 */
public class SettingHelper {
    public static final String PREFS_NAME = "settings";

    public static String getLastTableUid(final Context aContext) {
        SharedPreferences settings = aContext.getSharedPreferences(PREFS_NAME, 0);
        return settings.getString("last_table_uid", null);
    }

    public static void setLastTableUid(final Context aContext, final String aLastTableUid) {
        SharedPreferences settings = aContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("last_table_uid", aLastTableUid);
        editor.commit();
    }

    public static void removeLastTableUid(final Context aContext) {
        SharedPreferences settings = aContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("last_table_uid");
        editor.commit();
    }

    public static boolean isPrintingEnabled(final Context aContext) {
        return true;
    }
}
