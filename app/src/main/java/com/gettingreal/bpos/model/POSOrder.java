package com.gettingreal.bpos.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gettingreal.bpos.AssetDatabaseOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

/**
 * Created by ivanfoong on 28/3/14.
 */
public class POSOrder {
    private static final Hashtable<OrderingMode, String> ORDERING_MODE_VALUE_HASHTABLE = new Hashtable<OrderingMode, String>() {{
        put(OrderingMode.DINE_IN, "dine-in");
        put(OrderingMode.TAKE_AWAY, "take-away");
    }};
    private static final Hashtable<String, OrderingMode> ORDERING_MODE_KEY_HASHTABLE = new Hashtable<String, OrderingMode>() {{
        put("dine-in", OrderingMode.DINE_IN);
        put("take-away", OrderingMode.TAKE_AWAY);
    }};
    private Long mId, mReceiptId;
    private String mTableUid;
    private Date mOrderAt;
    private OrderingMode mOrderingMode;
    private ArrayList<POSOrderItem> mOrderItems;

    public POSOrder(final Long aId, final String aTableUid, final Long aReceiptId, final Date aOrderAt, final OrderingMode aOrderingMode, final ArrayList<POSOrderItem> aPOSOrderItems) {
        mId = aId;
        mTableUid = aTableUid;
        mReceiptId = aReceiptId;
        mOrderAt = aOrderAt;
        mOrderingMode = aOrderingMode;
        mOrderItems = aPOSOrderItems;
    }

