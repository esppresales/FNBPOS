package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerOrder;
import com.gettingreal.bpos.api.ServerOrderItem;
import com.gettingreal.bpos.helper.PaymentHelper;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSTable;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.Server;
import retrofit.client.Response;

/**
 * Created by ivanfoong on 31/3/14.
 */
public class OrderListFragment extends Fragment {
    private ToggleButton mOpenButton, mClosedButton, mAllButton, mUndeliveredButton, mUnpaidButton;
    private Spinner mFilterSpinner;
    private ListView mListView;
    private OrderListAdapter mOrderListAdapter;
    private TableNumberFilterAdapter mTableNumberFilterAdapter;
    private View mSubheaderLayout;
    private ServerAPIClient mServerAPIClient;
    private ToggleButton mLastButton;
    private int lastClickItemIndex = -1;

    private boolean mIsServerOnline = false;

    private BroadcastReceiver mServerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received server-status broadcast");
            mIsServerOnline = intent.getBooleanExtra("online", false);
        }
    };

    private View.OnClickListener mOnStateTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View aView) {

            if(mIsServerOnline){
              if (aView == mClosedButton) {

                mSubheaderLayout.setVisibility(View.GONE);
                mClosedButton.setChecked(true);
                mOpenButton.setChecked(false);

                mUndeliveredButton.setChecked(false);
                mUnpaidButton.setChecked(false);
                mAllButton.setChecked(true);

                if (mOrderListAdapter != null) {
                    mOrderListAdapter.clear();
                }

                    mServerAPIClient.getClosedOrders(new Callback<Collection<ServerOrder>>() {
                        @Override
                        public void success(final Collection<ServerOrder> aServerOrders, final Response aResponse) {
                            ArrayList<POSOrder> orders = new ArrayList<POSOrder>();
                            for (ServerOrder serverOrder : aServerOrders) {
                                ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                                for (int i = 0; i < serverOrder.order_items.length; i++) {
                                    ServerOrderItem serverOrderItem = serverOrder.order_items[i];
                                    orderItems.add(new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served));
                                }
                                POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                                orders.add(order);
                            }

                            mOrderListAdapter = new OrderListAdapter(aView.getContext(), orders);
                            mListView.setAdapter(mOrderListAdapter);
                        }

                        @Override
                        public void failure(final RetrofitError aRetrofitError) {
                            aRetrofitError.printStackTrace();
                        }
                    });

            } else {
                mSubheaderLayout.setVisibility(View.VISIBLE);
                mClosedButton.setChecked(false);
                mOpenButton.setChecked(true);
                mAllButton.performClick();
            }
            }else{

                mLastButton = (ToggleButton) aView;

                ArrayList<POSOrder> orders = POSOrder.getAllOpenOrders(getActivity());

                mOrderListAdapter = new OrderListAdapter(aView.getContext(), orders);

                mListView.setAdapter(mOrderListAdapter);
            }

        }
    };

    private View.OnClickListener mOnStatusTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View aView) {
            mLastButton = (ToggleButton) aView;

            if (aView == mUndeliveredButton) {
                mUndeliveredButton.setChecked(true);
                mUnpaidButton.setChecked(false);
                mAllButton.setChecked(false);

                if (mOrderListAdapter != null) { mOrderListAdapter.clear(); }
                mServerAPIClient.getUndeliveredOrders(new Callback<Collection<ServerOrder>>() {
                    @Override
                    public void success(final Collection<ServerOrder> aServerOrders, final Response aResponse) {
                        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();
                        for (ServerOrder serverOrder : aServerOrders) {
                            ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                            for (int i = 0; i < serverOrder.order_items.length; i++) {
                                ServerOrderItem serverOrderItem = serverOrder.order_items[i];
                                orderItems.add(new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served));
                            }
                            POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                            orders.add(order);
                        }

                        mOrderListAdapter = new OrderListAdapter(aView.getContext(), orders);
                        mListView.setAdapter(mOrderListAdapter);

                        Intent intent = new Intent("order-selected");

                        if (mOrderListAdapter.getCount() < lastClickItemIndex) {
                            lastClickItemIndex = -1;
                        }
                        if (lastClickItemIndex >= 0) {
                            final POSOrder selectedOrder = (POSOrder) mOrderListAdapter.getItem(lastClickItemIndex);
                            if (selectedOrder != null) {
                                intent.putExtra("order_id", selectedOrder.getId());
                                String tableUid = selectedOrder.getTableUid();
                                if (tableUid != null) {
                                    if (POSTable.getTable(aView.getContext(), tableUid)!=null) {
                                        POSTable table = POSTable.getTable(aView.getContext(), tableUid);
                                        if (table.getName()!=null) {
                                            intent.putExtra("table_name", table.getName());
                                        }else {
                                            intent.putExtra("table_name", "");
                                        }
                                    }else{
                                        intent.putExtra("table_name", "");
                                    }
                                }else {
                                    intent.putExtra("table_name", "");
                                }

                                String orderingMode = POSOrder.getOrderingModeValue(selectedOrder.getOrderingMode()).toUpperCase();
                                intent.putExtra("ordering_mode", orderingMode);

                                Date orderAt = selectedOrder.getOrderAt();
                                intent.putExtra("order_at", orderAt);

                                BigDecimal subtotalPrice = PaymentHelper.calculateSubTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                                intent.putExtra("subtotal_price", subtotalPrice.doubleValue());

                                BigDecimal totalPrice = PaymentHelper.calculateTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                                intent.putExtra("total_price", totalPrice.doubleValue());

                                if (selectedOrder.getReceiptId() != null) {
                                    intent.putExtra("is_paid", true);
                                }
                            }
                        }

                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }

                    @Override
                    public void failure(final RetrofitError aRetrofitError) {
                        aRetrofitError.printStackTrace();

                        Intent intent = new Intent("order-selected");
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                });
            } else if (aView == mUnpaidButton) {
                mUndeliveredButton.setChecked(false);
                mUnpaidButton.setChecked(true);
                mAllButton.setChecked(false);

                if (mOrderListAdapter != null) { mOrderListAdapter.clear(); }
                mServerAPIClient.getUnpaidOrders(new Callback<Collection<ServerOrder>>() {
                    @Override
                    public void success(final Collection<ServerOrder> aServerOrders, final Response aResponse) {
                        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();
                        for (ServerOrder serverOrder : aServerOrders) {
                            ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                            for (int i = 0; i < serverOrder.order_items.length; i++) {
                                ServerOrderItem serverOrderItem = serverOrder.order_items[i];
                                orderItems.add(new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served));
                            }
                            POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                            orders.add(order);
                        }

                        mOrderListAdapter = new OrderListAdapter(aView.getContext(), orders);
                        mListView.setAdapter(mOrderListAdapter);

                        Intent intent = new Intent("order-selected");

                        if (mOrderListAdapter.getCount() < lastClickItemIndex) {
                            lastClickItemIndex = -1;
                        }
                        if (lastClickItemIndex >= 0) {
                            final POSOrder selectedOrder = (POSOrder) mOrderListAdapter.getItem(lastClickItemIndex);
                            if (selectedOrder != null) {
                                intent.putExtra("order_id", selectedOrder.getId());
                                String tableUid = selectedOrder.getTableUid();
                                if (tableUid != null) {
                                    if (POSTable.getTable(aView.getContext(), tableUid)!=null) {
                                        POSTable table = POSTable.getTable(aView.getContext(), tableUid);
                                        if (table.getName()!=null){
                                            intent.putExtra("table_name", table.getName());
                                        } else {
                                            intent.putExtra("table_name", "");
                                        }
                                    }else {
                                        intent.putExtra("table_name", "");
                                    }
                                } else {
                                    intent.putExtra("table_name", "");
                                }

                                String orderingMode = POSOrder.getOrderingModeValue(selectedOrder.getOrderingMode()).toUpperCase();
                                intent.putExtra("ordering_mode", orderingMode);

                                Date orderAt = selectedOrder.getOrderAt();
                                intent.putExtra("order_at", orderAt);

                                BigDecimal subtotalPrice = PaymentHelper.calculateSubTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                                intent.putExtra("subtotal_price", subtotalPrice.doubleValue());

                                BigDecimal totalPrice = PaymentHelper.calculateTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                                intent.putExtra("total_price", totalPrice.doubleValue());

                                if (selectedOrder.getReceiptId() != null) {
                                    intent.putExtra("is_paid", true);
                                }
                            }
                        }

                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }

                    @Override
                    public void failure(final RetrofitError aRetrofitError) {
                        aRetrofitError.printStackTrace();

                        Intent intent = new Intent("order-selected");
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                });
            } else {
                mUndeliveredButton.setChecked(false);
                mUnpaidButton.setChecked(false);
                mAllButton.setChecked(true);

                if (mOrderListAdapter != null) { mOrderListAdapter.clear(); }
                mServerAPIClient.getOpenOrders(new Callback<Collection<ServerOrder>>() {
                    @Override
                    public void success(final Collection<ServerOrder> aServerOrders, final Response aResponse) {
                        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();
                        for (ServerOrder serverOrder : aServerOrders) {
                            ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                            for (int i = 0; i < serverOrder.order_items.length; i++) {
                                ServerOrderItem serverOrderItem = serverOrder.order_items[i];
                                orderItems.add(new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served));
                            }
                            POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                            orders.add(order);
                        }

                        mOrderListAdapter = new OrderListAdapter(aView.getContext(), orders);
                        mListView.setAdapter(mOrderListAdapter);

                        Intent intent = new Intent("order-selected");

                        if (lastClickItemIndex > mOrderListAdapter.getCount()) {
                            lastClickItemIndex = -1;
                        }
                        if (lastClickItemIndex >= 0) {
                            final POSOrder selectedOrder = (POSOrder) mOrderListAdapter.getItem(lastClickItemIndex);
                            if (selectedOrder != null) {
                                intent.putExtra("order_id", selectedOrder.getId());
                                String tableUid = selectedOrder.getTableUid();

                                if (tableUid != null) {
                                    if (POSTable.getTable(aView.getContext(), tableUid)!=null) {
                                        POSTable table = POSTable.getTable(aView.getContext(), tableUid);
                                        if(table.getName()!=null) {
                                            intent.putExtra("table_name", table.getName());
                                        } else {
                                            intent.putExtra("table_name", "");
                                        }
                                    }else {
                                        intent.putExtra("table_name", "");
                                    }
                                }else {
                                    intent.putExtra("table_name", "");
                                }

                                String orderingMode = POSOrder.getOrderingModeValue(selectedOrder.getOrderingMode()).toUpperCase();
                                intent.putExtra("ordering_mode", orderingMode);

                                Date orderAt = selectedOrder.getOrderAt();
                                intent.putExtra("order_at", orderAt);

                                BigDecimal subtotalPrice = PaymentHelper.calculateSubTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                                intent.putExtra("subtotal_price", subtotalPrice.doubleValue());

                                BigDecimal totalPrice = PaymentHelper.calculateTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                                intent.putExtra("total_price", totalPrice.doubleValue());

                                if (selectedOrder.getReceiptId() != null) {
                                    intent.putExtra("is_paid", true);
                                }
                            }
                        }

                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }

                    @Override
                    public void failure(final RetrofitError aRetrofitError) {
                        aRetrofitError.printStackTrace();

                        Intent intent = new Intent("order-selected");
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                    }
                });
            }
        }
    };
    private BroadcastReceiver mOrdersInvalidatedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received orders-invalidated broadcast");

            mLastButton.performClick();
        }
    };

    private AdapterView.OnItemClickListener mListViewItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
            lastClickItemIndex = i;
            final POSOrder selectedOrder = (POSOrder) mOrderListAdapter.getItem(i);
            if (selectedOrder != null) {
                Intent intent = new Intent("order-selected");
                // add data
                intent.putExtra("order_id", selectedOrder.getId());

                String tableUid = selectedOrder.getTableUid();
                if (tableUid != null) {
                    if (POSTable.getTable(aView.getContext(), tableUid)!=null) {
                            POSTable table = POSTable.getTable(aView.getContext(), tableUid);
                            if (table.getName()!=null) {
                                intent.putExtra("table_name", table.getName());
                            }else {
                                intent.putExtra("table_name", "");
                            }
                    }else {
                        intent.putExtra("table_name", "");
                    }
                } else {
                    intent.putExtra("table_name", "");
                }

                String orderingMode = POSOrder.getOrderingModeValue(selectedOrder.getOrderingMode()).toUpperCase();
                intent.putExtra("ordering_mode", orderingMode);

                Date orderAt = selectedOrder.getOrderAt();
                intent.putExtra("order_at", orderAt);

                BigDecimal subtotalPrice = PaymentHelper.calculateSubTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                intent.putExtra("subtotal_price", subtotalPrice.doubleValue());

                BigDecimal totalPrice = PaymentHelper.calculateTotalForOrder(aView.getContext(), selectedOrder.getOrderItems());
                intent.putExtra("total_price", totalPrice.doubleValue());

                if (selectedOrder.getReceiptId() != null) {
                    intent.putExtra("is_paid", true);
                }

                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);

        mSubheaderLayout = view.findViewById(R.id.layout_subheader);

        mOpenButton = (ToggleButton) view.findViewById(R.id.btn_open);
        mClosedButton = (ToggleButton) view.findViewById(R.id.btn_closed);
        mAllButton = (ToggleButton) view.findViewById(R.id.btn_all);
        mUndeliveredButton = (ToggleButton) view.findViewById(R.id.btn_undelivered);
        mUnpaidButton = (ToggleButton) view.findViewById(R.id.btn_unpaid);
        mFilterSpinner = (Spinner) view.findViewById(R.id.spinner_filter);
        mListView = (ListView) view.findViewById(R.id.list_view_orders);

        mOpenButton.setOnClickListener(mOnStateTabClickListener);
        mClosedButton.setOnClickListener(mOnStateTabClickListener);

        mAllButton.setOnClickListener(mOnStatusTabClickListener);
        mUndeliveredButton.setOnClickListener(mOnStatusTabClickListener);
        mUnpaidButton.setOnClickListener(mOnStatusTabClickListener);

        ArrayList<POSTable> POSTables = POSTable.getAllTables(getActivity());
        mTableNumberFilterAdapter = new TableNumberFilterAdapter(getActivity(), POSTables);
        mFilterSpinner.setAdapter(mTableNumberFilterAdapter);
        mFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
                if (i > 0) {
                    POSTable POSTable = (POSTable) mTableNumberFilterAdapter.getItem(i);

                    mOrderListAdapter.setTableUidFilter(POSTable.getUid());
                } else {
                    mOrderListAdapter.setTableUidFilter(null);
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> aAdapterView) {
                mOrderListAdapter.setTableUidFilter(null);
            }
        });

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");

        String serverUrl = "http://" + address + "/";

        mServerAPIClient = new ServerAPIClient(serverUrl);

        mOpenButton.performClick();

        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();
        mOrderListAdapter = new OrderListAdapter(getActivity(), orders);
        mListView.setOnItemClickListener(mListViewItemClickListener);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mOrdersInvalidatedBroadcastReceiver,
            new IntentFilter("orders-invalidated"));

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));

        if (mLastButton == null) {
            mOpenButton.performClick();
        } else {
            mLastButton.performClick();
        }
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mOrdersInvalidatedBroadcastReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mServerStatusBroadcastReceiver);

        super.onPause();
    }

    private class OrderListAdapter extends BaseAdapter {

        Context mContext;
        List<POSOrder> mOrders;
        List<POSOrder> mFilteredOrders = new ArrayList<POSOrder>();
        private String mTableUidFilter;

        public OrderListAdapter(Context context, List<POSOrder> aPOSOrders) {
            mContext = context;
            mOrders = aPOSOrders;
            mTableUidFilter = null;
        }

        @Override
        public Object getItem(final int i) {
            if (mTableUidFilter != null && !mTableUidFilter.equals("")) {
                return mFilteredOrders.get(i);
            }
            if (i < mOrders.size()) {
                return mOrders.get(i);
            }
            notifyDataSetChanged();
            return null;
        }

        @Override
        public int getCount() {
            if (mTableUidFilter != null && !mTableUidFilter.equals("")) {
                return mFilteredOrders.size();
            }
            return mOrders.size();
        }

        @Override
        public long getItemId(final int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.order_list_item, parent, false);
            }

            final POSOrder order = mOrders.get(position);

            if (order != null) {
                TextView orderNumberTextView = (TextView) convertView.findViewById(R.id.txt_order_number);
                TextView tableNumberTextView = (TextView) convertView.findViewById(R.id.txt_table_number);
                TextView priceTextView = (TextView) convertView.findViewById(R.id.txt_price);
                TextView orderingModeTextView = (TextView) convertView.findViewById(R.id.txt_ordering_mode);

                orderNumberTextView.setText(String.format("ORDER #%d", order.getId()));

                // calculate price
                BigDecimal totalPrice = PaymentHelper.calculateTotalForOrder(mContext, order.getOrderItems());


                if (order.getTableUid() != null) {

                    if (POSTable.getTable(convertView.getContext(), order.getTableUid())!=null) {
                        POSTable table = POSTable.getTable(convertView.getContext(), order.getTableUid());
                        tableNumberTextView.setText(table.getName());
                    }else {
                        tableNumberTextView.setText("");
                    }
                } else {
                    tableNumberTextView.setText("");
                }

                //orderNumberTextView.setText();
                priceTextView.setText(String.format("$%.2f", totalPrice));
                orderingModeTextView.setText((order.getOrderingMode() == POSOrder.OrderingMode.DINE_IN) ? "DINE-IN" : "TAKE AWAY");
            }

            return convertView;
        }

        public String getTableUidFilter() {
            return mTableUidFilter;
        }

        public void setTableUidFilter(final String aTableUidFilter) {
            mTableUidFilter = aTableUidFilter;

            mFilteredOrders.clear();
            for (POSOrder order : mOrders) {
                if (order.getTableUid() != null && order.getTableUid().equals(mTableUidFilter)) {
                    mFilteredOrders.add(order);
                }
            }
            notifyDataSetChanged();
        }

        public void clear() {
            mOrders.clear();
            mFilteredOrders.clear();
            notifyDataSetChanged();
        }
    }

    public class TableNumberFilterAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<POSTable> mPOSTables;

        public TableNumberFilterAdapter(final Context aContext, final ArrayList<POSTable> aPOSTables) {
            mContext = aContext;
            mPOSTables = aPOSTables;
        }

        @Override
        public int getCount() {
            return mPOSTables.size() + 1;
        }

        @Override
        public Object getItem(final int i) {
            if (i == 0) {
                return new Object(); // placeholder for takeaway option
            }
            return mPOSTables.get(i - 1);
        }

        @Override
        public long getItemId(final int i) {
            return i - 1;
        }

        @Override
        public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.spinner_item, null);
            }

            TextView textView = (TextView) view.findViewById(R.id.text);

            Object object = getItem(position);
            if (object instanceof POSTable) {
                POSTable POSTable = (POSTable) object;
                textView.setText(POSTable.getName());
            } else {
                textView.setText("None");
            }

            return view;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.spinner_item, null);
            }

            TextView textView = (TextView) view.findViewById(R.id.text);

            Object object = getItem(position);
            if (object instanceof POSTable) {
                POSTable POSTable = (POSTable) object;
                textView.setText(POSTable.getName());
            } else {
                textView.setText("FILTER BY TABLE");
            }

            return view;
        }
    }

    private boolean isNull(String tableuid) {
        return tableuid == null;
    }
}