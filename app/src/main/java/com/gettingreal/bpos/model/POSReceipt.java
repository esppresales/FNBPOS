package com.gettingreal.bpos.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.gettingreal.bpos.AssetDatabaseOpenHelper;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by ivanfoong on 16/6/14.
 */
public class POSReceipt {
    private Long mId;
    private String mDiscountDescription;
    private BigDecimal mPaidAmount, mDiscountAmount, mFinalAmount;
    private Date mPaidAt;
    private ArrayList<POSOrder> mOrders;

    public POSReceipt(final Long aId, final String aDiscountDescription, final BigDecimal aPaidAmount, final BigDecimal aDiscountAmount, final BigDecimal aFinalAmount, final Date aPaidAt, final ArrayList<POSOrder> aOrders) {
        mId = aId;
        mDiscountDescription = aDiscountDescription;
        mPaidAmount = aPaidAmount;
        mDiscountAmount = aDiscountAmount;
        mFinalAmount = aFinalAmount;
        mPaidAt = aPaidAt;
        mOrders = aOrders;
    }

    public static POSReceipt getReceipt(final Context aContext, final Long aReceiptId) {
        ArrayList<POSReceipt> receipts = getAllReceipts(aContext);
        for (POSReceipt receipt : receipts) {
            if (receipt.getId() == aReceiptId) {
                return receipt;
            }
        }
        return null;
    }

    public static ArrayList<POSReceipt> getAllReceipts(final Context aContext) {
        ArrayList<POSReceipt> receipts = new ArrayList<POSReceipt>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        String[] columns = new String[]{"id", "paid_amount", "paid_at", "discount_amount", "final_amount", "discount_description"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("receipts", columns, null, null, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            Long id = c.getLong(columnIndexes[0]);
            BigDecimal paidAmount = BigDecimal.valueOf(c.getFloat(columnIndexes[1]));
            Date paidAt = null;
            try {
                paidAt = sdf.parse(c.getString(columnIndexes[2]));
            } catch (ParseException e) {

            }

            BigDecimal discountAmount = BigDecimal.valueOf(c.getDouble(columnIndexes[3]));
            BigDecimal finalAmount = BigDecimal.valueOf(c.getDouble(columnIndexes[4]));
            String discountDescription = c.getString(columnIndexes[5]);

            ArrayList<POSOrder> orders = new ArrayList<POSOrder>();

            receipts.add(new POSReceipt(id, discountDescription, paidAmount, discountAmount, finalAmount, paidAt, orders));
        }

        db.close();

        return receipts;
    }

    public Long getId() {
        return mId;
    }

    public void setId(final Long aId) {
        mId = aId;
    }

    public String getDiscountDescription() {
        return mDiscountDescription;
    }

    public void setDiscountDescription(final String aDiscountDescription) {
        mDiscountDescription = aDiscountDescription;
    }

    public BigDecimal getPaidAmount() {
        return mPaidAmount;
    }

    public void setPaidAmount(final BigDecimal aPaidAmount) {
        mPaidAmount = aPaidAmount;
    }

    public BigDecimal getDiscountAmount() {
        return mDiscountAmount;
    }

    public void setDiscountAmount(final BigDecimal aDiscountAmount) {
        mDiscountAmount = aDiscountAmount;
    }

    public BigDecimal getFinalAmount() {
        return mFinalAmount;
    }

    public void setFinalAmount(BigDecimal aFinalAmount) {
        mFinalAmount = aFinalAmount;
    }

    public Date getPaidAt() {
        return mPaidAt;
    }

    public void setPaidAt(final Date aPaidAt) {
        mPaidAt = aPaidAt;
    }

    public ArrayList<POSOrder> getOrders() {
        return mOrders;
    }

    public void addOrder(final POSOrder aOrder) {
        if (!mOrders.contains(aOrder)) {
            mOrders.add(aOrder);
        }
    }

    public void removeOrder(final POSOrder aOrder) {
        Log.e("come come", "remove order");
        if (mOrders.contains(aOrder)) {
            Log.e(" hey hey remove","");
            mOrders.remove(aOrder);
        }
    }

    public void OrderUpdate(Context context, POSOrder order){
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(context);
        SQLiteDatabase db = dbHelper.openDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("receipt_id", 0);

        db.update("orders", contentValues, "id=?" ,new String[]{Long.toString(order.getId())});
        Log.e("done update","");
    }

    public void clearOrders() {
        mOrders.clear();
    }
}
