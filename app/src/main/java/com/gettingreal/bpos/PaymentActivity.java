package com.gettingreal.bpos;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.linedisplay.LineDisplay;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.eposprint.Builder;
import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerOrder;
import com.gettingreal.bpos.api.ServerOrderItem;
import com.gettingreal.bpos.api.ServerPostReceiptResponse;
import com.gettingreal.bpos.api.ServerReceipt;
import com.gettingreal.bpos.helper.PaymentHelper;
import com.gettingreal.bpos.helper.PrintPOS2Help;
import com.gettingreal.bpos.helper.PrinterSessionManager;
import com.gettingreal.bpos.helper.ServerStatusMenuHelper;
import com.gettingreal.bpos.helper.ShowMsg;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSReceipt;
import com.gettingreal.bpos.model.POSReceiptHeader;
import com.gettingreal.bpos.model.POSSurcharge;
import com.gettingreal.bpos.model.POSTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by ivanfoong on 17/6/14.
 */
public class PaymentActivity extends Activity implements PrintReceiptTask.OnPrintReceiptTaskCompleted{
    private TextView mTotalPayableTextView, mChangeDueTextView;
    private EditText mPaymentEditText, mDiscountDescriptionEditText, mDiscountPercentageEditText;
    private Button mMarkAsPaidButton;
    private BigDecimal mSubtotal;
    private long[] mOrderIds;

    private Menu mMenu;
    private boolean mIsServerOnline = false;
    private Printer  mPrinter = null;
    LineDisplay mLineDisplay = null;
    PrinterSessionManager printerSessionManager;
    private Context mContext;
    private PrintPOS2Help printPOS2Help;
    BigDecimal cashAmount,discountPercentage ;
    String printIP,totalAmount="";


    private BroadcastReceiver mServerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received server-status broadcast");
            mIsServerOnline = intent.getBooleanExtra("online", false);
            ServerStatusMenuHelper.updateServerStatusMenu(mMenu, mIsServerOnline);
        }
    };

    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setHomeButtonEnabled(true);
        mContext = this;
        printerSessionManager = new PrinterSessionManager(getApplicationContext());
        mTotalPayableTextView = (TextView) findViewById(R.id.text_view_total_payable);
        mChangeDueTextView = (TextView) findViewById(R.id.text_view_change_due);
        mPaymentEditText = (EditText) findViewById(R.id.edit_text_payment);
        mDiscountDescriptionEditText = (EditText) findViewById(R.id.edit_text_discount_description);
        mDiscountPercentageEditText = (EditText) findViewById(R.id.edit_text_discount_percentage);
        mMarkAsPaidButton = (Button) findViewById(R.id.button_mark_as_paid);
        printPOS2Help = new PrintPOS2Help();

        try {
            com.epson.epos2.Log.setLogSettings(mContext, com.epson.epos2.Log.PERIOD_PERMANENT, com.epson.epos2.Log.OUTPUT_STORAGE, null, 9090, 1, com.epson.epos2.Log.LOGLEVEL_LOW);


        } catch (Epos2Exception e) {
            e.printStackTrace();
        }


        mPaymentEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView aTextView, final int i, final KeyEvent aKeyEvent) {
                BigDecimal cashAmount = BigDecimal.valueOf(0.0);
                try {
                    cashAmount = BigDecimal.valueOf(Double.parseDouble(mPaymentEditText.getText().toString()));
                } catch (NumberFormatException e) {
                }
                ;

                BigDecimal discountAmount = BigDecimal.ZERO;
                try {
                    discountPercentage = BigDecimal.valueOf(Double.parseDouble(mDiscountPercentageEditText.getText().toString()));

                    if (discountPercentage.compareTo(BigDecimal.valueOf(100.0)) > 0) {
                        mDiscountPercentageEditText.setText("100");
                        discountPercentage = BigDecimal.valueOf(100.0);
                    }

                    discountAmount = mSubtotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100.0));
                } catch (NumberFormatException e) {
                }

                BigDecimal subTotalLessDiscount = mSubtotal.subtract(discountAmount);

                BigDecimal totalPayable = PaymentHelper.calculateTotalForSubTotal(aTextView.getContext(), subTotalLessDiscount);
                mTotalPayableTextView.setText(String.format("$%.2f", totalPayable.doubleValue()));

                BigDecimal changeAmount = cashAmount.subtract(totalPayable);
                String change = String.format("$%.2f", changeAmount.doubleValue());
                mChangeDueTextView.setText(change);
                Log.e("Balance", change);
                return false;
            }
        });

        mDiscountPercentageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView aTextView, final int i, final KeyEvent aKeyEvent) {
                BigDecimal cashAmount = BigDecimal.valueOf(0.0);
                try {
                    cashAmount = BigDecimal.valueOf(Double.parseDouble(mPaymentEditText.getText().toString()));
                } catch (NumberFormatException e) {
                }

                BigDecimal discountAmount = BigDecimal.ZERO;
                try {
                    BigDecimal discountPercentage = BigDecimal.valueOf(Double.parseDouble(mDiscountPercentageEditText.getText().toString()));

                    if (discountPercentage.compareTo(BigDecimal.valueOf(100.0)) > 0) {
                        mDiscountPercentageEditText.setText("100");
                        discountPercentage = BigDecimal.valueOf(100.0);
                    }
                    discountAmount = mSubtotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100.0));
                } catch (NumberFormatException e) {
                }

                BigDecimal subTotalLessDiscount = mSubtotal.subtract(discountAmount);

                BigDecimal totalPayable = PaymentHelper.calculateTotalForSubTotal(aTextView.getContext(), subTotalLessDiscount);


                mTotalPayableTextView.setText(String.format("$%.2f", totalPayable.doubleValue()));

                //BigDecimal subtotal_after_discount = totalPayable.subtract(discountAmount);
