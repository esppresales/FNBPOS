package com.gettingreal.bpos;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.gettingreal.bpos.helper.ServerStatusMenuHelper;
import com.gettingreal.bpos.model.POSCategory;
import com.slidinglayer.SlidingLayer;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 12:16 PM
 * To change this template use File | Settings | File Templates.
 */

public class MenuActivity extends Activity implements ActionBar.OnNavigationListener {

    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    ArrayList<POSCategory> mCategories;
    private View mCheckoutOverlay;
    private BroadcastReceiver mCheckoutBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received checkout broadcast");
            // Extract data included in the Intent
//            String message = intent.getStringExtra("message");
//            Log.d("receiver", "Got message: " + message);
            if (mCheckoutOverlay != null) {
                mCheckoutOverlay.setVisibility(View.VISIBLE);
            }
        }
    };
    private BroadcastReceiver mCheckoutCompletedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received checkout-complete broadcast");
            if (mCheckoutOverlay != null) {
                mCheckoutOverlay.setVisibility(View.INVISIBLE);
            }

            showOrderSentDialog();
        }
    };
    private ListView mOptionListView;
    private AdapterView.OnItemClickListener mSlidingMenuOptionItemClickListener;
    private SlidingLayer mSlidingLayer;
    private BaseAdapter mSlidingMenuOptionAdapter;
    private String[] mCategoryNames;
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
        setContentView(R.layout.activity_menu);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setHomeButtonEnabled(true);

        mCheckoutOverlay = findViewById(R.id.layout_checkout);

        mCheckoutOverlay.setVisibility(View.INVISIBLE);

        mSlidingLayer = (SlidingLayer) findViewById(R.id.slidingLayer);

        mOptionListView = (ListView) findViewById(R.id.listview_options);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
            .getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.status_activity_actions, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mCheckoutBroadcastReceiver,
            new IntentFilter("checkout"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mCheckoutCompletedBroadcastReceiver,
            new IntentFilter("checkout-completed"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));

        mSlidingLayer.closeLayer(false);
        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        if (settings.getBoolean("is_admin", false)) {
            mSlidingMenuOptionAdapter = new SlidingMenuOptionAdminAdapter(this);
            mSlidingMenuOptionItemClickListener = new SlidingMenuOptionAdminItemClickListener(mSlidingLayer, SlidingMenuOptionAdminAdapter.Option.Product);
        }
        else {
            mSlidingMenuOptionAdapter = new SlidingMenuOptionAdapter(this);
            mSlidingMenuOptionItemClickListener = new SlidingMenuOptionItemClickListener(mSlidingLayer, SlidingMenuOptionAdapter.Option.Product);
        }

        mOptionListView.setAdapter(mSlidingMenuOptionAdapter);
        mOptionListView.setOnItemClickListener(mSlidingMenuOptionItemClickListener);

        mCategories = POSCategory.getAllEnabledCategories(this);
        mCategoryNames = new String[mCategories.size() + 1];

        mCategoryNames[0] = "ALL PRODUCTS";
        for (int i = 0; i < mCategories.size(); i++) {
            mCategoryNames[i + 1] = mCategories.get(i).getName();
        }

        // update category filter list
        final ActionBar actionBar = getActionBar();

        // Specify a SpinnerAdapter to populate the dropdown list.
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
                R.layout.spinner_category_item, android.R.id.text1,
                mCategoryNames);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set up the dropdown list navigation in the action bar.
        actionBar.setListNavigationCallbacks(adapter, this);
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCheckoutBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCheckoutCompletedBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServerStatusBroadcastReceiver);

        super.onPause();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        /*
        Fragment fragment = new DummySectionFragment();
        Bundle args = new Bundle();
        args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
        fragment.setArguments(args);
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        */

        if (position > 0) {
            POSCategory selectedCategory = mCategories.get(position - 1);

            SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("category_uid", selectedCategory.getUid());
            editor.commit();
        } else { // ALL PRODUCTS
            SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.remove("category_uid");
            editor.commit();
        }

        Intent intent = new Intent("changed_category");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

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

    public void dismissCheckout(View v) {
        mCheckoutOverlay.setVisibility(View.INVISIBLE);
    }

    public void dismissSlidingLayer(View v) {
        mSlidingLayer.closeLayer(true);
    }


    public void showOrderSentDialog() {
        FragmentManager fm = getFragmentManager();
        OrderSentDialog orderSentDialog = new OrderSentDialog();
        orderSentDialog.show(fm, "fragment_order_sent_dialog");
//        finish();
//        startActivity(getIntent());
    }
}
