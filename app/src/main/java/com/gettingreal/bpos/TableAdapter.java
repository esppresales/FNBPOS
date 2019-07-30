package com.gettingreal.bpos;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerOrder;
import com.gettingreal.bpos.api.ServerOrderItem;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by ivanfoong on 30/6/14.
 */
public class TableAdapter extends ArrayAdapter<POSTable> {
    private Context mContext;
    private ArrayList<POSTable> mTables;
    private Hashtable<String, ArrayList<POSOrder>> mOrdersByTableUid;

    // Constructor
    public TableAdapter(Context context, ArrayList<POSTable> aPOSTables) {
        super(context, R.layout.table_item, aPOSTables);
        mContext = context;
        mTables = aPOSTables;
        mOrdersByTableUid = new Hashtable<String, ArrayList<POSOrder>>();
        GridViewItemLayout.initItemLayout(6, getCount());
    }

    @Override
    public int getCount() {
        return mTables.size();
    }

    @Override
    public POSTable getItem(int position) {
        return mTables.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Obtain system inflater
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate temp layout object for measuring
        GridViewItemLayout itemView = (GridViewItemLayout)inflater.inflate(R.layout.table_item, null);

        POSTable table = mTables.get(position);

        // Set position and data
        itemView.setPosition(position);
        itemView.updateItemDisplay(table, mOrdersByTableUid.get(table.getUid()));

        return itemView;
    }

    public void measureItems(int columnWidth) {
        // Obtain system inflater
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate temp layout object for measuring
        GridViewItemLayout itemView = (GridViewItemLayout)inflater.inflate(R.layout.table_item, null);

        // Create measuring specs
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(columnWidth, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        // Loop through each data object
        for(int index = 0; index < mTables.size(); index++) {
            POSTable table = mTables.get(index);

            // Set position and data
            itemView.setPosition(index);
            itemView.updateItemDisplay(table, mOrdersByTableUid.get(table.getUid()));

            // Force measuring
            itemView.requestLayout();
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void syncOrders() {
        SharedPreferences settings = mContext.getSharedPreferences(mContext.getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");

        String serverUrl = "http://" + address + "/";

        ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);

        serverAPIClient.getOpenOrders(new Callback<Collection<ServerOrder>>() {
            @Override
            public void success(final Collection<ServerOrder> aServerOrders, final Response aResponse) {
                mOrdersByTableUid.clear();
                for (ServerOrder serverOrder : aServerOrders) {
                    String tableUid = serverOrder.table_uid;
                    if (tableUid != null) {
                        if (!mOrdersByTableUid.containsKey(tableUid)) {
                            mOrdersByTableUid.put(tableUid, new ArrayList<POSOrder>());
                        }

                        ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                        for (ServerOrderItem serverOrderItem : serverOrder.order_items) {
                            POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                            orderItems.add(orderItem);
                        }
                        POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                        ArrayList<POSOrder> orders = mOrdersByTableUid.get(tableUid);
                        orders.add(order);
                    }
                }

                notifyDataSetChanged();
            }

            @Override
            public void failure(final RetrofitError aRetrofitError) {
                aRetrofitError.printStackTrace();
            }
        });
    }
}