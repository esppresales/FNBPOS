package com.gettingreal.bpos;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gettingreal.bpos.helper.ServerStatusMenuHelper;
import com.slidinglayer.SlidingLayer;

/**
 * Created by ivanfoong on 24/3/14.
 */
public class SettingActivity extends Activity {

    ListView mSettingsListView;
    SettingAdapter mSettingAdapter;
    private SlidingLayer mSlidingLayer;
    private ListView mOptionListView;
    private SlidingMenuOptionAdminAdapter mSlidingMenuOptionAdapter;
    private SlidingMenuOptionAdminItemClickListener mSlidingMenuOptionItemClickListener;

    private PrinterManagementFragment mPrinterManagementFragment;
    private ProductManagementFragment mProductManagementFragment;
    private CategoryManagementFragment mCategoryManagementFragment;
    private SyncManagementFragment mSyncManagementFragment;
    private ReceiptHeaderManagementFragment mReceiptHeaderManagementFragment;
    private SurchargeManagementFragment mSurchargeManagementFragment;

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
        setContentView(R.layout.activity_settings);

        mSettingsListView = (ListView) findViewById(R.id.list_view_settings);
        mSettingAdapter = new SettingAdapter(this);
        mSettingsListView.setAdapter(mSettingAdapter);
        mSettingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
                Fragment fragment = null;

                String value = (String) mSettingAdapter.getItem(i);

                if (value.equals("PRINTER MANAGEMENT")) {
                    if (mPrinterManagementFragment == null) {
                        mPrinterManagementFragment = new PrinterManagementFragment();
                    }
                    fragment = mPrinterManagementFragment;
                } else if (value.equals("PRODUCTS")) {
                    if (mProductManagementFragment == null) {
                        mProductManagementFragment = new ProductManagementFragment();
                    }
                    fragment = mProductManagementFragment;
                } else if (value.equals("CATEGORIES")) {
                    if (mCategoryManagementFragment == null) {
                        mCategoryManagementFragment = new CategoryManagementFragment();
                    }
                    fragment = mCategoryManagementFragment;
                } else if (value.equals("SYNC DATA")) {
                    if (mSyncManagementFragment == null) {
                        mSyncManagementFragment = new SyncManagementFragment();
                    }
                    fragment = mSyncManagementFragment;
                } else if (value.equals("RECEIPT HEADERS")) {
                    if (mReceiptHeaderManagementFragment == null) {
                        mReceiptHeaderManagementFragment = new ReceiptHeaderManagementFragment();
                    }
                    fragment = mReceiptHeaderManagementFragment;
                } else if (value.equals("SURCHARGES")) {
                    if (mSurchargeManagementFragment == null) {
                        mSurchargeManagementFragment = new SurchargeManagementFragment();
                    }
                    fragment = mSurchargeManagementFragment;
                } else {
                    Toast.makeText(SettingActivity.this, "Not implemented yet!", Toast.LENGTH_LONG).show();
                }

                if (fragment != null) {
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                    transaction.replace(R.id.layout_content, fragment);
                    transaction.commit();
                }
            }
        });

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            actionBar.setTitle(getString(R.string.app_name) + " " + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mSlidingLayer = (SlidingLayer) findViewById(R.id.slidingLayer);
        mSlidingLayer.closeLayer(false);

        mOptionListView = (ListView) findViewById(R.id.listview_options);
        mSlidingMenuOptionAdapter = new SlidingMenuOptionAdminAdapter(this);
        mOptionListView.setAdapter(mSlidingMenuOptionAdapter);
        mSlidingMenuOptionItemClickListener = new SlidingMenuOptionAdminItemClickListener(mSlidingLayer, SlidingMenuOptionAdminAdapter.Option.Setting);
        mOptionListView.setOnItemClickListener(mSlidingMenuOptionItemClickListener);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        transaction.replace(R.id.layout_content, new CategoryManagementFragment());
        transaction.commit();
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

    public class SettingAdapter extends BaseAdapter {

        final String[] settings = new String[]{"CATEGORIES", "PRODUCTS", "SURCHARGES", "SYNC DATA", "PRINTER MANAGEMENT", "RECEIPT HEADERS"};
        Context mContext;

        public SettingAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return settings.length;
        }

        @Override
        public Object getItem(int i) {
            return settings[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = View.inflate(mContext, R.layout.setting_item, null);
            }

            TextView textView = (TextView) view.findViewById(R.id.text_view);
            String value = settings[i];
            textView.setText(value);

            return view;
        }
    }
}
