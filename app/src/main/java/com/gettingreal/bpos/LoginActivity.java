package com.gettingreal.bpos;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gettingreal.bpos.helper.ServerStatusMenuHelper;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 11:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoginActivity extends Activity implements LoginDialog.LoginDialogListener, InitialServerAddressDialog.InitialServerAddressDialogListener {
    private Menu mMenu;

    private boolean mIsServerOnline = false;
    private BroadcastReceiver mServerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received server-status broadcast");
            mIsServerOnline = intent.getBooleanExtra("online", false);
            ServerStatusMenuHelper.updateServerStatusMenu(mMenu, mIsServerOnline);
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        new CopyAssetsTask(this).execute();

        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        if (!settings.contains("address")) {
            showInitialServerAddressDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServerStatusBroadcastReceiver);

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.status_activity_actions, menu);
        mMenu = menu;
        return true;
    }

    public void showLoginDialog(View v) {
        FragmentManager fm = getFragmentManager();
        LoginDialog loginDialog = new LoginDialog();
        loginDialog.show(fm, "fragment_login_dialog");
    }

    public void showMenu(View v) {
        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("is_admin", false);
        editor.commit();

        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
    }

    public void onFinishLoginDialog(String username, String password) {
        if (username.contentEquals("user") && password.contentEquals("pass")) {
            SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("is_admin", true);
            editor.commit();

            Intent intent = new Intent(this, MenuActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, "Login failed!", Toast.LENGTH_LONG).show();
        }
    }

    public void showInitialServerAddressDialog() {
        FragmentManager fm = getFragmentManager();
        InitialServerAddressDialog initialServerAddressDialog = new InitialServerAddressDialog();
        initialServerAddressDialog.show(fm, "fragment_initial_server_address_dialog");
    }

    @Override
    public void onFinishInitialServerAddressDialog(String address) {
        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("address", address);

        // Commit the edits!
        editor.commit();
    }

    private class CopyAssetsTask extends AsyncTask<Void, Void, Void> {

        private Context mContext;
        private ProgressDialog mProgressDialog;

        private CopyAssetsTask(final Context aContext) {
            mContext = aContext;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage("Copying assets...");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(final Void... aVoids) {
            AssetOpenHelper assetHelper = new AssetOpenHelper(mContext);
            assetHelper.copyAssets();

            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            mProgressDialog.dismiss();
        }
    }
}
