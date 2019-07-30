package com.gettingreal.bpos;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;

import com.gettingreal.bpos.helper.ServerStatusMenuHelper;
import com.gettingreal.bpos.model.POSTable;
import com.slidinglayer.SlidingLayer;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 4/6/14.
 */
public class TableActivity extends Activity {
    private SlidingMenuOptionAdminItemClickListener mSlidingMenuOptionItemClickListener;
    private ListView mOptionListView;
    private SlidingLayer mSlidingLayer;
    private SlidingMenuOptionAdminAdapter mSlidingMenuOptionAdapter;
    private GridView mGridView;
    private TableAdapter mTableAdapter;

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
        setContentView(R.layout.activity_table);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mGridView = (GridView) findViewById(R.id.gridview_table);

        ArrayList<POSTable> tables = POSTable.getAllTables(this);

        mTableAdapter = new TableAdapter(this, tables);
        mGridView.setAdapter(mTableAdapter);

        mSlidingLayer = (SlidingLayer) findViewById(R.id.slidingLayer);
        mSlidingLayer.closeLayer(false);

        mOptionListView = (ListView) findViewById(R.id.listview_options);
        mSlidingMenuOptionAdapter = new SlidingMenuOptionAdminAdapter(this);
        mOptionListView.setAdapter(mSlidingMenuOptionAdapter);
        mSlidingMenuOptionItemClickListener = new SlidingMenuOptionAdminItemClickListener(mSlidingLayer, SlidingMenuOptionAdminAdapter.Option.Table);
        mOptionListView.setOnItemClickListener(mSlidingMenuOptionItemClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mTableAdapter != null) {
            mTableAdapter.syncOrders();
        }

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
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.table_activity_actions, menu);

        mMenu = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (mSlidingLayer.isOpened()) {
                    mSlidingLayer.closeLayer(true);
                } else {
                    mSlidingLayer.openLayer(true);
                }
                return true;
            }
            case R.id.action_add: {
                showAddTableDialog();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void dismissSlidingLayer(View v) {
        mSlidingLayer.closeLayer(true);
    }

    private void showAddTableDialog() {
        FragmentManager fm = getFragmentManager();
        TableAddDialog tableAddDialog = new TableAddDialog();
        tableAddDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                ArrayList<POSTable> tables = POSTable.getAllTables(TableActivity.this);

                mTableAdapter = new TableAdapter(TableActivity.this, tables);
                mGridView.setAdapter(mTableAdapter);

                mTableAdapter.syncOrders();
            }
        });
        tableAddDialog.show(fm, "fragment_table_add");
    }
}
