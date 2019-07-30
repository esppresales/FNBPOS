package com.gettingreal.bpos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.epson.epos2.printer.Printer;
import com.gettingreal.bpos.helper.PrintHelper;
import com.gettingreal.bpos.model.POSCategory;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by ivanfoong on 3/4/14.
 */
public class PrintOrderTask extends AsyncTask<Void, Void, Void> {
    private Activity mActivity;
    private ProgressDialog mProgressDialog;
    private String mErrorMessage = null;
    private POSOrder mOrder;
    private OnPrintOrderTaskCompleted mOnPrintOrderTaskCompleted;
    private Printer  mPrinter = null;
    Context mContext;

    public PrintOrderTask(final Activity aActivity, final POSOrder aPOSOrder, final OnPrintOrderTaskCompleted aOnPrintOrderTaskCompleted) {
        mActivity = aActivity;
        mOrder = aPOSOrder;
        mOnPrintOrderTaskCompleted = aOnPrintOrderTaskCompleted;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog = ProgressDialog.show(mActivity, "Processing",
            "Please wait...", true);
    }

    @Override
    protected Void doInBackground(Void... urls) {
        try {
            SharedPreferences settings = mActivity.getSharedPreferences(mActivity.getPackageName(), 0);
            String address = settings.getString("address", "192.168.192.168");

            // print to master printer
          //  PrintHelper.getSharedInstance().printOrderItems(mActivity, address, 8009, "TCP:A4:EE:57:5C:6D:C2", mOrder, mOrder.getOrderItems());

            Hashtable<String, ArrayList<POSOrderItem>> orderItemsForPrinterUid = new Hashtable<String, ArrayList<POSOrderItem>>();
            for (POSOrderItem orderItem : mOrder.getOrderItems()) {
                POSProduct product = POSProduct.getProduct(mActivity, orderItem.getProductUid());
                for (String categoryUid : product.getCategoryUids()) {
                    POSCategory category = POSCategory.getCategory(mActivity, categoryUid);
                    for (String printerUid : category.getPrinterUids()) {
                        if (!orderItemsForPrinterUid.containsKey(printerUid)) {
                            orderItemsForPrinterUid.put(printerUid, new ArrayList<POSOrderItem>());
                        }

                        ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                        orderItems.add(orderItem);
                    }
                }
            }

            // print order items to their respective category printers
            for (String printerUid : orderItemsForPrinterUid.keySet()) {
                ArrayList<POSOrderItem> orderItems = orderItemsForPrinterUid.get(printerUid);
                Log.e("Print Order Item",printerUid);
               // PrintHelper.getSharedInstance().printOrderItems(mActivity, address, 8009, printerUid, mOrder, orderItems);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mErrorMessage = e.getLocalizedMessage();
        }

        return null;
    }

    protected void onPostExecute(Void result) {
        //showDialog("Downloaded " + result + " bytes");
        mProgressDialog.dismiss();

        mOnPrintOrderTaskCompleted.onPrintOrderTaskCompleted(mErrorMessage);
    }

    public interface OnPrintOrderTaskCompleted {
        public void onPrintOrderTaskCompleted(String errorMessage);
    }

}
