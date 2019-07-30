package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.Fragment;
//import android.support.v4.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.epson.eposprint.Builder;
import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerOrder;
import com.gettingreal.bpos.api.ServerOrderItem;
import com.gettingreal.bpos.api.ServerPostOrderResponse;
import com.gettingreal.bpos.helper.PrintPOS2Help;
import com.gettingreal.bpos.helper.PrinterSessionManager;
import com.gettingreal.bpos.helper.ShowMsg;
import com.gettingreal.bpos.model.POSCategory;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSSurcharge;
import com.gettingreal.bpos.model.POSTable;
import com.google.gson.Gson;
import com.slidinglayer.SlidingLayer;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class CheckoutFragment extends Fragment implements PrintOrderTask.OnPrintOrderTaskCompleted,ReceiveListener {
    private POSOrder mOrder;

    private ListView mListView;

    private View mTableNumberLayout;

    private ToggleButton mDineInButton, mTakeawayButton;

    private TextView mSubtotalTextView, mTaxTextView, mServiceChargeTextView, mTotalTextView, mServiceChargeTitleTextView, mTaxTitleTextView;

    private BigDecimal mSubtotal = BigDecimal.ZERO, mServiceChargeAmount = BigDecimal.ZERO, mTaxAmount = BigDecimal.ZERO, mTotalAmount = BigDecimal.ZERO;

    private Spinner mTableNumberSpinner;

    private boolean isTakeAway = false;

    private ArrayList<POSTable> mPOSTables;

    private TableNumberAdapter mTableNumberAdapter;

    private Picasso mPicasso;

    private Context mContext = null;

    //private Printer  mPrinter = null;

    POSCategory PrinterIP=null;

    PrinterSessionManager printerSessionManager;

    String quantity_item,item_pricePerItem,totalPrice,totalPricePerItem,mSubtotalPrice,mServiceChargePrice,mTaxPrice;

    ArrayList<POSOrderItem> orderItems;

    String printIPCheckenRoom,strOrderItemName,strOrderItemByQty;

    String receiptType;

    Hashtable<String, ArrayList<POSOrderItem>> orderItemsForPrinterUid;
    Hashtable<String, Printer> mPrinters;

    PrintPOS2Help printPOS2Help;

    String thelastPrintIP;

    String masterPrinter;

    private SlidingLayer mSlidingLayer;

    private BroadcastReceiver mCheckoutBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received cart-updated broadcast");
            updateCart();
        }
    };

    private boolean mIsServerOnline = false;

    private BroadcastReceiver mServerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received server-status broadcast");
            mIsServerOnline = intent.getBooleanExtra("online", false);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_checkout, container, false);
        printPOS2Help=new PrintPOS2Help();
        mListView = (ListView) view.findViewById(R.id.listView);
        mSubtotalTextView = (TextView) view.findViewById(R.id.txt_subtotal_amount);
        mTaxTextView = (TextView) view.findViewById(R.id.txt_tax_amount);
        mServiceChargeTextView = (TextView) view.findViewById(R.id.txt_service_charge_amount);
        mTotalTextView = (TextView) view.findViewById(R.id.txt_total_amount);
        mServiceChargeTitleTextView = (TextView)view.findViewById(R.id.txt_service_charge_title);
        mTaxTitleTextView = (TextView)view.findViewById(R.id.txt_tax_amount_title);
        printerSessionManager=new PrinterSessionManager(getActivity().getApplicationContext());
        mSlidingLayer = (SlidingLayer) view.findViewById(R.id.slidingLayer);

        masterPrinter=printerSessionManager.getPrintURL().toUpperCase();

        // TODO: fix this hack to use priority to identify surcharge percentage
        for (POSSurcharge surcharge : POSSurcharge.getAllSurcharges(view.getContext())) {
            if (surcharge.getPriority() == 1) {
                mServiceChargeTitleTextView.setText("Svc Charge (" + String.format("%.0f", surcharge.getPercentage()) + "%)");
            }
            else if (surcharge.getPriority() == 2) {
                mTaxTitleTextView.setText("GST (" + String.format("%.0f", surcharge.getPercentage()) + "%)");
                totalPrice=mTaxTextView.getText().toString();
            }
        }

        try {
            com.epson.epos2.Log.setLogSettings(mContext,  com.epson.epos2.Log.PERIOD_PERMANENT,  com.epson.epos2.Log.OUTPUT_STORAGE, null, 9090, 1,  com.epson.epos2.Log.LOGLEVEL_LOW);


        } catch (Epos2Exception e) {
            e.printStackTrace();
        }

        mTableNumberLayout = view.findViewById(R.id.layout_table_number);
        Button confirmOrderButton = (Button) view.findViewById(R.id.btn_confirm_order);
        confirmOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                if (!isTakeAway) {
                    if (mTableNumberSpinner.getSelectedItemPosition() == 0) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                        alertDialogBuilder
                                .setMessage("Table Number is required for Dine-In checkout")
                                .setCancelable(false)
                                .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        mTableNumberSpinner.requestFocus();
                                    }
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    } else {
                        confirmOrder(aView);
                    }
                }else {
                    confirmOrder(aView);
                }

            }
        });

        mDineInButton = (ToggleButton) view.findViewById(R.id.btn_dine_in);

        mDineInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                dineIn();
            }
        });

        mTakeawayButton = (ToggleButton) view.findViewById(R.id.btn_takeaway);

        mTakeawayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                takeaway();
            }
        });

        mTableNumberSpinner = (Spinner) view.findViewById(R.id.spn_table_number);

        mPicasso = ((MyApplication)getActivity().getApplication()).getPicasso();

        //CK: initialising printers
        mPrinters = new Hashtable<String, Printer>();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mCheckoutBroadcastReceiver,
                new IntentFilter("checkout"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));

        mPOSTables = POSTable.getAllTables(getActivity());
        mTableNumberAdapter = new TableNumberAdapter(getActivity(), mPOSTables);
        mTableNumberSpinner.setAdapter(mTableNumberAdapter);
        mTableNumberAdapter.notifyDataSetChanged();
        updateCart();

        printerSessionManager=new PrinterSessionManager(getActivity().getApplicationContext());
        masterPrinter=printerSessionManager.getPrintURL().toUpperCase();
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mCheckoutBroadcastReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mServerStatusBroadcastReceiver);

        printerSessionManager=new PrinterSessionManager(getActivity().getApplicationContext());
        masterPrinter=printerSessionManager.getPrintURL().toUpperCase();
        super.onPause();
    }

    private void saveTableNumberSelection(final Context aContext) {
        final int tableNumberSpinnerSelectedIndex = mTableNumberSpinner.getSelectedItemPosition();
        if (tableNumberSpinnerSelectedIndex > 0) {
            POSTable selectedPOSTable = (POSTable) mTableNumberAdapter.getItem(tableNumberSpinnerSelectedIndex);
            SettingHelper.setLastTableUid(aContext, selectedPOSTable.getUid());
        } else {
            SettingHelper.removeLastTableUid(aContext);
        }
    }

    private void updateCart() {
        final String lastTableUid = SettingHelper.getLastTableUid(getActivity());
        if (lastTableUid != null) {
            dineIn();

            for (int i = 1; i < mTableNumberAdapter.getCount(); i++) {
                POSTable POSTable = (POSTable) mTableNumberAdapter.getItem(i);
                if (POSTable.getUid().contentEquals(lastTableUid)) {
                    mTableNumberSpinner.setSelection(i);
                    break;
                }
            }
        } else {
            takeaway();
        }

        orderItems = POSOrderItem.getPendingOrderItems(getActivity());
        final OrderItemAdapter adapter = new OrderItemAdapter(getActivity(), orderItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mListView.setAdapter(adapter);

        mSubtotal = BigDecimal.ZERO;
        for (POSOrderItem orderItem : orderItems) {
            POSProduct product = POSProduct.getProduct(getActivity(), orderItem.getProductUid());
            if (product != null) {
                mSubtotal = mSubtotal.add(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(orderItem.getQuantityOrdered())));
            }
        }
        mSubtotalTextView.setText(String.format("%.2f", mSubtotal));

        // TODO: fix this hack to use priority to identify surcharge percentage
        for (POSSurcharge surcharge : POSSurcharge.getAllSurcharges(getActivity())) {
            if (surcharge.getPriority() == 1) {
                mServiceChargeAmount = mSubtotal.multiply(BigDecimal.valueOf(surcharge.getPercentage()).divide(BigDecimal.valueOf(100.0)));
                mServiceChargeTextView.setText(String.format("%.2f", mServiceChargeAmount));
            }
            else if (surcharge.getPriority() == 2) {
                mTaxAmount = mSubtotal.add(mServiceChargeAmount).multiply(BigDecimal.valueOf(surcharge.getPercentage()).divide(BigDecimal.valueOf(100.0)));
                mTaxTextView.setText(String.format("%.2f", mTaxAmount));
            }
        }

        mTotalAmount = mSubtotal.add(mServiceChargeAmount).add(mTaxAmount);
        mTotalTextView.setText(String.format("%.2f", mTotalAmount));
        mSubtotalPrice=mSubtotalTextView.getText().toString();
        mServiceChargePrice=mServiceChargeTextView.getText().toString();
        mTaxPrice=mTaxTextView.getText().toString();
        totalPrice=mTotalTextView.getText().toString();

    }

    public void confirmOrder(View v) {
            saveTableNumberSelection(v.getContext());
            // validate if table number is selected for dine-in
            if (!isTakeAway) {

                if (mTableNumberSpinner.getSelectedItemPosition() == 0) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder
                            .setMessage("Table Number is required for Dine-In checkout")
                            .setCancelable(false)
                            .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mTableNumberSpinner.requestFocus();
                                }
                            });

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();
                } else {
                    SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
                    String address = settings.getString("address", "192.168.192.168");

                    String serverUrl = "http://" + address + "/";

                    ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);

                    final POSOrder order = createOrder();
                   // Toast.makeText(mContext, "db save success", Toast.LENGTH_LONG).show();
                    ServerOrder serverOrder = new ServerOrder();
                    serverOrder.table_uid = order.getTableUid();
                    serverOrder.receipt_id = order.getReceiptId();
                    serverOrder.ordering_mode = POSOrder.getOrderingModeValue(order.getOrderingMode());
                    serverOrder.order_at = order.getOrderAt();

                    ArrayList<POSOrderItem> posOrderItems = order.getOrderItems();
                    serverOrder.order_items = new ServerOrderItem[posOrderItems.size()];
                    for (int i = 0; i < posOrderItems.size(); i++) {
                        POSOrderItem posOrderItem = posOrderItems.get(i);

                        ServerOrderItem orderItem = new ServerOrderItem();
                        orderItem.product_uid = posOrderItem.getProductUid();
                        orderItem.quantity_ordered = posOrderItem.getQuantityOrdered();
                        orderItem.quantity_served = posOrderItem.getQuantityServed();
                        if (posOrderItem.getRemark() == null || posOrderItem.getRemark() == "") {
                            orderItem.remark = "-";
                        } else {
                            orderItem.remark = posOrderItem.getRemark();
                        }

                        serverOrder.order_items[i] = orderItem;

                    }

                    ///Edit for Start PrintOrder List///
                    mOrder = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                    orderItemsForPrinterUid = new Hashtable<String, ArrayList<POSOrderItem>>();
                    for (POSOrderItem orderItem1 : mOrder.getOrderItems()) {
                        POSProduct product = POSProduct.getProduct(getActivity(), orderItem1.getProductUid());
                        for (String categoryUid : product.getCategoryUids()) {
                            POSCategory category = POSCategory.getCategory(getActivity(), categoryUid);
                            for (String printerUid : category.getPrinterUids()) {
                                if (!orderItemsForPrinterUid.containsKey(printerUid)) {
                                    orderItemsForPrinterUid.put(printerUid, new ArrayList<POSOrderItem>());
                                }

                                ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                                orderItems.add(orderItem1);

                            }
                        }
                    }

                    if (orderItemsForPrinterUid.keySet().size() <= 1) {
                        for (String printerUid : orderItemsForPrinterUid.keySet()) {
                            thelastPrintIP = "";
                            ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                            PrintCheckenRoomSequence(printerUid.toUpperCase(), mOrder, orderItems);
                            thelastPrintIP = printerUid.toUpperCase();

                        }
                        if (thelastPrintIP != null) {
                            if (!thelastPrintIP.equals(masterPrinter)) {
                                PrintCheckenRoomSequence(masterPrinter, mOrder, orderItems);
                            }
                        }
                    } else {
                        for (String printerUid : orderItemsForPrinterUid.keySet()) {
                            ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                            PrintCheckenRoomSequence(printerUid.toUpperCase(), mOrder, orderItems);
                            thelastPrintIP = printerUid.toUpperCase();
                        }
                        if (thelastPrintIP != null) {
                            if (thelastPrintIP.equals(masterPrinter)) {
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        PrintCheckenRoomSequence(masterPrinter, mOrder, orderItems);
                                    }
                                }, 2500);
                            } else if (!thelastPrintIP.equals(masterPrinter)) {
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        PrintCheckenRoomSequence(masterPrinter, mOrder, orderItems);
                                    }
                                }, 1000);
                            }
                        }
                    }
                    ///Edit End PrintOrder List///

                        //print orderItem
                        serverAPIClient.createOrder(serverOrder, new Callback<ServerPostOrderResponse>() {
                            @Override
                            public void success(final ServerPostOrderResponse aServerPostOrderResponse, final Response aResponse) {
                                POSOrder.clearOrders(getActivity());
                                POSOrderItem.clearOrderItems(getActivity());
                                Log.e("ServerPOSt", aServerPostOrderResponse.orders.toString());

                                for (int i = 0; i < aServerPostOrderResponse.orders.length; i++) {
                                    ServerOrder serverOrder = aServerPostOrderResponse.orders[i];
                                    ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                                    for (int k = 0; k < serverOrder.order_items.length; k++) {
                                        ServerOrderItem serverOrderItem = serverOrder.order_items[k];
                                        POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                                        orderItem.setRemark(serverOrderItem.remark);
                                        orderItems.add(orderItem);
                                    }

                                    POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                                    printOrder(order);
                                }
                            }

                            @Override
                            public void failure(final RetrofitError aRetrofitError) {
                                aRetrofitError.printStackTrace();
                                Log.e("CreateOrderFail_Error", aRetrofitError.toString());
                              //  Toast.makeText(getActivity(), "Server can't Create Order Items", Toast.LENGTH_SHORT).show();
                            }
                        });
                        // end api

                }
            } else {
                SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
                String address = settings.getString("address", "192.168.192.168");

                String serverUrl = "http://" + address + "/";

                ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);

                final POSOrder order = createOrder();

                ServerOrder serverOrder = new ServerOrder();

                if (order.getTableUid()==null){
                    serverOrder.table_uid = "";
                }else {
                    serverOrder.table_uid = order.getTableUid();
                }

                serverOrder.receipt_id = order.getReceiptId();
                serverOrder.ordering_mode = POSOrder.getOrderingModeValue(order.getOrderingMode());
                serverOrder.order_at = order.getOrderAt();

                ArrayList<POSOrderItem> posOrderItems = order.getOrderItems();
                serverOrder.order_items = new ServerOrderItem[posOrderItems.size()];
                for (int i = 0; i < posOrderItems.size(); i++) {
                    POSOrderItem posOrderItem = posOrderItems.get(i);

                    ServerOrderItem orderItem = new ServerOrderItem();
                    orderItem.product_uid = posOrderItem.getProductUid();
                    orderItem.quantity_ordered = posOrderItem.getQuantityOrdered();
                    orderItem.quantity_served = posOrderItem.getQuantityServed();
                    if (posOrderItem.getRemark()==null){
                        orderItem.remark ="-";
                    }else {
                        orderItem.remark = posOrderItem.getRemark();
                    }
                    serverOrder.order_items[i] = orderItem;
                }

                ///Start Print Order List///
                mOrder = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                orderItemsForPrinterUid = new Hashtable<String, ArrayList<POSOrderItem>>();
                for (POSOrderItem orderItem1 : mOrder.getOrderItems()) {
                    POSProduct product = POSProduct.getProduct(getActivity(), orderItem1.getProductUid());
                    for (String categoryUid : product.getCategoryUids()) {
                        POSCategory category = POSCategory.getCategory(getActivity(), categoryUid);
                        for (String printerUid : category.getPrinterUids()) {
                            if (!orderItemsForPrinterUid.containsKey(printerUid)) {
                                orderItemsForPrinterUid.put(printerUid, new ArrayList<POSOrderItem>());
                            }

                            ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                            orderItems.add(orderItem1);

                        }
                    }
                }
                if (orderItemsForPrinterUid.keySet().size()<=1){
                    for (String printerUid : orderItemsForPrinterUid.keySet()) {
                        thelastPrintIP=" ";
                        ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                        PrintCheckenRoomSequence(printerUid.toUpperCase(), mOrder, orderItems);
                        thelastPrintIP = printerUid.toUpperCase();
                    }
                    if (thelastPrintIP!=null) {
                        if (!thelastPrintIP.equals(masterPrinter)) {
                            PrintCheckenRoomSequence(masterPrinter, mOrder, orderItems);

                        }
                    }
                }else {
                    for (String printerUid : orderItemsForPrinterUid.keySet()) {
                        thelastPrintIP="";
                        ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                        PrintCheckenRoomSequence(printerUid.toUpperCase(), mOrder, orderItems);
                        thelastPrintIP = printerUid.toUpperCase();
                    }
                    if (thelastPrintIP!=null) {
                        if (thelastPrintIP.equals(masterPrinter)) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    PrintCheckenRoomSequence(masterPrinter, mOrder, orderItems);
                                }
                            }, 2500);
                        } else if (!thelastPrintIP.equals(masterPrinter)) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    PrintCheckenRoomSequence(masterPrinter, mOrder, orderItems);
                                }
                            }, 1000);
                        }
                    }
                }

                ///End Print Order List///
                serverAPIClient.createOrder(serverOrder, new Callback<ServerPostOrderResponse>() {
                    @Override
                    public void success(final ServerPostOrderResponse aServerPostOrderResponse, final Response aResponse) {
                        POSOrder.clearOrders(getActivity());
                        POSOrderItem.clearOrderItems(getActivity());

                        for (int i = 0; i < aServerPostOrderResponse.orders.length; i++) {
                            ServerOrder serverOrder = aServerPostOrderResponse.orders[i];
                            ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                            for (int k = 0; k < serverOrder.order_items.length; k++) {
                                ServerOrderItem serverOrderItem = serverOrder.order_items[k];
                                POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                                orderItem.setRemark(serverOrderItem.remark);
                                orderItems.add(orderItem);
                            }

                            POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                            printOrder(order);
                        }
                    }

                    @Override
                    public void failure(final RetrofitError aRetrofitError) {
                        aRetrofitError.printStackTrace();
                        Log.e("CreateOrderFail_Error", aRetrofitError.toString());
                       // Toast.makeText(getActivity(),"Server can't Create Order Items",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        // Offline Thank you box
        if (mIsServerOnline) {
        }else{((MenuActivity)getActivity()).showOrderSentDialog();
        }
    }

    public void dineIn() {
        isTakeAway = false;
        mDineInButton.setChecked(true);
        mTakeawayButton.setChecked(false);
        mTableNumberLayout.setVisibility(View.VISIBLE);
        receiptType="Dine-In";
    }

    public void takeaway() {
        isTakeAway = true;
        mDineInButton.setChecked(false);
        mTakeawayButton.setChecked(true);
        mTableNumberLayout.setVisibility(View.GONE);
        receiptType="Takeaway";
    }

    private POSOrder createOrder() {
        ArrayList<POSOrderItem> orderItems = POSOrderItem.getPendingOrderItems(getActivity());
        if (isTakeAway == false && mTableNumberSpinner.getSelectedItemPosition() > 0) {
            POSTable selectedPOSTable = mPOSTables.get(mTableNumberSpinner.getSelectedItemPosition() - 1);
            return POSOrder.createOrder(getActivity(), Calendar.getInstance().getTime(), selectedPOSTable.getUid(), isTakeAway ? POSOrder.OrderingMode.TAKE_AWAY : POSOrder.OrderingMode.DINE_IN, orderItems);
        } else {
            return POSOrder.createOrder(getActivity(), Calendar.getInstance().getTime(), null, isTakeAway ? POSOrder.OrderingMode.TAKE_AWAY : POSOrder.OrderingMode.DINE_IN, orderItems);
        }
    }

    public void printOrder(POSOrder aPOSOrder) {
        PrintOrderTask printOrderTask = new PrintOrderTask(getActivity(), aPOSOrder, this);
        printOrderTask.execute();

    }

    public void onPrintOrderTaskCompleted(final String errorMessage) {
        Intent intent = new Intent("cart-updated");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

        if (errorMessage != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();
        } else {
            intent = new Intent("checkout-completed");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
    }


    private class OrderItemAdapter extends ArrayAdapter<POSOrderItem> {

        Context mContext;
        List<POSOrderItem> mOrderItems;

        public OrderItemAdapter(Context context, List<POSOrderItem> aPOSOrderItems) {
            super(context, R.layout.cart_item, aPOSOrderItems);
            mContext = context;
            mOrderItems = aPOSOrderItems;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.cart_item, parent, false);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.img_item);
            TextView nameTextView = (TextView) rowView.findViewById(R.id.txt_name);
            TextView priceTextView = (TextView) rowView.findViewById(R.id.txt_price);
            TextView quantityTextView = (TextView) rowView.findViewById(R.id.txt_quantity);

            POSOrderItem orderItem = mOrderItems.get(position);

            POSProduct product = POSProduct.getProduct(mContext, orderItem.getProductUid());
            if (product != null) {
                nameTextView.setText(product.getName());
                priceTextView.setText(String.format("$%.2f", product.getPrice() * orderItem.getQuantityOrdered()));

                mPicasso.load(product.getImageFile()).fit().into(imageView);
                totalPricePerItem=priceTextView.getText().toString();
            }
            item_pricePerItem= product.getPrice().toString();
            if (orderItem.getQuantityOrdered() > 1) {
                quantityTextView.setVisibility(View.VISIBLE);
                quantityTextView.setText(String.valueOf(orderItem.getQuantityOrdered()));
                quantity_item=quantityTextView.getText().toString();
            } else {
                quantityTextView.setVisibility(View.INVISIBLE);
            }

            return rowView;
        }
    }

    public class TableNumberAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<POSTable> mPOSTables;
        String receiptTable;
        public TableNumberAdapter(final Context aContext, final ArrayList<POSTable> aPOSTables) {
            mContext = aContext;
            mPOSTables = aPOSTables;
        }

        public String getTableName(){
            return receiptTable;
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
                receiptTable=POSTable.getName();
            } else {
                textView.setText("CHOOSE TABLE NO.");
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
                receiptTable=POSTable.getName();
            } else {
                textView.setText("CHOOSE TABLE NO.");
            }
            return view;
        }
    }

    private boolean PrintCheckenRoomSequence(String printerIp,final POSOrder order,ArrayList<POSOrderItem> orderItems) {
        //Min Thein Win
        String printIP="",printName="";
        boolean haveNumber= PrintPOS2Help.containsDigitPinterStr(printerIp);
        if (haveNumber){
            StringTokenizer namEIP=new StringTokenizer(printerIp,",");
            ArrayList<String> strNew=new ArrayList<String>();
            while (namEIP.hasMoreTokens()){
                strNew.add(namEIP.nextToken());
            }
            printIP=strNew.get(0);
            printName=strNew.get(1);

        }else {
            //set Default Printe Model and IP;
            printIP="TCP:192.168.192.168";
            printName="TM-T88V";
        }

        if (!initializeObject(printerIp)) {
            return false;
        }
        //retrieve the printer object from hashtable
        Printer printer = mPrinters.get(printerIp);

        // Printing ....
            if (printerSessionManager.getOrderPrintSize() != null){
                if (printerSessionManager.getOrderPrintSize().equalsIgnoreCase("88MM")) {
                    if(!PrintOrder80MMThaiItem(order, orderItems,printIP)) {
                        finalizeObject(printer);
                        return false;
                    }

                } else if(printerSessionManager.getOrderPrintSize().equalsIgnoreCase("58MM")) {
                    if (!PrintOrder58MMThaiItem(order, orderItems,printIP)) {
                        finalizeObject(printer);
                        return false;

                    }
                }
            }else {
                Toast.makeText(mContext.getApplicationContext(),"PLease Choice OrderItems PrintPaper Size",Toast.LENGTH_LONG).show();
            }

        if (!printDataCheckenRoom(printIP)) {
            finalizeObject(printer);
            return false;
        }
        return true;
    }

    public boolean printData(String receiptPrintIP) {

        //retrieve the printer object from hashtable
        Printer mPrinter = mPrinters.get(receiptPrintIP);
        if (mPrinter == null) {
            return false;
        }

        if (!connectPrinter(receiptPrintIP)) {
            return false;
        }

        PrinterStatusInfo status = mPrinter.getStatus();
        if (!isPrintable(status)) {
            ShowMsg.showMsg(printPOS2Help.makeErrorMessage(status), getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "sendData", getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        return true;
    }

    public boolean printDataCheckenRoom(String printIPCheckenRoom) {

        //retrieve the printer object from hashtable
        Printer mPrinter = mPrinters.get(printIPCheckenRoom);
        if (mPrinter == null) {
            return false;
        }

        if (!connectPrinterCheckenRoom(printIPCheckenRoom)) {
            return false;
        }

        PrinterStatusInfo status = mPrinter.getStatus();
        if (!isPrintable(status)) {
            ShowMsg.showMsg(printPOS2Help.makeErrorMessage(status), getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "sendData", getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        return true;
    }

    public boolean isPrintable(PrinterStatusInfo status) {
        if (status == null) {
            return false;
        }

        if (status.getConnection() == Printer.FALSE) {
            return false;
        }
        else if (status.getOnline() == Printer.FALSE) {
            return false;
        }
        else {
            ;//print available
        }

        return true;
    }

    public boolean initializeObject(String printerIp) {
        String printIP="",printName="";
        boolean haveNumber= PrintPOS2Help.containsDigitPinterStr(printerIp);
        if (haveNumber){
            StringTokenizer namEIP=new StringTokenizer(printerIp,",");
            ArrayList<String> strNew=new ArrayList<String>();
            while (namEIP.hasMoreTokens()){
                strNew.add(namEIP.nextToken());
            }
            printIP=strNew.get(0);
            printName=strNew.get(1);

        }else {
            //set Default Printe Model and IP;
            printIP="TCP:192.168.192.168";
            printName="TM-T88V";
        }

        //TODO: if printer is not inside the hash table, add it in, and create new object, else just retrieve and skip the creation
        if(!mPrinters.contains(printIP)) {
            Printer mPrinter;

            try {

                if (printName.equalsIgnoreCase("TM-T88V")) {
                    mPrinter = new Printer(Printer.TM_T88, Printer.LANG_EN, getActivity());
                } else if (printName.equalsIgnoreCase("TM-T82")) {
                    mPrinter = new Printer(Printer.TM_T82, Printer.LANG_EN, getActivity());
                } else if (printName.equalsIgnoreCase("TM-T81")) {
                    mPrinter = new Printer(Printer.TM_T81, Printer.LANG_EN, getActivity());
                } else {
                    mPrinter = new Printer(Printer.TM_T88, Printer.LANG_EN, getActivity());
                }
            } catch (Exception e) {
                ShowMsg.showException(e, "Printer", getActivity());
                return false;
            }
            mPrinter.setReceiveEventListener(this);
            //put printer object into the table
            mPrinters.put(printIP,mPrinter);
        }
        return true;
    }
    public void finalizeObject(Printer mPrinter) {//TODO: to finalised which object?
        if (mPrinter == null) {
            return;
        }

        mPrinter.clearCommandBuffer();

        //mPrinter.setReceiveEventListener(null);

        //mPrinter = null;
    }

    public boolean connectPrinter(String receiptPrintIP) {
        boolean isBeginTransaction = false;

        //retrieve the printer object from hashtable
        Printer mPrinter = mPrinters.get(receiptPrintIP);
        if (mPrinter == null) {
            return false;
        }

        try {
            mPrinter.connect(receiptPrintIP, Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "connect", getActivity());
            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {
            ShowMsg.showException(e, "beginTransaction", getActivity());
        }

        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            }
            catch (Epos2Exception e) {
                // Do nothing
                return false;
            }
        }

        return true;
    }

    public boolean connectPrinterCheckenRoom(String printIPCheckenRoom) {
        boolean isBeginTransaction = false;
        //retrieve the printer object from hashtable
        Printer mPrinter = mPrinters.get(printIPCheckenRoom);

        if (mPrinter == null) {
            return false;
        }

        try {
            mPrinter.connect(printIPCheckenRoom, Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "connect", getActivity());
            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {
            ShowMsg.showException(e, "beginTransaction", getActivity());
        }

        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            }
            catch (Epos2Exception e) {
                // Do nothing
                return false;
            }
        }

        return true;
    }

    @Override
    public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                disconnectPrinter(printerObj);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //disconnectPrinter(printerObj);
                    }
                }).start();
            }
        });
    }

    private void disconnectPrinter(Printer printerObj) {
        if (printerObj == null) {
            return;
        }

        try {
            printerObj.endTransaction();
        } catch (final Exception e) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "endTransaction", getActivity());
                }
            });
        }

        try {
            printerObj.disconnect();
        } catch (final Exception e) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    //ShowMsg.showException(e, "disconnect", getActivity());
                }
            });
        }

        finalizeObject(printerObj);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // 58MM kitchen
    private boolean PrintOrder58MMThaiItem(final POSOrder order, ArrayList<POSOrderItem> orderItems, String printerIP) {
        String method = "";
        String spaceDate="",item_name="";
        String orderType;
        StringBuilder textData = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder qty = new StringBuilder();

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String currentTimestamp = df.format(Calendar.getInstance().getTime());
        String strOrderTable = "",spaceOrderTable="",itemName="",newStringName="",nextStringName="",nextItemName1="";
        int itemLength;String spaceQty="",spaceItem="";;
        orderType= order.getOrderingMode() == POSOrder.OrderingMode.TAKE_AWAY ? "Take Away" : "Dine-In";
        if (order.getTableUid() != null &&orderType.equalsIgnoreCase("Dine-In")) {
            POSTable selectedPOSTable = POSTable.getTable(getActivity(), order.getTableUid());
            strOrderTable = selectedPOSTable.getName();
        }

        //retrieve the printer object from hashtable
        Printer mPrinter = mPrinters.get(printerIP);

        if (mPrinter == null) {
            return false;
        }
        try {
            method = "addPageArea";
            method = "addText";
            method = "addTextSize";
            mPrinter.addTextFont(Builder.FONT_B);
            mPrinter.addTextLang(Printer.LANG_TH);
            mPrinter.addTextSize(1, 1);
            for (int i=0;i<(40-currentTimestamp.toString().trim().length())/2;i++){
                spaceDate+=" ";
            }
            textData.append(spaceDate+currentTimestamp+""+spaceDate+"\n");
            if (orderType.equalsIgnoreCase("Take Away")){
                for (int i = 0; i <40-orderType.toString().trim().length(); i++) {
                    spaceOrderTable +=" ";
                }
                textData.append(spaceOrderTable+ orderType + "\n");
            }else {
                for (int i = 0; i <40-(orderType.toString().trim().length()+strOrderTable.toString().trim().length()); i++) {
                    spaceOrderTable += " ";
                }
                textData.append(strOrderTable + spaceOrderTable+orderType + "\n");
            }
            textData.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            name.append("Name");
            method = "addText";
            mPrinter.addText(name.toString());
            method = "addHPosition";
            mPrinter.addHPosition(310);
            qty.append("Qty\t");
            method = "addText";
            mPrinter.addText(qty.toString());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);
            name.delete(0, name.length());
            qty.delete(0,qty.length());

            for (POSOrderItem orderItem : orderItems) {
                POSProduct product = POSProduct.getProduct(getActivity(), orderItem.getProductUid());
                itemName = product.getName().toString().trim().replaceAll("\\s+", " ").replaceAll("\\s+$", " ").replaceAll("\\s{2,}", " ").trim();
                if (itemName.length() > 30) {
                    item_name = itemName.trim().substring(0, 30) + "..";
                } else {
                    item_name = itemName;
                }
                method = "addTextAlign";
                mPrinter.addTextAlign(Printer.ALIGN_LEFT);
                name.append(item_name);
                method = "addText";
                mPrinter.addText(item_name.toString());
                method = "addHPosition";
                mPrinter.addHPosition(310);
                qty.append(orderItem.getQuantityOrdered() + "\t");
                method = "addText";
                mPrinter.addText(qty.toString());
                method = "addFeedLine";
                mPrinter.addFeedLine(1);
                name.delete(0, name.length());
                qty.delete(0, qty.length());
            }
            method = "addCut";
            mPrinter.addCut(Printer.CUT_FEED);

        }catch (Exception e) {
            ShowMsg.showException(e, method, getActivity());
            return false;
        }
        return true;
    }

    // kitchen 88MM
    private boolean PrintOrder80MMThaiItem(final POSOrder order, ArrayList<POSOrderItem> orderItems, String printerIP) {
        String method = "";
        String spaceDate="",spaceOrderBy="",item_name="";
        String orderType;
        StringBuilder textData = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder qty = new StringBuilder();

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String currentTimestamp = df.format(Calendar.getInstance().getTime());

        String strOrderTable = "",spaceOrderTable="",itemName="",newStringName="",nextStringName="",nextItemName1="",spaceQty="";
        int itemLength;
        orderType= order.getOrderingMode() == POSOrder.OrderingMode.TAKE_AWAY ? "Take Away" : "Dine-In";
        if (order.getTableUid()!= null &&orderType.equalsIgnoreCase("Dine-In")) {
            POSTable selectedPOSTable = POSTable.getTable(getActivity(), order.getTableUid());
            strOrderTable = selectedPOSTable.getName();
        }

        //retrieve the printer object from hashtable
        Printer mPrinter = mPrinters.get(printerIP);
        if (mPrinter == null) {
            return false;
        }
        try {
            method = "addTextFont";
            mPrinter.addTextFont(Builder.FONT_A);
            method="addTextLang";
            mPrinter.addTextLang(Printer.LANG_TH);
            method = "addTextSize";
            mPrinter.addTextSize(1,1);
            for (int i=0;i<(42-currentTimestamp.toString().trim().length())/2;i++){
                spaceDate+=" ";

            }

            textData.append(spaceDate+currentTimestamp+spaceDate+"\n");

            if (orderType.equalsIgnoreCase("Take Away")){
                for (int i = 0; i <40-orderType.toString().trim().length(); i++) {
                    spaceOrderTable+=" ";
                }
                textData.append(spaceOrderTable+ orderType + "\n");
            }else {
                for (int i = 0; i <40-(strOrderTable.toString().trim().length()+orderType.toString().trim().length()); i++) {
                    spaceOrderTable+= " ";
                }
                textData.append(strOrderTable + spaceOrderTable+orderType + "\n");
            }
            textData.append("-----------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            name.append("Name");
            method = "addText";
            mPrinter.addText(name.toString());
            method = "addHPosition";
            mPrinter.addHPosition(445);
            qty.append("Qty\t");
            method = "addText";
            mPrinter.addText(qty.toString());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);
            name.delete(0, name.length());
            qty.delete(0,qty.length());

            for (POSOrderItem orderItem : orderItems) {
                POSProduct product = POSProduct.getProduct(getActivity(), orderItem.getProductUid());
                itemName = product.getName().toString().trim().replaceAll("\\s+", " ").replaceAll("\\s+$", " ").replaceAll("\\s{2,}", " ").trim();
                if (itemName.length() > 30) {
                    item_name = itemName.trim().substring(0, 30) + "..";
                } else {
                    item_name = itemName;
                }
                method = "addTextAlign";
                mPrinter.addTextAlign(Printer.ALIGN_LEFT);
                name.append(item_name);
                method = "addText";
                mPrinter.addText(item_name.toString());
                method = "addHPosition";
                mPrinter.addHPosition(445);
                qty.append(orderItem.getQuantityOrdered() + "\t");
                method = "addText";
                mPrinter.addText(qty.toString());
                method = "addFeedLine";
                mPrinter.addFeedLine(1);
                name.delete(0, name.length());
                qty.delete(0, qty.length());
            }
            method = "addCut";
            mPrinter.addCut(Printer.CUT_FEED);


        } catch (Exception e) {
            ShowMsg.showException(e, method, getActivity());
            return false;
        }
        return true;
    }

    public static boolean isBetweenNum(int x, int lower, int upper) {
        return lower <= x && x <= upper;
    }

    static boolean isMark(char ch)
    {
        int type = Character.getType(ch);
        return type == Character.NON_SPACING_MARK ||
                type == Character.ENCLOSING_MARK ||
                type == Character.COMBINING_SPACING_MARK;
    }


    public static int getGraphemeCount(String text) {
        int count = 0;
        for(int i=0; i<text.length(); i++)
        {
            if(!isMark(text.charAt(i)))
                count++;
        }
        return count;
    }


    public static int getCount(String text) {
        int graphemeCount = 0;
        BreakIterator graphemeCounter = BreakIterator.getCharacterInstance();
        graphemeCounter.setText(text);
        while (graphemeCounter.next() != BreakIterator.DONE)
            graphemeCount++;
        return graphemeCount;
    }

   public static int graphemeLength(String str) {
        BreakIterator iter = BreakIterator.getCharacterInstance();
        iter.setText(str);

        int count = 0;
        while (iter.next() != BreakIterator.DONE) count++;

        return count;
    }
}