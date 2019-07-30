package com.gettingreal.bpos;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;

import com.slidinglayer.SlidingLayer;

/**
 * Created by ivanfoong on 18/6/14.
 */
public class SlidingMenuOptionItemClickListener implements AdapterView.OnItemClickListener {
    private SlidingLayer mSlidingLayer;
    private SlidingMenuOptionAdapter.Option mCurrentOption;

    public SlidingMenuOptionItemClickListener(final SlidingLayer aSlidingLayer, final SlidingMenuOptionAdapter.Option aCurrentOption) {
        mSlidingLayer = aSlidingLayer;
        mCurrentOption = aCurrentOption;
    }

    @Override
    public void onItemClick(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
        switch (i) {
            case 0: { // logout
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
