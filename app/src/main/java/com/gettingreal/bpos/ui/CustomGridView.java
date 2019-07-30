package com.gettingreal.bpos.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

import com.gettingreal.bpos.TableAdapter;

/**
 * Created by ivanfoong on 25/6/14.
 */
public class CustomGridView extends GridView {
    public CustomGridView(final Context context) {
        super(context);
    }

    public CustomGridView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomGridView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(changed) {
            TableAdapter adapter = (TableAdapter)getAdapter();

            int numColumns = 6;

            if(numColumns > 1) {
                int columnWidth = getMeasuredWidth() / numColumns;
                adapter.measureItems(columnWidth);
            }
        }
        super.onLayout(changed, l, t, r, b);
    }
}