//                TODO:: Changed
                BigDecimal changeAmount = cashAmount.subtract(totalPayable);
                //BigDecimal changeAmount = cashAmount.subtract(totalPayable);
                String change = String.format("$%.2f", changeAmount.doubleValue());
                mChangeDueTextView.setText(change);
                return false;
            }
        });

        mDiscountPercentageEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View aView, boolean b) {
                BigDecimal cashAmount = BigDecimal.valueOf(0.0);
                try {
                    cashAmount = BigDecimal.valueOf(Double.parseDouble(mPaymentEditText.getText().toString()));
                } catch (NumberFormatException e) {
                }

                BigDecimal discountAmount = BigDecimal.ZERO;
                try {
                    BigDecimal discountPercentage = BigDecimal.valueOf(Double.parseDouble(mDiscountPercentageEditText.getText().toString()));

                    if (discountPercentage.compareTo(BigDecimal.valueOf(100.0)) > 0) {
                        mDiscountPercentageEditText.setText("100");
                        discountPercentage = BigDecimal.valueOf(100.0);
                    }

                    discountAmount = mSubtotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100.0));
                } catch (NumberFormatException e) {
                }

                BigDecimal subTotalLessDiscount = mSubtotal.subtract(discountAmount);

                BigDecimal totalPayable = PaymentHelper.calculateTotalForSubTotal(aView.getContext(), subTotalLessDiscount);
                mTotalPayableTextView.setText(String.format("$%.2f", totalPayable.doubleValue()));

             //   BigDecimal subtotal_after_discount = totalPayable.subtract(discountAmount);
