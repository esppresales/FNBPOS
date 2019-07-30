package com.gettingreal.bpos;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.gettingreal.bpos.helper.ServerStatusMenuHelper;
import com.slidinglayer.SlidingLayer;

/**
 * Created by ivanfoong on 1/7/14.
 */
public class ReportsActivity extends Activity {
    private SlidingLayer mSlidingLayer;
    private SlidingMenuOptionAdminAdapter mSlidingMenuOptionAdapter;
    private SlidingMenuOptionAdminItemClickListener mSlidingMenuOptionItemClickListener;
    private ListView mOptionListView;

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
        setContentView(R.layout.activity_reports);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mSlidingLayer = (SlidingLayer) findViewById(R.id.slidingLayer);
        mOptionListView = (ListView) findViewById(R.id.listview_options);
        mSlidingMenuOptionAdapter = new SlidingMenuOptionAdminAdapter(this);
        mOptionListView.setAdapter(mSlidingMenuOptionAdapter);
        mSlidingMenuOptionItemClickListener = new SlidingMenuOptionAdminItemClickListener(mSlidingLayer, SlidingMenuOptionAdminAdapter.Option.Report);
        mOptionListView.setOnItemClickListener(mSlidingMenuOptionItemClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServerStatusBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.status_activity_actions, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mSlidingLayer.isOpened()) {
                    mSlidingLayer.closeLayer(true);
                } else {
                    mSlidingLayer.openLayer(true);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void dismissSlidingLayer(View v) {
        mSlidingLayer.closeLayer(true);
    }
}