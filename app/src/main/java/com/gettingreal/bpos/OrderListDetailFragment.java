package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerOrderItem;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSReceipt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import de.timroes.android.listview.EnhancedListView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by ivanfoong on 31/3/14.
 */
public class OrderListDetailFragment extends Fragment {
    private TextView mOrderNumberTextView, mTableNumberTextView, mOrderingModeTextView, mTimestampTextView, mPriceTextView;
    private EnhancedListView mListView;
    private Button mPayButton;
    private OrderItemsAdapter mOrderItemsAdapter;
    private ServerAPIClient mServerAPIClient;
    private Long mSelectedOrderId = null;
    private String mTableName = null;
    private String mOrderingMode = null;
    private Date mOrderAt = null;
    private Double mTotalPayable = null, mSubTotalPayable;
    private boolean mIsPaid = false;

    private BroadcastReceiver mOrderSelectedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received order-selected broadcast");

            if (intent.hasExtra("order_id")) {
                mSelectedOrderId = intent.getLongExtra("order_id", -1L);
                mTableName = intent.getStringExtra("table_name");
                mOrderingMode = intent.getStringExtra("ordering_mode");
                mOrderAt = (Date) intent.getSerializableExtra("order_at");
                mTotalPayable = intent.getDoubleExtra("total_price", 0.0);
                mSubTotalPayable = intent.getDoubleExtra("subtotal_price", 0.0);
                mIsPaid = intent.getBooleanExtra("is_paid", false);

                updateOrderDetails(context, mSelectedOrderId, mTableName, mOrderingMode, mOrderAt, mTotalPayable, mIsPaid);

                mServerAPIClient.getOrderItemsForOrderId(mSelectedOrderId, new Callback<Collection<ServerOrderItem>>() {
                    @Override
                    public void success(final Collection<ServerOrderItem> aServerOrderItems, final Response aResponse) {
                        ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();

                        for (ServerOrderItem serverOrderItem : aServerOrderItems) {
                            POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                            orderItem.setRemark(serverOrderItem.remark);
                            orderItems.add(orderItem);
                        }

                        mOrderItemsAdapter = new OrderItemsAdapter(getActivity(), orderItems);
                        mListView.setAdapter(mOrderItemsAdapter);
                    }

                    @Override
                    public void failure(final RetrofitError aRetrofitError) {
                        aRetrofitError.printStackTrace();
                        ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                        mOrderItemsAdapter = new OrderItemsAdapter(getActivity(), orderItems);
                        mListView.setAdapter(mOrderItemsAdapter);
                    }
                });
            } else {
                clearOrderDetails();
                mOrderItemsAdapter = null;
                mListView.setAdapter(mOrderItemsAdapter);
            }
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_list_detail, container, false);

        mOrderNumberTextView = (TextView) view.findViewById(R.id.txt_order_number);
        mTableNumberTextView = (TextView) view.findViewById(R.id.txt_table_number);
        mOrderingModeTextView = (TextView) view.findViewById(R.id.txt_ordering_mode);
        mTimestampTextView = (TextView) view.findViewById(R.id.txt_timestamp);
        mPriceTextView = (TextView) view.findViewById(R.id.txt_price);
        mPayButton = (Button) view.findViewById(R.id.btn_pay);
        mListView = (EnhancedListView) view.findViewById(R.id.list_view_order_items);

        mListView.setShouldSwipeCallback(new EnhancedListView.OnShouldSwipeCallback() {
            @Override
            public boolean onShouldSwipe(final EnhancedListView listView, final int position) {
                return mOrderItemsAdapter.canBeDelivered(position);
            }
        });

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        String address = settings.getString("address", "192.168.1.168");

        String serverUrl = "http://" + address + "/";

        mServerAPIClient = new ServerAPIClient(serverUrl);

        mListView.setDismissCallback(new EnhancedListView.OnDismissCallback() {
            @Override
            public EnhancedListView.Undoable onDismiss(final EnhancedListView listView, final int position) {
                OrderItemIndividual orderItemIndividual = (OrderItemIndividual) mOrderItemsAdapter.getItem(position);

                final Long orderId = orderItemIndividual.getOrderId();
                final String productUid = orderItemIndividual.getPOSProduct().getUid();

                for (POSOrderItem orderItem : mOrderItemsAdapter.getOrderItems()) {
                    if (orderItem.getOrderId() == orderId && orderItem.getProductUid().equals(productUid)) {
                        ServerOrderItem serverOrderItem = new ServerOrderItem();
                        serverOrderItem.order_id = orderItem.getOrderId();
                        serverOrderItem.product_uid = orderItem.getProductUid();
                        serverOrderItem.quantity_ordered = orderItem.getQuantityOrdered();
                        serverOrderItem.quantity_served = orderItem.getQuantityServed();
                        serverOrderItem.remark = orderItem.getRemark();

                        serverOrderItem.quantity_served += 1;

                        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
                        String address = settings.getString("address", "192.168.192.168");

                        String serverUrl = "http://" + address + "/";

                        final ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);
                        serverAPIClient.updateOrderItemForOrderId(serverOrderItem, new Callback<Collection<ServerOrderItem>>() {
                            @Override
                            public void success(final Collection<ServerOrderItem> aServerOrderItems, final Response aResponse) {
                                serverAPIClient.getOrderItemsForOrderId(orderId, new Callback<Collection<ServerOrderItem>>() {
                                    @Override
                                    public void success(final Collection<ServerOrderItem> aServerOrderItems, final Response aResponse) {
                                        ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();

                                        for (ServerOrderItem serverOrderItem : aServerOrderItems) {
                                            POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                                            orderItem.setRemark(serverOrderItem.remark);
                                            orderItems.add(orderItem);
                                        }

                                        mOrderItemsAdapter = new OrderItemsAdapter(getActivity(), orderItems);
                                        mListView.setAdapter(mOrderItemsAdapter);
                                    }

                                    @Override
                                    public void failure(final RetrofitError aRetrofitError) {
                                        aRetrofitError.printStackTrace();
                                    }
                                });
                            }

                            @Override
                            public void failure(final RetrofitError aRetrofitError) {
                                aRetrofitError.printStackTrace();
                            }
                        });

                        break;
                    }
                }

                mOrderItemsAdapter.remove(position);
                mOrderItemsAdapter.notifyDataSetChanged();

                return null;
            }
        });

        mListView.setSwipingLayout(R.id.swiping_layout);
        mListView.setSwipeDirection(EnhancedListView.SwipeDirection.END);
        mListView.enableSwipeToDismiss();

        mPayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