    public static ArrayList<POSOrder> getAllOrders(final Context aContext) {
        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        String[] columns = new String[]{"id", "table_uid", "receipt_id", "order_at", "ordering_mode"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("orders", columns, null, null, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long id = c.getLong(columnIndexes[0]);
            String tableUid = c.getString(columnIndexes[1]);
            Long receiptId = c.getLong(columnIndexes[2]);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date orderAt = null;
            try {
                orderAt = sdf.parse(c.getString(columnIndexes[3]));
            } catch (ParseException e) {

            }

            String orderingModeString = c.getString(columnIndexes[4]);
            OrderingMode orderingMode = getOrderingMode(orderingModeString);

            ArrayList<POSOrderItem> orderItems = POSOrderItem.getOrderItems(aContext, id);

            orders.add(new POSOrder(id, tableUid, receiptId, orderAt, orderingMode, orderItems));
        }

        db.close();

        return orders;
    }

    public static ArrayList<POSOrder> getAllClosedOrders(final Context aContext) {
        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        String[] columns = new String[]{"id", "table_uid", "receipt_id", "order_at", "ordering_mode"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("orders", columns, "receipt_id NOT NULL", null, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long id = c.getLong(columnIndexes[0]);
            String tableUid = c.getString(columnIndexes[1]);
            Long receiptId = c.getLong(columnIndexes[2]);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date orderAt = null;
            try {
                orderAt = sdf.parse(c.getString(columnIndexes[3]));
            } catch (ParseException e) {

            }

            String orderingModeString = c.getString(columnIndexes[4]);
            OrderingMode orderingMode = getOrderingMode(orderingModeString);

            ArrayList<POSOrderItem> orderItems = POSOrderItem.getOrderItems(aContext, id);

            orders.add(new POSOrder(id, tableUid, receiptId, orderAt, orderingMode, orderItems));
        }

        db.close();

        return orders;
    }

    public static ArrayList<POSOrder> getAllOpenOrders(final Context aContext) {
        ArrayList<POSOrder> orders = new ArrayList<POSOrder>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        String[] columns = new String[]{"id", "table_uid", "receipt_id", "order_at", "ordering_mode"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("orders", columns, "receipt_id IS NULL", null, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            long id = c.getLong(columnIndexes[0]);
            String tableUid = c.getString(columnIndexes[1]);
            Long receiptId = c.getLong(columnIndexes[2]);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date orderAt = null;
            try {
                orderAt = sdf.parse(c.getString(columnIndexes[3]));
            } catch (ParseException e) {

            }

            String orderingModeString = c.getString(columnIndexes[4]);
            OrderingMode orderingMode = getOrderingMode(orderingModeString);

            ArrayList<POSOrderItem> orderItems = POSOrderItem.getOrderItems(aContext, id);

            orders.add(new POSOrder(id, tableUid, receiptId, orderAt, orderingMode, orderItems));
        }

        db.close();

        return orders;
    }

    public static ArrayList<POSOrder> getAllUndeliveredOrders(final Context aContext) {
        ArrayList<POSOrder> orders = getAllOpenOrders(aContext);
        ArrayList<POSOrder> undeliveredOrders = new ArrayList<POSOrder>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        for (POSOrder order : orders) { // check for any undelivered order items
            boolean isAllQuantityServed = true;
            ArrayList<POSOrderItem> orderItems = POSOrderItem.getOrderItems(aContext, order.getId());
            for (POSOrderItem orderItem : orderItems) {
                if (orderItem.getQuantityOrdered() != orderItem.getQuantityServed()) {
                    isAllQuantityServed = false;
                    break;
                }
            }

            if (!isAllQuantityServed) {
                undeliveredOrders.add(order);
            }
        }

        db.close();

        return undeliveredOrders;
    }

    public static ArrayList<POSOrder> getAllUnpaidOrders(final Context aContext) {
        ArrayList<POSOrder> orders = getAllOpenOrders(aContext);
        ArrayList<POSOrder> unpaidOrders = new ArrayList<POSOrder>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        for (POSOrder order : orders) { // check for all delivered order items
            boolean isAllQuantityServed = true;
            ArrayList<POSOrderItem> orderItems = POSOrderItem.getOrderItems(aContext, order.getId());
            for (POSOrderItem orderItem : orderItems) {
                if (orderItem.getQuantityOrdered() != orderItem.getQuantityServed()) {
                    isAllQuantityServed = false;
                    break;
                }
            }

            if (isAllQuantityServed) {
                unpaidOrders.add(order);
            }
        }

        db.close();

        return unpaidOrders;
    }

    public static POSOrder getOrder(final Context aContext, final long aOrderId) {
        for (POSOrder order : getAllOrders(aContext)) {
            if (order.getId() == aOrderId) {
                return order;
            }
        }
        return null;
    }


    public static void clearOrders(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        db.delete("orders", null, null);

        POSOrderItem.clearOrderItems(aContext);
    }

    public static POSOrder createOrder(final Context aContext, final Date aOrderAt, final String aTableUid, final OrderingMode aOrderingMode, final ArrayList<POSOrderItem> aPOSOrderItems) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        long orderId = -1;

        {
            ContentValues contentValues = new ContentValues();

            contentValues.put("order_at", sdf.format(aOrderAt));
            contentValues.put("table_uid", aTableUid);
            contentValues.put("ordering_mode", getOrderingModeValue(aOrderingMode));
            orderId = db.insert("orders", null, contentValues);
        }

        if (orderId != -1) {
            for (POSOrderItem orderItem : aPOSOrderItems) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("order_id", orderId);
                db.update("order_items", contentValues, "order_id IS NULL AND product_uid = ?", new String[]{orderItem.getProductUid()});
            }
        }

        db.close();

        return getOrder(aContext, orderId);
    }


    public static String getOrderingModeValue(OrderingMode aOrderingMode) {
        return ORDERING_MODE_VALUE_HASHTABLE.get(aOrderingMode);
    }

    public static OrderingMode getOrderingMode(String aOrderingMode) {
        return ORDERING_MODE_KEY_HASHTABLE.get(aOrderingMode);
    }

    public long getId() {
        return mId;
    }

    public String getTableUid() {
        return mTableUid;
    }

    public void setTableUid(final String aTableUid) {
        mTableUid = aTableUid;
    }

    public Long getReceiptId() {
        return mReceiptId;
    }

    public void setReceiptId(final Long aReceiptId) {
        mReceiptId = aReceiptId;
    }

    public Date getOrderAt() {
        return mOrderAt;
    }

    public void setOrderAt(final Date aOrderAt) {
        mOrderAt = aOrderAt;
    }

    public OrderingMode getOrderingMode() {
        return mOrderingMode;
    }

    public void setOrderingMode(final OrderingMode aOrderingMode) {
        mOrderingMode = aOrderingMode;
    }

    public ArrayList<POSOrderItem> getOrderItems() {
        return mOrderItems;
    }

    public void addOrderItem(final POSOrderItem aPOSOrderItem) {
        if (mOrderItems == null) {
            mOrderItems = new ArrayList<POSOrderItem>();
        }

        mOrderItems.add(aPOSOrderItem);
    }

    public void removeOrderItem(final POSOrderItem aPOSOrderItem) {
        if (mOrderItems == null) {
            mOrderItems = new ArrayList<POSOrderItem>();
        }

        if (mOrderItems.contains(aPOSOrderItem)) {
            mOrderItems.remove(aPOSOrderItem);
        }
    }

    public void save(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // update order
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put("receipt_id", mReceiptId);
            contentValues.put("table_uid", mTableUid);
            contentValues.put("order_at", sdf.format(mOrderAt));
            contentValues.put("ordering_mode", getOrderingModeValue(mOrderingMode));
            db.update("orders", contentValues, "id = ?", new String[]{String.valueOf(mId)});
        }

        db.close();
    }

    public enum OrderingMode {DINE_IN, TAKE_AWAY}
}
