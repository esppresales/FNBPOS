package com.gettingreal.bpos;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Created by ivanfoong on 18/6/14.
 */
public class SlidingMenuOptionAdapter extends BaseAdapter {
    public static final String OPTION_PRODUCTS = "PRODUCTS";
    public static final String OPTION_LOGOUT = "LOG OUT";
    private Context mContext;

    public enum Option {
        Product
    }

    // Constructor
    public SlidingMenuOptionAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public Object getItem(int position) {
        switch (position) {
            case 0: {
                return OPTION_LOGOUT;
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String item = (String) getItem(position);

        Integer imageResourceId = null;
        if (item == OPTION_PRODUCTS) {
            imageResourceId = R.drawable.ic_menu_products;
        } else if (item == OPTION_LOGOUT) {
            imageResourceId = R.drawable.ic_menu_logout;
        }

        ImageView imageView = null;
        if (convertView != null) {
            imageView = (ImageView) convertView;
        }
        if (imageView == null) {
            imageView = new ImageView(mContext);
            imageView.setBackgroundColor(Color.TRANSPARENT);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(10, 10, 10, 10);
            imageView.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
        }

        if (imageResourceId == null) {
            return null;
        }

        imageView.setImageResource(imageResourceId);
        imageView.setTag(item);

        return imageView;
    }
}