//                if (mOrderItemsAdapter.isAllOrderItemsDelivered()) {
//                    makePayment(aView.getContext());
//                }
//                else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder
                        .setTitle("No all items are delivered yet!")
                        .setMessage("Continue with payment?")
                        .setCancelable(false)
                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                makePayment(aView.getContext());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface aDialogInterface, final int i) {

                            }
                        });

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
 //               }
            }
        });

        return view;
    }

    private POSReceipt createReceipt(final Context aContext, final POSOrder aPOSOrder) {
//        float totalAmount = PaymentHelper.calculateTotalForOrder(aContext, aPOSOrder.getOrderItems());
//
//        POSReceipt newReceipt = POSReceipt.createReceipt(getActivity(), null, totalAmount, 0.0f, Calendar.getInstance().getTime());
//        aPOSOrder.setReceiptId(newReceipt.getUid());
//        return newReceipt;
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mOrderSelectedBroadcastReceiver,
            new IntentFilter("order-selected"));

        clearOrderDetails();
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mOrderSelectedBroadcastReceiver);
        super.onPause();
    }

    private void updateOrderDetails(final Context aContext, final Long aOrderId, final String aTableName, final String aOrderingMode, final Date aOrderAt, final Double aPrice, final boolean aIsPaid) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mma");

        String orderNumber = String.format("ORDER #%d", aOrderId);
        String orderAt = sdf.format(aOrderAt);
        String price = String.format("$%.2f", aPrice);

        mOrderNumberTextView.setText(orderNumber);
        mTableNumberTextView.setText(aTableName);
        mOrderingModeTextView.setText(aOrderingMode);
        mTimestampTextView.setText(orderAt);
        mPriceTextView.setText(price);

       // mPayButton.setVisibility(aIsPaid ? View.INVISIBLE : View.VISIBLE);
        mPayButton.setVisibility(View.VISIBLE);
    }

    private void clearOrderDetails() {
        String orderNumberString = "", tableNumberString = "", orderingModeString = "",
            timestampString = "", priceString = "";

        mOrderNumberTextView.setText(orderNumberString);
        mTableNumberTextView.setText(tableNumberString);
        mOrderingModeTextView.setText(orderingModeString);
        mTimestampTextView.setText(timestampString);
        mPriceTextView.setText(priceString);
        mPayButton.setVisibility(View.INVISIBLE);

        mOrderItemsAdapter = null;
        mListView.setAdapter(mOrderItemsAdapter);
    }

    private void makePayment(final Context aContext) {
        Intent intent = new Intent(aContext, PaymentActivity.class);
        intent.putExtra("subtotal", mSubTotalPayable);
        intent.putExtra("order_ids", new long[]{mSelectedOrderId});
        startActivity(intent);
    }

    public class OrderItemIndividual {
        private POSProduct mPOSProduct;
        private long mOrderId;
        private boolean mIsDelivered;

        public OrderItemIndividual(final POSProduct aPOSProduct, final long aOrderId, final boolean aIsDelivered) {
            mPOSProduct = aPOSProduct;
            mOrderId = aOrderId;
            mIsDelivered = aIsDelivered;
        }

        public POSProduct getPOSProduct() {
            return mPOSProduct;
        }

        public long getOrderId() {
            return mOrderId;
        }

        public boolean isDelivered() {
            return mIsDelivered;
        }

        public void setDelivered(final boolean aIsDelivered) {
            mIsDelivered = aIsDelivered;
        }
    }

    public class OrderItemsAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<POSOrderItem> mOrderItems;
        private ArrayList<OrderItemIndividual> mDeliveredOrderItemIndividual, mUndeliveredOrderItemIndividual;

        public OrderItemsAdapter(final Context aContext, final ArrayList<POSOrderItem> aPOSOrderItems) {
            mContext = aContext;
            mOrderItems = aPOSOrderItems;

            processOrderItems(mContext, mOrderItems);
        }

        private void processOrderItems(final Context aContext, final ArrayList<POSOrderItem> aPOSOrderItems) {
            mDeliveredOrderItemIndividual = new ArrayList<OrderItemIndividual>();
            mUndeliveredOrderItemIndividual = new ArrayList<OrderItemIndividual>();

            for (POSOrderItem orderItem : aPOSOrderItems) {
                POSProduct product = POSProduct.getProduct(aContext, orderItem.getProductUid());

                int quantityOrdered = orderItem.getQuantityOrdered();
                int quantityServed = orderItem.getQuantityServed();
                int quantityUndelivered = quantityOrdered - quantityServed;

                for (int i = 0; i < quantityServed; i++) {
                    mDeliveredOrderItemIndividual.add(new OrderItemIndividual(product, orderItem.getOrderId(), true));
                }

                for (int i = 0; i < quantityUndelivered; i++) {
                    mUndeliveredOrderItemIndividual.add(new OrderItemIndividual(product, orderItem.getOrderId(), false));
                }
            }
        }

        @Override
        public int getCount() {
            return mUndeliveredOrderItemIndividual.size() + mDeliveredOrderItemIndividual.size() + 2;
        }

        @Override
        public Object getItem(final int i) {
            if (i == 0) {
                // Undelivered section header
                return null;
            } else if (i < mUndeliveredOrderItemIndividual.size() + 1) {
                return mUndeliveredOrderItemIndividual.get(i - 1);
            } else if (i == mUndeliveredOrderItemIndividual.size() + 1) {
                // Delivered section header
                return null;
            } else {
                return mDeliveredOrderItemIndividual.get(i - mUndeliveredOrderItemIndividual.size() - 2);
            }
        }

        @Override
        public long getItemId(final int i) {
            return i;
        }

        public ArrayList<POSOrderItem> getOrderItems() {
            return mOrderItems;
        }

        public boolean canBeDelivered(final int i) {
            return (getItem(i) != null && i < mUndeliveredOrderItemIndividual.size() + 1);
        }

        public boolean isAllOrderItemsDelivered() {
            return (mUndeliveredOrderItemIndividual.size() == 0);
        }

        @Override
        public View getView(final int i, final View aView, final ViewGroup aViewGroup) {
            View view = aView;

            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.list_item_delete, null, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.text);
            Button deleteButton = (Button) view.findViewById(R.id.button_delete);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View aView) {
                    final OrderItemIndividual orderItemIndividual = (OrderItemIndividual) getItem(i);
                    new AlertDialog.Builder(getActivity())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Void item?")
                            .setMessage("Void this item? (" + orderItemIndividual.getPOSProduct().getName() + ")")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface aDialogInterface, int index) {

                                    final Long orderId = orderItemIndividual.getOrderId();
                                    final String productUid = orderItemIndividual.getPOSProduct().getUid();

                                    for (POSOrderItem orderItem : getOrderItems()) {
                                        if (orderItem.getOrderId() == orderId && orderItem.getProductUid().equals(productUid)) {
                                            ServerOrderItem serverOrderItem = new ServerOrderItem();
                                            serverOrderItem.order_id = orderItem.getOrderId();
                                            serverOrderItem.product_uid = orderItem.getProductUid();
                                            serverOrderItem.quantity_ordered = orderItem.getQuantityOrdered();
                                            serverOrderItem.quantity_served = orderItem.getQuantityServed();
                                            serverOrderItem.remark = orderItem.getRemark();

                                            serverOrderItem.quantity_ordered -= 1;

                                            if (serverOrderItem.quantity_ordered <= 0) {
                                                SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
                                                String address = settings.getString("address", "192.168.192.168");

                                                String serverUrl = "http://" + address + "/";

                                                final ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);
                                                serverAPIClient.deleteOrderItemForOrderId(serverOrderItem.order_id, serverOrderItem.product_uid, new Callback<Collection<ServerOrderItem>>() {
                                                    @Override
                                                    public void success(final Collection<ServerOrderItem> aServerOrderItems, final Response aResponse) {
                                                        serverAPIClient.getOrderItemsForOrderId(orderId, new Callback<Collection<ServerOrderItem>>() {
                                                            @Override
                                                            public void success(final Collection<ServerOrderItem> aServerOrderItems, final Response aResponse) {
                                                                ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();

                                                                for (ServerOrderItem serverOrderItem : aServerOrderItems) {
                                                                    POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                                                                    orderItem.setRemark(serverOrderItem.remark);
                                                                    orderItems.add(orderItem);
                                                                }

                                                                mOrderItemsAdapter = new OrderItemsAdapter(getActivity(), orderItems);
                                                                mListView.setAdapter(mOrderItemsAdapter);
                                                            }

                                                            @Override
                                                            public void failure(final RetrofitError aRetrofitError) {
                                                                aRetrofitError.printStackTrace();
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void failure(final RetrofitError aRetrofitError) {
                                                        aRetrofitError.printStackTrace();
                                                    }
                                                });
                                            }
                                            else {
                                                SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
                                                String address = settings.getString("address", "192.168.192.168");

                                                String serverUrl = "http://" + address + "/";

                                                final ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);
                                                serverAPIClient.updateOrderItemForOrderId(serverOrderItem, new Callback<Collection<ServerOrderItem>>() {
                                                    @Override
                                                    public void success(final Collection<ServerOrderItem> aServerOrderItems, final Response aResponse) {
                                                        serverAPIClient.getOrderItemsForOrderId(orderId, new Callback<Collection<ServerOrderItem>>() {
                                                            @Override
                                                            public void success(final Collection<ServerOrderItem> aServerOrderItems, final Response aResponse) {
                                                                ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();

                                                                for (ServerOrderItem serverOrderItem : aServerOrderItems) {
                                                                    POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                                                                    orderItem.setRemark(serverOrderItem.remark);
                                                                    orderItems.add(orderItem);
                                                                }

                                                                mOrderItemsAdapter = new OrderItemsAdapter(getActivity(), orderItems);
                                                                mListView.setAdapter(mOrderItemsAdapter);
                                                            }

                                                            @Override
                                                            public void failure(final RetrofitError aRetrofitError) {
                                                                aRetrofitError.printStackTrace();
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void failure(final RetrofitError aRetrofitError) {
                                                        aRetrofitError.printStackTrace();
                                                    }
                                                });
                                            }

                                            remove(i);
                                            notifyDataSetChanged();

                                            Intent intent = new Intent("orders-invalidated");
                                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

                                            break;
                                        }
                                    }
                                }
                            })
                            .show();
                }
            });

            View backgroundView = view.findViewById(R.id.swiping_layout);

            deleteButton.setVisibility(View.VISIBLE);

            Object object = getItem(i);
            if (object != null) {
                if (mIsPaid) {
                    deleteButton.setVisibility(View.GONE);
                }
                OrderItemIndividual orderItemIndividual = (OrderItemIndividual) object;
                if (orderItemIndividual.getPOSProduct() != null) {
                    POSProduct product = POSProduct.getProduct(mContext, orderItemIndividual.getPOSProduct().getUid());
                    if (product != null) {
                        textView.setText(product.getName());
                    }
                }
                backgroundView.setBackgroundColor(Color.parseColor("#ffffff"));
            } else {
                backgroundView.setBackgroundColor(Color.parseColor("#e0e0e0"));
                deleteButton.setVisibility(View.GONE);
                if (i == 0) {
                    textView.setText("Undelivered");
                } else {
                    textView.setText("Delivered");
                }
            }

            return view;
        }

        public void remove(final int i) {
            if (i == 0) {
                // Undelivered section header
            } else if (i < mUndeliveredOrderItemIndividual.size() + 1) {
                mUndeliveredOrderItemIndividual.remove(i - 1);
            } else if (i == mUndeliveredOrderItemIndividual.size() + 1) {
                // Delivered section header
            } else {
                mDeliveredOrderItemIndividual.remove(i - mUndeliveredOrderItemIndividual.size() - 2);
            }
        }
    }
}
