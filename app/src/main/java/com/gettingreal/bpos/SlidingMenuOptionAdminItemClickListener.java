package com.gettingreal.bpos;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;

import com.slidinglayer.SlidingLayer;

/**
 * Created by ivanfoong on 1/4/14.
 */
public class SlidingMenuOptionAdminItemClickListener implements AdapterView.OnItemClickListener {
    private SlidingLayer mSlidingLayer;
    private SlidingMenuOptionAdminAdapter.Option mCurrentOption;

    public SlidingMenuOptionAdminItemClickListener(final SlidingLayer aSlidingLayer, final SlidingMenuOptionAdminAdapter.Option aCurrentOption) {
        mSlidingLayer = aSlidingLayer;
        mCurrentOption = aCurrentOption;
    }

    @Override
    public void onItemClick(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
        switch (i) {
            case 0: { // products
                mSlidingLayer.closeLayer(true);
                if (mCurrentOption != SlidingMenuOptionAdminAdapter.Option.Product) {
                    Intent intent = new Intent(mSlidingLayer.getContext(), MenuActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    mSlidingLayer.getContext().startActivity(intent);
                }
                break;
            }
            case 1: { // orders
                mSlidingLayer.closeLayer(true);
                if (mCurrentOption != SlidingMenuOptionAdminAdapter.Option.Order) {
                    Intent intent = new Intent(mSlidingLayer.getContext(), OrdersActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    mSlidingLayer.getContext().startActivity(intent);
                }
                break;
            }
            case 2: { // tables
                mSlidingLayer.closeLayer(true);
                if (mCurrentOption != SlidingMenuOptionAdminAdapter.Option.Table) {
                    Intent intent = new Intent(mSlidingLayer.getContext(), TableActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    mSlidingLayer.getContext().startActivity(intent);
                }
                break;
            }
            case 3: { // reports
                mSlidingLayer.closeLayer(true);
                if (mCurrentOption != SlidingMenuOptionAdminAdapter.Option.Report) {
                    Intent intent = new Intent(mSlidingLayer.getContext(), ReportsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    mSlidingLayer.getContext().startActivity(intent);
                }
                break;
            }
            case 4: { // settings
                mSlidingLayer.closeLayer(true);
                if (mCurrentOption != SlidingMenuOptionAdminAdapter.Option.Setting) {
                    Intent intent = new Intent(mSlidingLayer.getContext(), SettingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    mSlidingLayer.getContext().startActivity(intent);
                }
                break;
            }
            case 5: { // logout
                mSlidingLayer.closeLayer(true);

                SharedPreferences settings = mSlidingLayer.getContext().getSharedPreferences(mSlidingLayer.getContext().getPackageName(), 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("is_admin", false);
                editor.commit();

                Intent intent  = new Intent(mSlidingLayer.getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mSlidingLayer.getContext().startActivity(intent);

                Activity host = (Activity)mSlidingLayer.getContext();
                host.finish();

                break;
            }
            default: {
                mSlidingLayer.closeLayer(true);
                break;
            }
        }
    }
}
