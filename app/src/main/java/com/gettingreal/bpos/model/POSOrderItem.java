package com.gettingreal.bpos.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import com.gettingreal.bpos.AssetDatabaseOpenHelper;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 7/3/14
 * Time: 6:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class POSOrderItem {
    private Long mOrderId;
    private String mProductUid;
    private int mQuantityOrdered, mQuantityServed;
    private String mRemark;

    public POSOrderItem(final Long aOrderId, final String aProductUid, final int aQuantityOrdered, final int aQuantityServed) {
        mOrderId = aOrderId;
        mProductUid = aProductUid;
        mQuantityOrdered = aQuantityOrdered;
        mQuantityServed = aQuantityServed;
    }

    public static ArrayList<POSOrderItem> getOrderItems(final Context aContext, final long aOrderId) {
        ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        String[] columns = new String[]{"order_id", "product_uid", "quantity_ordered", "quantity_served", "remark"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("order_items", columns, "order_id = ?", new String[]{String.valueOf(aOrderId)}, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long orderId = c.getLong(columnIndexes[0]);
            String productUid = c.getString(columnIndexes[1]), remark = c.getString(columnIndexes[4]);
            int quantityOrdered = c.getInt(columnIndexes[2]), quantityServed = c.getInt(columnIndexes[3]);

            POSProduct product = POSProduct.getProduct(aContext, productUid);
            POSOrderItem orderItem = new POSOrderItem(orderId, productUid, quantityOrdered, quantityServed);
            orderItem.setRemark(remark);
            orderItems.add(orderItem);
        }

        db.close();

        return orderItems;
    }

    public static POSOrderItem getOrderItemForProductUid(final Context aContext, final long aOrderId, final String aProductUid) {
        ArrayList<POSOrderItem> orderItems = getOrderItems(aContext, aOrderId);
        for (POSOrderItem orderItem : orderItems) {
            if (orderItem.getProductUid().contentEquals(aProductUid)) {
                return orderItem;
            }
        }
        return null;
    }

    public static void addPendingOrderItem(final Context aContext, final String aProductUid, final int aQuantity) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        // increment quantity if exist, create if not exist
        Cursor c = db.query("order_items", new String[]{"quantity_ordered"}, "order_id IS NULL AND product_uid = ?", new String[]{aProductUid}, null, null, null);
        c.moveToFirst();
        if (c.getCount() > 0) {
            int quantity = c.getInt(0);
            quantity += aQuantity;

            ContentValues contentValues = new ContentValues();
            contentValues.put("quantity_ordered", quantity);

            db.update("order_items", contentValues, "order_id IS NULL AND product_uid = ?", new String[]{aProductUid});
            notifyOrderChanged(aContext);
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put("product_uid", aProductUid);
            contentValues.put("quantity_ordered", aQuantity);

            db.insert("order_items", null, contentValues);
            notifyOrderChanged(aContext);
        }

        db.close();
    }

    public static void removePendingOrderItem(final Context aContext, final String aProductUid, final int aQuantity) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        // decrement quantity if exist and more than 0 remaining, delete if 0 quantity left
        Cursor c = db.query("order_items", new String[]{"quantity_ordered"}, "order_id IS NULL AND product_uid = ?", new String[]{aProductUid}, null, null, null);
        c.moveToFirst();
        if (c.getCount() > 0) {
            int quantity = c.getInt(0);
            if ((quantity - aQuantity) > 0) {
                quantity -= aQuantity;

                ContentValues contentValues = new ContentValues();
                contentValues.put("quantity_ordered", quantity);

                db.update("order_items", contentValues, "order_id IS NULL AND product_uid = ?", new String[]{aProductUid});
                notifyOrderChanged(aContext);
            } else {
                db.delete("order_items", "order_id IS NULL AND product_uid = ?", new String[]{aProductUid});
                notifyOrderChanged(aContext);
            }
        }

        db.close();
    }

    public static void deletePendingOrderItem(final Context aContext, final String aProductUid) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        db.delete("order_items", "order_id IS NULL AND product_uid = ?", new String[]{aProductUid});
        notifyOrderChanged(aContext);

        db.close();
    }

    public static ArrayList<POSOrderItem> getPendingOrderItems(final Context aContext) {
        ArrayList<POSOrderItem> orderItems = new ArrayList<POSOrderItem>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        String[] columns = new String[]{"product_uid", "quantity_ordered", "quantity_served", "remark"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("order_items", columns, "order_id IS NULL", null, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String productUid = c.getString(columnIndexes[0]), remark = c.getString(columnIndexes[3]);
            int quantityOrdered = c.getInt(columnIndexes[1]), quantityServed = c.getInt(columnIndexes[2]);
            POSOrderItem orderItem = new POSOrderItem(null, productUid, quantityOrdered, quantityServed);
            orderItem.setRemark(remark);
            orderItems.add(orderItem);
        }

        db.close();
        return orderItems;
    }

    public static POSOrderItem getPendingOrderItemForProductUid(final Context aContext, final String aProductUid) {
        ArrayList<POSOrderItem> orderItems = getPendingOrderItems(aContext);
        for (POSOrderItem orderItem : orderItems) {
            if (orderItem.getProductUid().contentEquals(aProductUid)) {
                return orderItem;
            }
        }
        return null;
    }

    public static void clearPendingOrderItems(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        db.delete("order_items", "order_id IS NULL", null);
        db.close();
        notifyOrderChanged(aContext);
    }

    public static void clearOrderItems(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        db.delete("order_items", null, null);
        db.close();
    }

    private static void notifyOrderChanged(final Context aContext) {
        Intent intent = new Intent("cart-updated");
        LocalBroadcastManager.getInstance(aContext).sendBroadcast(intent);
    }

    public Long getOrderId() {
        return mOrderId;
    }

    public void setOrderId(final Long aOrderId) {
        mOrderId = aOrderId;
    }

    public String getProductUid() {
        return mProductUid;
    }

    public void setProductUid(final String aProductUid) {
        mProductUid = aProductUid;
    }

    public int getQuantityOrdered() {
        return mQuantityOrdered;
    }

    public void setQuantityOrdered(final int aQuantityOrdered) {
        mQuantityOrdered = aQuantityOrdered;
    }

    public int getQuantityServed() {
        return mQuantityServed;
    }

    public void setQuantityServed(final int aQuantityServed) {
        mQuantityServed = aQuantityServed;
    }

    public String getRemark() {
        return mRemark;
    }

    public void setRemark(final String aRemark) {
        mRemark = aRemark;
    }

    public void save(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        // update product
        ContentValues contentValues = new ContentValues();
        contentValues.put("quantity_ordered", mQuantityOrdered);
        contentValues.put("quantity_served", mQuantityServed);
        contentValues.put("remark", mRemark);

        if (mOrderId != null) {
            //contentValues.put("order_id", mOrderId);
            db.update("order_items", contentValues, "order_id = " + String.valueOf(mOrderId) + " AND product_uid = ?", new String[]{mProductUid});
        } else {
            db.update("order_items", contentValues, "order_id IS NULL AND product_uid = ?", new String[]{mProductUid});
        }

        notifyOrderChanged(aContext);

        db.close();
    }
}