//                TODO:: Changed
                BigDecimal changeAmount = cashAmount.subtract(totalPayable);

                //BigDecimal changeAmount = cashAmount.subtract(totalPayable);
                String change = String.format("$%.2f", changeAmount.doubleValue());
                mChangeDueTextView.setText(change);

            }
        });

        mMarkAsPaidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                BigDecimal cashAmount = BigDecimal.ZERO;
                try {
                    cashAmount = BigDecimal.valueOf(Double.parseDouble(mPaymentEditText.getText().toString())).setScale(2, BigDecimal.ROUND_FLOOR);
                    //cashAmount=cashAmount.setScale(2,BigDecimal.ROUND_FLOOR);

                } catch (NumberFormatException e) {
                }
                ;

                BigDecimal discountAmount = BigDecimal.ZERO;
                try {
                    BigDecimal discountPercentage = BigDecimal.valueOf(Double.parseDouble(mDiscountPercentageEditText.getText().toString()));

                    if (discountPercentage.compareTo(BigDecimal.valueOf(100.0)) > 0) {
                        mDiscountPercentageEditText.setText("100");
                        discountPercentage = BigDecimal.valueOf(100.0);
                    }

                    discountAmount = mSubtotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100.0));
                } catch (NumberFormatException e) {
                }

                BigDecimal subTotalLessDiscount = mSubtotal.subtract(discountAmount);
                BigDecimal totalPayable = PaymentHelper.calculateTotalForSubTotal(aView.getContext(), subTotalLessDiscount).setScale(2, BigDecimal.ROUND_FLOOR);

                if (totalPayable.compareTo(cashAmount) <= 0) {
                    payment();

                } else {
                    Toast.makeText(PaymentActivity.this, "Cash is less than total amount payable!", Toast.LENGTH_LONG).show();
                }
            }
        });

        mSubtotal = BigDecimal.valueOf(getIntent().getDoubleExtra("subtotal", 0.0));
        mOrderIds = getIntent().getLongArrayExtra("order_ids");
        BigDecimal discountAmount = BigDecimal.ZERO;
        try {
            BigDecimal discountPercentage = BigDecimal.valueOf(Double.parseDouble(mDiscountPercentageEditText.getText().toString()));
            discountAmount = mSubtotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100.0));
        } catch (NumberFormatException e) {
        }

        BigDecimal subTotalLessDiscount = mSubtotal.subtract(discountAmount);

        BigDecimal totalPayable = PaymentHelper.calculateTotalForSubTotal(this, subTotalLessDiscount);
        mTotalPayableTextView.setText(String.format("$%.2f", totalPayable.doubleValue()));

        mChangeDueTextView.setText("$0");


        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");
        ArrayList<String> strNew = new ArrayList<String>();

        if (printerSessionManager.getDisplay() != null || printerSessionManager.getDisplay() != "") {
            if (printerSessionManager.getDisplay().equalsIgnoreCase("true")) {
//                if (address.contains("/")) {
//                    StringTokenizer server_ip = new StringTokenizer(address, "/");
//                    while (server_ip.hasMoreTokens()) {
//                        strNew.add(server_ip.nextToken());
//                    }
//                    runLineDisplaySequence(strNew.get(0), mTotalPayableTextView.getText().toString());
//                } else {
//                    runLineDisplaySequence(address, mTotalPayableTextView.getText().toString());
//                }

//        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
//        String address = settings.getString("address", "192.168.192.168");
//        ArrayList<String> strNew = new ArrayList<String>();

                boolean haveNumber = PrintPOS2Help.containsDigitPinterStr(printerSessionManager.getPrintURL());
                if (printerSessionManager.getPrintURL() != null || haveNumber) {
                    StringTokenizer namEIP = new StringTokenizer(printerSessionManager.getPrintURL().toUpperCase(), ",");
                    ArrayList<String> strNew2 = new ArrayList<String>();
                    while (namEIP.hasMoreTokens()) {
                        strNew2.add(namEIP.nextToken());
                    }
                    printIP = strNew2.get(0);

                } else {
                    printIP = "TCP:192.168.192.168";
                }

//                if (printerSessionManager.getDisplay().equals("true")) {
//            if (printIP.contains("/")) {

//                runLineDisplaySequence(strNew.get(0), mTotalPayableTextView.getText().toString());
//            } else {
//                Log.e(" Printer IP ", printIP);
                    String[] parts = printIP.split(":");
                    String part2 = parts[1]; // 192.168.1.158

                    if(!part2.equals("") || !part2.equals("null") || part2 != null) {
                        runLineDisplaySequence(part2, mTotalPayableTextView.getText().toString());
                    }else{
                        StringTokenizer server_ip = new StringTokenizer(address, "/");
                        while (server_ip.hasMoreTokens()) {
                            strNew.add(server_ip.nextToken());
                            runLineDisplaySequence(strNew.get(0), mTotalPayableTextView.getText().toString());
                        }
                    }
                    //           }
 //               }

//        boolean haveNumber= PrintPOS2Help.containsDigitPinterStr(printerSessionManager.getPrintURL());
//        if(printerSessionManager.getPrintURL() != null || haveNumber){
//            StringTokenizer namEIP=new StringTokenizer(printerSessionManager.getPrintURL().toUpperCase(),",");
//            ArrayList<String> strNew=new ArrayList<String>();
//            while (namEIP.hasMoreTokens()){
//                strNew.add(namEIP.nextToken());
//            }
//            printIP=strNew.get(0);
//            Log.e("printIIII", printIP);
//            runLineDisplaySequence(printIP, mTotalPayableTextView.getText().toString());
//        }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServerStatusBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.status_activity_actions, menu);
        mMenu = menu;
        return true;
    }

    public void payment() {

        BigDecimal cashAmount = BigDecimal.valueOf(0.0);
        if (!mPaymentEditText.getText().toString().equals("")) {
            cashAmount = BigDecimal.valueOf(Double.parseDouble(mPaymentEditText.getText().toString()));
        }

        BigDecimal discountAmount = BigDecimal.valueOf(0.0);
        String discountDescription = "";
        if (!mDiscountPercentageEditText.getText().toString().equals("")) {
            BigDecimal discountPercentage = BigDecimal.valueOf(Double.parseDouble(mDiscountPercentageEditText.getText().toString()));
            discountAmount = mSubtotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100.0));
            discountDescription = String.format("%s @ %f%%", mDiscountDescriptionEditText.getText().toString(), discountPercentage);
        }

        BigDecimal subTotalLessDiscount = mSubtotal.subtract(discountAmount);

        BigDecimal finalAmount = PaymentHelper.calculateTotalForSubTotal(this, subTotalLessDiscount);

        Long[] orderIds = new Long[mOrderIds.length];
        for (int i = 0; i < mOrderIds.length; i++) {
            orderIds[i] = mOrderIds[i];
        }

        ServerReceipt serverReceipt = new ServerReceipt();
        serverReceipt.paid_at = new Date();
        serverReceipt.paid_amount = cashAmount;
        serverReceipt.discount_amount = discountAmount;
        serverReceipt.discount_description = discountDescription;
        serverReceipt.final_amount = finalAmount;
        serverReceipt.order_ids = orderIds;

        SharedPreferences settings = getSharedPreferences(getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");
        String serverUrl = "http://" + address + "/";

        if(mIsServerOnline){

            ServerAPIClient serverAPIClient = new ServerAPIClient(serverUrl);
            serverAPIClient.createReceipt(serverReceipt, new Callback<ServerPostReceiptResponse>() {
                @Override
                public void success(final ServerPostReceiptResponse aServerPostReceiptResponse, final Response aResponse) {
                    for (int i = 0; i < aServerPostReceiptResponse.receipts.length; i++) {
                        ServerReceipt serverReceipt = aServerPostReceiptResponse.receipts[i];

                        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();

                        for (int j = 0; j < serverReceipt.orders.length; j++) {
                            ServerOrder serverOrder = serverReceipt.orders[j];

                            ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();
                            for (int k = 0; k < serverOrder.order_items.length; k++) {
                                ServerOrderItem serverOrderItem = serverOrder.order_items[k];
                                POSOrderItem orderItem = new POSOrderItem(serverOrderItem.order_id, serverOrderItem.product_uid, serverOrderItem.quantity_ordered, serverOrderItem.quantity_served);
                                orderItems.add(orderItem);
                            }

                            POSOrder order = new POSOrder(serverOrder.id, serverOrder.table_uid, serverOrder.receipt_id, serverOrder.order_at, POSOrder.getOrderingMode(serverOrder.ordering_mode), orderItems);
                            orders.add(order);
                        }

                        POSReceipt receipt = new POSReceipt(serverReceipt.id, serverReceipt.discount_description, serverReceipt.paid_amount, serverReceipt.discount_amount, serverReceipt.final_amount, serverReceipt.paid_at, orders);
                        new PrintReceiptTask(PaymentActivity.this, receipt, PaymentActivity.this).execute();
                        runPrintReceiptSequence(receipt);
                    }
                }

                @Override
                public void failure(final RetrofitError aRetrofitError) {
                    aRetrofitError.printStackTrace();
                   // Toast.makeText(getApplicationContext(), "Server can't Create Receipt", Toast.LENGTH_SHORT).show();
                    Log.e("PaymentResponseError", aRetrofitError.toString());
                }
            });

            ArrayList<String> strNew = new ArrayList<String>();
            if (printerSessionManager.getDisplay()!=null||printerSessionManager.getDisplay()!=""){
                if (printerSessionManager.getDisplay().equalsIgnoreCase("true")) {
                    if (address.contains("/")) {
                        StringTokenizer server_ip = new StringTokenizer(address, "/");
                        while (server_ip.hasMoreTokens()) {
                            strNew.add(server_ip.nextToken());
                        }
                        runClearLineDisplaySequence(strNew.get(0));
                    } else {
                        runClearLineDisplaySequence(address);
                    }
                }
            }
        }else{
            // offline receipt
            ArrayList<POSOrder> orders = new ArrayList<POSOrder>();

            for (int j = 0; j < mOrderIds.length; j++) {
                POSOrder order_ = POSOrder.getOrder(mContext, mOrderIds[j]);
                orders.add(order_);
            }

            Long default_id = 0L;
            POSReceipt receipt = new POSReceipt(default_id, serverReceipt.discount_description, serverReceipt.paid_amount, serverReceipt.discount_amount, serverReceipt.final_amount, serverReceipt.paid_at, orders);

            runPrintReceiptSequence(receipt);

            for (int j = 0; j < mOrderIds.length; j++) {
                POSOrder order_ = POSOrder.getOrder(mContext, mOrderIds[j]);
                receipt.OrderUpdate(mContext, order_);
            }


            Intent i = new Intent(this, MenuActivity.class);
            startActivity(i);
            finish();
        }
    }

    @Override
    public void onPrintReceiptTaskCompleted(final String errorMessage) {

        if (errorMessage != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
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
            Intent intent = new Intent("orders-invalidated");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            finish();
        }
    }

    private boolean runPrintReceiptSequence(final POSReceipt receipt) {
        //Min Thein Win
        String printName="";
        boolean haveNumber= PrintPOS2Help.containsDigitPinterStr(printerSessionManager.getPrintURL());
        if (printerSessionManager.getPrintURL() != null|| haveNumber){
            StringTokenizer namEIP=new StringTokenizer(printerSessionManager.getPrintURL().toUpperCase(),",");
            ArrayList<String> strNew=new ArrayList<String>();
            while (namEIP.hasMoreTokens()){
                strNew.add(namEIP.nextToken());
            }
            printIP=strNew.get(0);
            Log.e("printIIII", printIP);
            printName=strNew.get(1);

        }else {
            //set Default Printe Model and IP;
            printIP="TCP:192.168.192.168";
            printName="TM-T88V";
        }

        if (!initializeObject(printerSessionManager.getPrintURL().toUpperCase())) {
            return false;
        }

            Boolean t88v = printName.equalsIgnoreCase("TM-T88V");
            if (printerSessionManager.getCashierPrintSize()!=null) {
                if (printerSessionManager.getCashierPrintSize().equalsIgnoreCase("88MM")) {
                    if (!ThaiReceiptPrint80MMForm(receipt, t88v)) {
                        finalizeObject();
                        return false;
                    }
                } else if (printerSessionManager.getCashierPrintSize().equalsIgnoreCase("58MM")) {

                    if (!ThaiReiptPrintPaper58MMForm(receipt, t88v)) {
                        finalizeObject();
                        return false;
                    }
                }
            }else {

                Toast.makeText(mContext.getApplicationContext(),"Please Choice Receipt PrintPaper Size",Toast.LENGTH_LONG).show();
            }

        if (!printData(printIP)) {
            finalizeObject();
            return false;
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
        try {
            if (printName.equalsIgnoreCase("TM-T88V")){
                mPrinter = new Printer(Printer.TM_T88,Printer.LANG_EN,mContext);
            }else if (printName.equalsIgnoreCase("TM-T82")){
                mPrinter = new Printer(Printer.TM_T88,Printer.LANG_EN,mContext);
            }else if (printName.equalsIgnoreCase("TM-T81")){
                mPrinter = new Printer(Printer.TM_T81,Printer.LANG_EN,mContext);
            }else {
                mPrinter = new Printer(Printer.TM_T88,Printer.LANG_EN,mContext);
            }
        }
        catch (Exception e) {
            ShowMsg.showException(e, "Printer", mContext);
            Log.e("LanguageError",e.toString());
            return false;
        }

        mPrinter.setReceiveEventListener(new com.epson.epos2.printer.ReceiveListener() {
            /*
            *Min Thein Win...
            */
            @Override
            public void onPtrReceive(Printer printer, int i, PrinterStatusInfo printerStatusInfo, String s) {

                runOnUiThread(new Runnable() {
                    @Override
                    public synchronized void run() {
                        disconnectPrinter();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                disconnectPrinter();
                            }
                        }).start();
                    }
                });
            }
        });

        if (printerSessionManager.getCashierDrawer()!=null) {
            if (printerSessionManager.getCashierDrawer().equalsIgnoreCase("true")) {
                try {

                    mPrinter.addPulse(Printer.DRAWER_HIGH, Printer.PULSE_100);
                    mPrinter.sendData(Printer.PARAM_DEFAULT);
                } catch (Epos2Exception e) {

                }
            }
        }
        return true;
    }

    public void finalizeObject() {
        if (mPrinter == null) {
            return;
        }

        mPrinter.clearCommandBuffer();

        mPrinter.setReceiveEventListener(null);
        mPrinter = null;

    }

    public boolean printData(String receiptPrintIP) {
        if (mPrinter == null) {
            return false;
        }

        if (!connectPrinter(receiptPrintIP)) {
            return false;
        }

        PrinterStatusInfo status = mPrinter.getStatus();
        if (!isPrintable(status)) {
            ShowMsg.showMsg(printPOS2Help.makeErrorMessage(status), mContext);
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
            ShowMsg.showException(e, "sendData", mContext);
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

    public boolean connectPrinter(String receiptPrintIP) {
        boolean isBeginTransaction = false;
        if (mPrinter == null) {
            return false;
        }
        try {
            mPrinter.connect(receiptPrintIP, Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            //  ShowMsg.showException(e, "connect", mContext);
            Log.e("LanguageError1",  ShowMsg.getEposExceptionText(((Epos2Exception) e).getErrorStatus()));
            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {
            ShowMsg.showException(e, "beginTransaction", mContext);
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
    private void disconnectPrinter() {
        if (mPrinter == null) {
            return;
        }

        try {
            mPrinter.endTransaction();
        } catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "endTransaction", mContext);
                }
            });
        }

        try {
            mPrinter.disconnect();
        } catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "disconnect", mContext);
                }
            });
        }

        finalizeObject();
    }

    private boolean runLineDisplaySequence(String display_ip,String totalAmount) {
        if (!initializeDisplayObject()) {
            return false;
        }

        if (!createDisplayData(totalAmount)) {
            finalizeDisplayObject();
            return false;
        }

        if (!connectDisplay(display_ip)) {
            finalizeDisplayObject();
            return false;
        }

        try {
            mLineDisplay.sendData();
        }
        catch (Exception e) {
            disconnectDisplay();
            return false;
        }

        return true;
    }

    private boolean initializeDisplayObject() {
        try {
            //mLineDisplay = new LineDisplay(LineDisplay.DM_D110, mContext);
            mLineDisplay = new LineDisplay(LineDisplay.DM_D30, mContext);
        }
        catch (Exception e) {
            Log.e("sendData", e + "");
            return false;
        }

        mLineDisplay.setReceiveEventListener(new com.epson.epos2.linedisplay.ReceiveListener() {
            @Override
            public void onDispReceive(LineDisplay lineDisplay, int i) {
                runOnUiThread(new Runnable() {
                    @Override
                    public synchronized void run() {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                disconnectDisplay();
                            }
                        }).start();
                    }
                });
            }
        });

        return true;
    }

    private boolean connectDisplay(String display_ip) {
        if (mLineDisplay == null) {
            return false;
        }

        try {
            //mLineDisplay.connect("TCP:"+display_ip+"[local_display]", LineDisplay.PARAM_DEFAULT);
            mLineDisplay.connect("TCP:"+display_ip, LineDisplay.PARAM_DEFAULT);
        }
        catch (Epos2Exception e) {
            ShowMsg.showException(e, "Display Connect", mContext);
            return false;
        }

        return true;
    }

    private void finalizeDisplayObject() {
        if (mLineDisplay == null) {
            return;
        }

        mLineDisplay.clearCommandBuffer();

        mLineDisplay.setReceiveEventListener(null);

        mLineDisplay = null;
    }

    private boolean createDisplayData(String amountBalance) {
        String method = "";
        if (mLineDisplay == null) {
            return false;
        }

        try {
            method = "addInitialize";
            mLineDisplay.addInitialize();

            method = "addSetCursorPosition";
            mLineDisplay.addSetCursorPosition(1, 1);

            method = "addText";
            mLineDisplay.addText("TOTAL: = " + amountBalance);
        }
        catch (Exception e) {
            ShowMsg.showException(e, method, mContext);
            return false;
        }

        return true;
    }

    private void disconnectDisplay() {
        if (mLineDisplay == null) {
            return;
        }

        try {
            mLineDisplay.clearCommandBuffer();
            mLineDisplay.disconnect();
        }
        catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "disconnect", mContext);
                }
            });
        }

        finalizeObject();
    }


    private boolean runClearLineDisplaySequence(String display_ip) {
        if (!initializeDisplayObject()) {
            return false;
        }

        if (!createClearDisplayData()) {
            finalizeDisplayObject();
            return false;
        }

        if (!connectDisplay(display_ip)) {
            finalizeDisplayObject();
            return false;
        }

        try {
            mLineDisplay.sendData();
        }
        catch (Exception e) {
            disconnectDisplay();
            return false;
        }

        return true;
    }
    private boolean createClearDisplayData() {
        String method = "";
        if (mLineDisplay == null) {
            return false;
        }

        try {
            method = "addInitialize";
            mLineDisplay.addInitialize();

            method = "addSetCursorPosition";
            mLineDisplay.addSetCursorPosition(1, 1);

            method = "addText";
            mLineDisplay.addText(" ");
        }
        catch (Exception e) {
            ShowMsg.showException(e, method, mContext);
            return false;
        }

        return true;
    }

    private String leftPaddingWithSpaces(String str, int num) {
        return String.format("%1$" + num + "s", str);
    }

    // receipt tm82 58MM #addHPosition
    private boolean ThaiReiptPrintPaper58MMForm(final POSReceipt receipt, final boolean t88v) {

        String strHeader0 = "", strHeader1 = "", strHeader2 = "", strHeader3 = "", strHeader4 = "";
        String spcHeader0 = "", spcHeader1 = "", spcHeader2 = "", spcHeader3 = "", spcHeader4 = "", spaceDate = "", spcChangeAmt = " ";
        String method = "";
        String thankY = "";
        StringBuilder textData = new StringBuilder();
        StringBuilder dot = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder qty = new StringBuilder();
        StringBuilder price = new StringBuilder();
        StringBuilder date = new StringBuilder();
        StringBuilder receipt_id = new StringBuilder();
        StringBuilder table_id = new StringBuilder();
        StringBuilder order_mode = new StringBuilder();

        String spaceColum = "   ";
        String spacetotalPrice = " ";
        String itemName = "", space1 = "", space2 = "", tableName = "", spcDiscount = "", spcCashpay = "",next_itemName="",newStringName="",nextStringName1="";
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String currentTimestamp = df.format(Calendar.getInstance().getTime());
        Hashtable<String, POSProduct> productsLookupTable = new Hashtable<String, POSProduct>();
        Hashtable<String, Integer> orderProductQuantity = new Hashtable<String, Integer>();
        ArrayList<POSOrderItem> totalOrderItems = new ArrayList<POSOrderItem>();

        StringBuilder subTotal = new StringBuilder();
        StringBuilder subTotalValue = new StringBuilder();

        StringBuilder discount = new StringBuilder();
        StringBuilder discount_value = new StringBuilder();

        StringBuilder svc = new StringBuilder();
        StringBuilder svc_value = new StringBuilder();

        StringBuilder gst = new StringBuilder();
        StringBuilder gst_value = new StringBuilder();

        StringBuilder total = new StringBuilder();
        StringBuilder total_value = new StringBuilder();

        StringBuilder cash = new StringBuilder();
        StringBuilder cash_value = new StringBuilder();

        StringBuilder change = new StringBuilder();
        StringBuilder change_value = new StringBuilder();

        if (mPrinter == null) {
            return false;
        }
        try {

            method = "addTextFont";
            mPrinter.addTextFont(Builder.FONT_B);

            method = "addTextLang";
            mPrinter.addTextLang(Printer.LANG_TH);

            int headerIndex = 0;
            for (POSReceiptHeader receiptHeader : POSReceiptHeader.getAllReceiptHeaders(getApplicationContext())) {
                if (headerIndex == 0) {
                    strHeader0 = receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                } else if (headerIndex == 1) {
                    strHeader1 = receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                } else if (headerIndex == 2) {
                    strHeader2 = receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                } else if (headerIndex == 3) {
                    strHeader3 = receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                } else if (headerIndex == 4) {
                    strHeader4 = receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                }
                headerIndex++;
            }

            for (int i = 0; i < (42 - strHeader0.length()) / 2; i++) {
                spcHeader0 += " ";
            }
            textData.append(spcHeader0 + strHeader0 + spcHeader0 + "\n");

            for (int i = 0; i < (42 - strHeader1.length()) / 2; i++) {
                spcHeader1 += " ";
            }
            textData.append(spcHeader1 + strHeader1 + spcHeader1 + "\n");

            for (int i = 0; i < (42 - strHeader2.length()) / 2; i++) {
                spcHeader2 += " ";
            }
            textData.append(spcHeader2 + strHeader2 + spcHeader2 + "\n");
            for (int i = 0; i < (42 - strHeader3.length()) / 2; i++) {
                spcHeader3 += " ";
            }
            textData.append(spcHeader3 + strHeader3 + spcHeader3 + "\n");

            for (int i = 0; i < (42 - strHeader4.length()) / 2; i++) {
                spcHeader4 += " ";
            }
            textData.append(spcHeader4 + strHeader4 + spcHeader4 + "\n");

            if (receipt.getOrders().get(0).getTableUid() != null) {
                POSTable selectedPOSTable = POSTable.getTable(getApplicationContext(), receipt.getOrders().get(0).getTableUid());
                if (selectedPOSTable != null) {
                    tableName = selectedPOSTable.getName();
                }
            }
            for (int i = 0; i < 39 - tableName.toString().trim().length(); i++) {
                space1 += " ";
            }

            textData.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            date.append(currentTimestamp.toString());
            method = "addText";
            mPrinter.addText(date.toString());
            date.delete(0,total.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }
            //use order ID instead
            receipt_id.append("# " + mOrderIds[0]);
            method = "addText";
            mPrinter.addText(leftPaddingWithSpaces(receipt_id.toString(), 10));
            receipt_id.delete(0,receipt_id.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            String orderingMode = (receipt.getOrders().get(0).getOrderingMode() == POSOrder.OrderingMode.TAKE_AWAY ? "Take Away" : "Dine-In");
            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            if (orderingMode.equalsIgnoreCase("Take Away")) {
                table_id.append("");
            }else{
                table_id.append(tableName.toString());
            }
            method = "addText";
            mPrinter.addText(table_id.toString());
            table_id.delete(0,table_id.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }
            order_mode.append(leftPaddingWithSpaces(orderingMode, 10));
            method = "addText";
            mPrinter.addText(order_mode.toString());
            order_mode.delete(0,order_mode.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

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
            mPrinter.addHPosition(240);
            qty.append("Qty");
            method = "addText";
            mPrinter.addText(qty.toString());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }
            price.append(leftPaddingWithSpaces("Price", 10));
            method = "addText";
            mPrinter.addText(price.toString());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);
            name.delete(0, name.length());
            qty.delete(0,qty.length());
            price.delete(0,price.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            for (POSOrder order : receipt.getOrders()) {
                for (POSOrderItem item : order.getOrderItems()) {
                    totalOrderItems.add(item);
                    if (!productsLookupTable.containsKey(item.getProductUid())) {
                        POSProduct product = POSProduct.getProduct(mContext, item.getProductUid());
                        productsLookupTable.put(item.getProductUid(), product);
                    }

                    if (!orderProductQuantity.containsKey(item.getProductUid())) {
                        orderProductQuantity.put(item.getProductUid(), item.getQuantityOrdered());
                    } else {
                        Integer existingQuantity = orderProductQuantity.get(item.getProductUid());
                        Integer newQuantity = existingQuantity + item.getQuantityOrdered();
                        orderProductQuantity.put(item.getProductUid(), newQuantity);
                    }
                }
            }
            BigDecimal subtotal = PaymentHelper.calculateSubTotalForOrder(getApplicationContext(), totalOrderItems);
//            subtotal = subtotal.subtract(receipt.getDiscountAmount());
            BigDecimal totalAmount = PaymentHelper.calculateTotalForSubTotal(getApplicationContext(), subtotal);
            BigDecimal totalForCalculation = subtotal;
            for (int i = 0; i < totalOrderItems.size(); i++) {
                POSOrderItem orderItem = totalOrderItems.get(i);
                POSProduct product = POSProduct.getProduct(mContext, orderItem.getProductUid());

                String product3 = product.getName().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                String product_3 = "";
                if(product3.length() > 26){
                    product_3 = product3.trim().substring(0,26) + "..";
                }else{
                    product_3 = product3;
                }
                name.append(product_3);
                method = "addText";
                mPrinter.addText(name.toString());
                method = "addHPosition";
                mPrinter.addHPosition(240);
                qty.append(leftPaddingWithSpaces(String.valueOf(orderItem.getQuantityOrdered()), 3));
                method = "addText";
                mPrinter.addText(qty.toString());
                method = "addHPosition";
                if(t88v) {
                    mPrinter.addHPosition(290);
                }else{
                    mPrinter.addHPosition(270);
                }
                price.append(leftPaddingWithSpaces("$" + product.getPrice() + "0", 10));
                method = "addText";
                mPrinter.addText(price.toString());
                method = "addFeedLine";
                mPrinter.addFeedLine(1);
                name.delete(0, name.length());
                qty.delete(0,qty.length());
                price.delete(0,price.length());
            }
            textData.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            subTotal.append("SUB-TOTAL");
            method = "addText";
            mPrinter.addText(subTotal.toString());
            subTotal.delete(0,subTotal.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }
            subTotalValue.append(leftPaddingWithSpaces("$" + String.format("%.2f", Double.parseDouble(totalForCalculation.toString())), 10));
            method = "addText";
            mPrinter.addText(subTotalValue.toString());
            subTotalValue.delete(0,subTotalValue.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            discount.append("Discount("+ mDiscountPercentageEditText.getText().toString() + "%)");
            method = "addText";
            mPrinter.addText(discount.toString());
            discount.delete(0,discount.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }
            discount_value.append(leftPaddingWithSpaces("$" + String.format("-%.2f", receipt.getDiscountAmount()), 10));
            method = "addText";
            mPrinter.addText(discount_value.toString());
            discount_value.delete(0,discount_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            for (POSSurcharge surcharge : POSSurcharge.getAllSurcharges(getApplicationContext())) {
                String spc = "";
                BigDecimal subtotal_after_discount = totalForCalculation.subtract(receipt.getDiscountAmount());
//                TODO:: tax after discount
                BigDecimal surchargeAmount = subtotal_after_discount.multiply(BigDecimal.valueOf(surcharge.getPercentage() / 100.0));
                for (int i = 0; i < 41 - (surcharge.getName().toString().trim().length() + String.format("%.2f", surchargeAmount).length()); i++) {
                    spc += " ";
                }

                method = "addTextAlign";
                mPrinter.addTextAlign(Printer.ALIGN_LEFT);
                gst.append(surcharge.getName().toString().trim());
                method = "addText";
                mPrinter.addText(gst.toString());
                gst.delete(0,gst.length());
                method = "addHPosition";
                if(t88v) {
                    mPrinter.addHPosition(290);
                }else{
                    mPrinter.addHPosition(270);
                }
                gst_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", surchargeAmount), 10));
                method = "addText";
                mPrinter.addText(gst_value.toString());
                gst_value.delete(0,gst_value.length());
                method = "addFeedLine";
                mPrinter.addFeedLine(1);
                totalForCalculation = totalForCalculation.add(surchargeAmount);
                spc = "";
            }
            textData.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            total.append("TOTAL");
            method = "addText";
            mPrinter.addText(total.toString());
            total.delete(0,total.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }

            BigDecimal subtotal_after_discount = totalForCalculation.subtract(receipt.getDiscountAmount());

            total_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", Double.parseDouble(subtotal_after_discount.toString())), 10));
            method = "addText";
            mPrinter.addText(total_value.toString());
            total_value.delete(0,total_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            cash.append("CASH");
            method = "addText";
            mPrinter.addText(cash.toString());
            cash.delete(0,cash.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }
            cash_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", receipt.getPaidAmount()).toString().trim(), 10));
            method = "addText";
            mPrinter.addText(cash_value.toString());
            cash_value.delete(0,cash_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            change.append("CHANGE");
            method = "addText";
            mPrinter.addText(change.toString());
            change.delete(0,change.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(290);
            }else{
                mPrinter.addHPosition(270);
            }
            String grandtotal = mTotalPayableTextView.getText().toString();
            String final_grandtotal = grandtotal.substring(1);
            BigDecimal grand_total = new BigDecimal(final_grandtotal);
            change_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", receipt.getPaidAmount().subtract(grand_total)), 10));
            method = "addText";
            mPrinter.addText(change_value.toString());
            change_value.delete(0,change_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);
            dot.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(dot.toString());
            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);

            textData.append("                THANK YOU!                \n");
            method = "addText";
            mPrinter.addText(textData.toString());
            method = "addCut";
            mPrinter.addCut(Printer.CUT_FEED);
        }catch (Exception e) {
            ShowMsg.showException(e, method, mContext);
            return false;
        }
        return true;
    }

    //receipt tm82 88MM
    private boolean ThaiReceiptPrint80MMForm(final POSReceipt receipt, Boolean t88v) {
        String strHeader0="",strHeader1="",strHeader2="",strHeader3="",strHeader4="";
        String spcHeader0="",spcHeader1="",spcHeader2="",spcHeader3="",spcHeader4="",spaceDate="",spcChangeAmt=" ";
        String method = "";
        StringBuilder textData = new StringBuilder();
        StringBuilder dot = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder qty = new StringBuilder();
        StringBuilder price = new StringBuilder();

        StringBuilder date = new StringBuilder();
        StringBuilder receipt_id = new StringBuilder();
        StringBuilder table_id = new StringBuilder();
        StringBuilder order_mode = new StringBuilder();

        String tableName="";

        StringBuilder subTotal = new StringBuilder();
        StringBuilder subTotalValue = new StringBuilder();

        StringBuilder discount = new StringBuilder();
        StringBuilder discount_value = new StringBuilder();

        StringBuilder svc = new StringBuilder();
        StringBuilder svc_value = new StringBuilder();

        StringBuilder gst = new StringBuilder();
        StringBuilder gst_value = new StringBuilder();

        StringBuilder total = new StringBuilder();
        StringBuilder total_value = new StringBuilder();

        StringBuilder cash = new StringBuilder();
        StringBuilder cash_value = new StringBuilder();

        StringBuilder change = new StringBuilder();
        StringBuilder change_value = new StringBuilder();

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String currentTimestamp = df.format(Calendar.getInstance().getTime());
        Hashtable<String, POSProduct> productsLookupTable = new Hashtable<String, POSProduct>();
        Hashtable<String, Integer> orderProductQuantity = new Hashtable<String, Integer>();
        ArrayList<POSOrderItem> totalOrderItems = new ArrayList<POSOrderItem>();
        if (mPrinter == null) {
            return false;
        }
        try {
            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_CENTER);

            method = "addTextFont";
            mPrinter.addTextFont(Builder.FONT_A);

            method="addTextLang";
            mPrinter.addTextLang(Printer.LANG_TH);

            method = "addTextSize";
            mPrinter.addTextSize(1,1);
            int headerIndex = 0;

            for (POSReceiptHeader receiptHeader : POSReceiptHeader.getAllReceiptHeaders(getApplicationContext())) {
                if (headerIndex==0) {
                    strHeader0=receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                }else if (headerIndex==1) {
                    strHeader1=receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                }else if (headerIndex==2) {
                    strHeader2=receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                }else if(headerIndex==3) {
                    strHeader3=receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                }else if(headerIndex==4) {
                    strHeader4=receiptHeader.getContent().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                }
                headerIndex++;
            }

            textData.append(strHeader0+"\n");
            textData.append(strHeader1+"\n");
            textData.append(strHeader2+"\n");
            textData.append(strHeader3+"\n");
            textData.append(strHeader4+"\n");

            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0, textData.length());

            method="addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            method = "addTextFont";
            mPrinter.addTextFont(Builder.FONT_A);
            method="addTextLang";
            mPrinter.addTextLang(Printer.LANG_TH);

            if (receipt.getOrders().get(0).getTableUid() != null) {
                POSTable selectedPOSTable = POSTable.getTable(getApplicationContext(), receipt.getOrders().get(0).getTableUid());
                if (selectedPOSTable!=null) {
                    tableName = selectedPOSTable.getName();
                }
            }

            textData.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            date.append(currentTimestamp.toString());
            method = "addText";
            mPrinter.addText(date.toString());
            date.delete(0,total.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }
            receipt_id.append("# " + mOrderIds[0]);
            method = "addText";
            mPrinter.addText(leftPaddingWithSpaces(receipt_id.toString(), 10));
            receipt_id.delete(0,receipt_id.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            String orderingMode = (receipt.getOrders().get(0).getOrderingMode() == POSOrder.OrderingMode.TAKE_AWAY ? "Take Away" : "Dine-In");
            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            if (orderingMode.equalsIgnoreCase("Take Away")) {
                table_id.append("");
            }else{
                table_id.append(tableName.toString());
            }
            method = "addText";
            mPrinter.addText(table_id.toString());
            table_id.delete(0,table_id.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }
            order_mode.append(leftPaddingWithSpaces(orderingMode, 10));
            method = "addText";
            mPrinter.addText(order_mode.toString());
            order_mode.delete(0,order_mode.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);
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
            if(t88v) {
                mPrinter.addHPosition(330);
            }else{
                mPrinter.addHPosition(300);
            }
            qty.append("Qty");
            method = "addText";
            mPrinter.addText(qty.toString());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }
            price.append(leftPaddingWithSpaces("Price", 10));
            method = "addText";
            mPrinter.addText(price.toString());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);
            name.delete(0, name.length());
            qty.delete(0,qty.length());
            price.delete(0,price.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            for (POSOrder order : receipt.getOrders()) {
                for (POSOrderItem item : order.getOrderItems()) {
                    totalOrderItems.add(item);
                    if (!productsLookupTable.containsKey(item.getProductUid())) {
                        POSProduct product = POSProduct.getProduct(mContext, item.getProductUid());
                        productsLookupTable.put(item.getProductUid(), product);
                    }

                    if (!orderProductQuantity.containsKey(item.getProductUid())) {
                        orderProductQuantity.put(item.getProductUid(), item.getQuantityOrdered());
                    } else {
                        Integer existingQuantity = orderProductQuantity.get(item.getProductUid());
                        Integer newQuantity = existingQuantity + item.getQuantityOrdered();
                        orderProductQuantity.put(item.getProductUid(), newQuantity);
                    }
                }
            }

            BigDecimal subtotal = PaymentHelper.calculateSubTotalForOrder(getApplicationContext(), totalOrderItems);
  //          BigDecimal subtotal_after_discount = subtotal.subtract(receipt.getDiscountAmount());

            BigDecimal totalAmount = PaymentHelper.calculateTotalForSubTotal(getApplicationContext(), subtotal);
            BigDecimal totalForCalculation = subtotal;

            for (int i=0;i<totalOrderItems.size();i++){
                POSOrderItem orderItem=totalOrderItems.get(i);
                POSProduct product = POSProduct.getProduct(mContext, orderItem.getProductUid());

                String product3 = product.getName().toString().trim().replaceAll("\\s+$", "").replaceAll("\\s{2,}", " ").trim();
                String product_3 = "";
                if(product3.length() > 26){
                    product_3 = product3.trim().substring(0,26) + "..";
                }else{
                    product_3 = product3;
                }
                name.append(product_3);
                method = "addText";
                mPrinter.addText(name.toString());
                method = "addHPosition";
                if(t88v) {
                    mPrinter.addHPosition(330);
                }else{
                    mPrinter.addHPosition(300);
                }
                qty.append(leftPaddingWithSpaces(String.valueOf(orderItem.getQuantityOrdered()), 3));
                method = "addText";
                mPrinter.addText(qty.toString());
                method = "addHPosition";
                if(t88v) {
                    mPrinter.addHPosition(380);
                }else{
                    mPrinter.addHPosition(350);
                }
                price.append(leftPaddingWithSpaces("$" + product.getPrice() + "0", 10));
                method = "addText";
                mPrinter.addText(price.toString());
                method = "addFeedLine";
                mPrinter.addFeedLine(1);
                name.delete(0, name.length());
                qty.delete(0,qty.length());
                price.delete(0,price.length());

            }
            textData.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            subTotal.append("SUB-TOTAL");
            method = "addText";
            mPrinter.addText(subTotal.toString());
            subTotal.delete(0,subTotal.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }
            subTotalValue.append(leftPaddingWithSpaces("$" + String.format("%.2f", Double.parseDouble(totalForCalculation.toString())), 10));
            method = "addText";
            mPrinter.addText(subTotalValue.toString());
            subTotalValue.delete(0,subTotalValue.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            discount.append("Discount("+ mDiscountPercentageEditText.getText().toString() + "%)");
            method = "addText";
            mPrinter.addText(discount.toString());
            discount.delete(0,discount.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }
            discount_value.append(leftPaddingWithSpaces("$" + String.format("-%.2f", receipt.getDiscountAmount()), 10));
            method = "addText";
            mPrinter.addText(discount_value.toString());
            discount_value.delete(0,discount_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            for (POSSurcharge surcharge : POSSurcharge.getAllSurcharges(getApplicationContext())) {
                String spc = "";
                BigDecimal subtotal_after_discount = totalForCalculation.subtract(receipt.getDiscountAmount());
                // TODO:: subtotal after discount for tax
                BigDecimal surchargeAmount = subtotal_after_discount.multiply(BigDecimal.valueOf(surcharge.getPercentage() / 100.0));
                for (int i = 0; i < 41 - (surcharge.getName().toString().length() + String.format("%.2f", surchargeAmount).length()); i++) {
                    spc += " ";
                }
                method = "addTextAlign";
                mPrinter.addTextAlign(Printer.ALIGN_LEFT);
                gst.append(surcharge.getName().toString().trim());
                method = "addText";
                mPrinter.addText(gst.toString());
                gst.delete(0,gst.length());
                method = "addHPosition";
                if(t88v) {
                    mPrinter.addHPosition(380);
                }else{
                    mPrinter.addHPosition(350);
                }
                gst_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", surchargeAmount), 10));
                method = "addText";
                mPrinter.addText(gst_value.toString());
                gst_value.delete(0,gst_value.length());
                method = "addFeedLine";
                mPrinter.addFeedLine(1);
                totalForCalculation = totalForCalculation.add(surchargeAmount);
                spc = "";
            }
            textData.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            textData.delete(0,textData.length());

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            total.append("TOTAL");
            method = "addText";
            mPrinter.addText(total.toString());
            total.delete(0,total.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }
            BigDecimal subtotal_after_discount = totalForCalculation.subtract(receipt.getDiscountAmount());
            total_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", Double.parseDouble(subtotal_after_discount.toString())), 10));
            method = "addText";
            mPrinter.addText(total_value.toString());
            total_value.delete(0,total_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            cash.append("CASH");
            method = "addText";
            mPrinter.addText(cash.toString());
            cash.delete(0,cash.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }
            cash_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", receipt.getPaidAmount()).toString().trim(), 10));
            method = "addText";
            mPrinter.addText(cash_value.toString());
            cash_value.delete(0,cash_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);

            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_LEFT);
            change.append("CHANGE");
            method = "addText";
            mPrinter.addText(change.toString());
            change.delete(0,change.length());
            method = "addHPosition";
            if(t88v) {
                mPrinter.addHPosition(380);
            }else{
                mPrinter.addHPosition(350);
            }

            String grandtotal = mTotalPayableTextView.getText().toString();
            String final_grandtotal = grandtotal.substring(1);
            BigDecimal grand_total = new BigDecimal(final_grandtotal);

            change_value.append(leftPaddingWithSpaces("$" + String.format("%.2f", receipt.getPaidAmount().subtract(grand_total)), 10));
            method = "addText";
            mPrinter.addText(change_value.toString());
            change_value.delete(0,change_value.length());
            method = "addFeedLine";
            mPrinter.addFeedLine(1);
            dot.append("------------------------------------------\n");
            method = "addText";
            mPrinter.addText(dot.toString());


            method = "addTextAlign";
            mPrinter.addTextAlign(Printer.ALIGN_CENTER);
            textData.append("THANK YOU!\n");
            method = "addText";
            mPrinter.addText(textData.toString());
            method = "addCut";
            mPrinter.addCut(Printer.CUT_FEED);

        } catch (Exception e) {
            ShowMsg.showException(e, method, mContext);
            return false;
        }
        return true;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2){
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
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

    static boolean isMark(char ch)
    {
        int type = Character.getType(ch);
        return type == Character.NON_SPACING_MARK ||
                type == Character.ENCLOSING_MARK ||
                type == Character.COMBINING_SPACING_MARK;
    }

}
