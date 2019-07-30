package com.gettingreal.bpos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.commbox.CommBox;
import com.epson.epos2.commbox.ReceiveListener;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.gettingreal.bpos.helper.PaymentHelper;
import com.gettingreal.bpos.helper.PrintHelper;
import com.gettingreal.bpos.helper.PrintPOS2Help;
import com.gettingreal.bpos.helper.PrinterSessionManager;
import com.gettingreal.bpos.helper.ShowMsg;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSReceipt;
import com.gettingreal.bpos.model.POSReceiptHeader;
import com.gettingreal.bpos.model.POSSurcharge;
import com.gettingreal.bpos.model.POSTable;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by ivanfoong on 3/4/14.
 */
public class PrintReceiptTask extends AsyncTask<Void, Void, Void>{

    private Activity mActivity;
    private ProgressDialog mProgressDialog;
    private String mErrorMessage = null;
    private POSReceipt mReceipt;
    private OnPrintReceiptTaskCompleted mOnPrintReceiptTaskCompleted;
    PrinterSessionManager printerSessionManager;
    public Printer  mPrinter = null;
    private Context mContext;
    private PrintPOS2Help printPOS2Help;
    public PrintReceiptTask(final Activity aActivity, final POSReceipt aReceipt, final OnPrintReceiptTaskCompleted aOnPrintReceiptTaskCompleted) {
        mActivity = aActivity;
        mReceipt = aReceipt;
        mOnPrintReceiptTaskCompleted = aOnPrintReceiptTaskCompleted;
        printPOS2Help=new PrintPOS2Help();
    }



    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = ProgressDialog.show(mActivity, "Processing",
            "Please wait...", true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(final DialogInterface aDialogInterface) {
                PrintReceiptTask.this.cancel(true);
            }
        });
    }

    @Override
    protected Void doInBackground(Void... urls) {
        try {
            SharedPreferences settings = mActivity.getSharedPreferences(mActivity.getPackageName(), 0);
            String address = settings.getString("address", "192.168.192.168");

           // PrintHelper.getSharedInstance().printReceipt(mActivity, address, 8009, "local_printIP", mReceipt);
        } catch (Exception e) {
            e.printStackTrace();
            mErrorMessage = e.getLocalizedMessage();
        }
        return null;
    }

    protected void onPostExecute(Void result) {
        //showDialog("Downloaded " + result + " bytes");
        mProgressDialog.dismiss();

        mOnPrintReceiptTaskCompleted.onPrintReceiptTaskCompleted(mErrorMessage);
    }

    public interface OnPrintReceiptTaskCompleted {
        public void onPrintReceiptTaskCompleted(String errorMessage);
    }



}
