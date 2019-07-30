package com.gettingreal.bpos;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSTable;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 30/6/14.
 */
public class GridViewItemLayout extends LinearLayout {

    // Array of max cell heights for each row
    private static int[] mMaxRowHeight;

    // The number of columns in the grid view
    private static int mNumColumns;

    // The position of the view cell
    private int mPosition;

    // Public constructor
    public GridViewItemLayout(Context context) {
        super(context);
    }

    // Public constructor
    public GridViewItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Set the position of the view cell
     *
     * @param position
     */
    public void setPosition(int position) {
        mPosition = position;
    }

    /**
     * Set the number of columns and item count in order to accurately store the
     * max height for each row. This must be called whenever there is a change to the layout
     * or content data.
     *
     * @param numColumns
     * @param itemCount
     */
    public static void initItemLayout(int numColumns, int itemCount) {
        mNumColumns = numColumns;
        mMaxRowHeight = new int[itemCount];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Do not calculate max height if column count is only one
        if (mNumColumns <= 1 || mMaxRowHeight == null) {
            return;
        }

        // Get the current view cell index for the grid row
        int rowIndex = mPosition / mNumColumns;
        // Get the measured height for this layout
        int measuredHeight = getMeasuredHeight();
        // If the current height is larger than previous measurements, update the array
        if (measuredHeight > mMaxRowHeight[rowIndex]) {
            mMaxRowHeight[rowIndex] = measuredHeight;
        }
        // Update the dimensions of the layout to reflect the max height
        setMeasuredDimension(getMeasuredWidth(), mMaxRowHeight[rowIndex]);
    }

    public void updateItemDisplay(POSTable aTable, ArrayList<POSOrder> aOrders) {
        final TextView tableNameTextView = (TextView)findViewById(R.id.txt_table_name);
        final ImageView tableStatusImageView = (ImageView)findViewById(R.id.img_status);
        final LinearLayout ordersContainer = (LinearLayout)findViewById(R.id.container_orders);

        tableNameTextView.setText(aTable.getName());
        ordersContainer.removeAllViews();
        if (aOrders != null && aOrders.size() > 0) {
            ordersContainer.setVisibility(View.VISIBLE);
            tableStatusImageView.setImageResource(R.drawable.ic_table_occupied);

            for (POSOrder order : aOrders) {
                LinearLayout container = new LinearLayout(ordersContainer.getContext());
                container.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                container.setOrientation(LinearLayout.VERTICAL);

                TextView orderIdTextView = new TextView(container.getContext());
                orderIdTextView.setText("ORDER #" + String.valueOf(order.getId()));
                container.addView(orderIdTextView);

                TextView orderStatusTextView = new TextView(container.getContext());
                boolean hasUndeliveredItems = false;
                for (POSOrderItem orderItem : order.getOrderItems()) {
                    if (orderItem.getQuantityServed() < orderItem.getQuantityOrdered()) {
                        hasUndeliveredItems = true;
                        break;
                    }
                }
                orderStatusTextView.setText(hasUndeliveredItems?"Pending Delivery":"Pending Payment");
                container.addView(orderStatusTextView);

                ordersContainer.addView(container);
            }
        }
        else {
            ordersContainer.setVisibility(View.GONE);
            tableStatusImageView.setImageResource(R.drawable.ic_table_available);
        }
    }
}
