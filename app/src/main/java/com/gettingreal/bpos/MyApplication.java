package com.gettingreal.bpos;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;

import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerStatus;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 7/3/14
 * Time: 7:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyApplication extends Application {

    private static Picasso mPicasso;
    private static LruCache mCache;

    private Handler handler = new Handler();

    private Runnable checkOnlineRunnable = new Runnable() {

        public void run() {
            SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
            String address = settings.getString("address", "192.168.192.168");

            String serverUrl = "http://" + address + "/";

            ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);

            serverAPIClient.getOnline(new Callback<ServerStatus>() {
                @Override
                public void success(ServerStatus aServerStatus, Response aResponse) {
                    Intent intent = new Intent("server-status");
                    intent.putExtra("online", true);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }

                @Override
                public void failure(RetrofitError aRetrofitError) {
                    Intent intent = new Intent("server-status");
                    intent.putExtra("online", false);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                }
            });

            handler.postDelayed(this, 1000);
        }


    };

    public MyApplication() {
        handler.post(checkOnlineRunnable);
    }

    public Picasso getPicasso() {
        if (mPicasso == null) {
            mPicasso = new Picasso.Builder(getApplicationContext()).memoryCache(getCache()).build();
        }
        return mPicasso;
    }

    public LruCache getCache() {
        if (mCache == null) {
            mCache = new LruCache(4000);
        }
        return mCache;
    }
}