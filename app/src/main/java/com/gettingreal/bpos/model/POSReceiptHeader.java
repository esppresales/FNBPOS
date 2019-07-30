package com.gettingreal.bpos.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gettingreal.bpos.AssetDatabaseOpenHelper;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 20/6/14.
 */
public class POSReceiptHeader {
    public static ArrayList<POSReceiptHeader> mReceiptHeaderCache = null;
    private Long mId;
    private String mContent;
    private Integer mPriority;

    public POSReceiptHeader(final Long aId, final String aContent, final Integer aPriority) {
        mId = aId;
        mContent = aContent;
        mPriority = aPriority;
    }

    public Long getId() {
        return mId;
    }

    public void setId(final Long aId) {
        mId = aId;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(final String aContent) {
        mContent = aContent;
    }

    public Integer getPriority() {
        return mPriority;
    }

    public void setPriority(final Integer aPriority) {
        mPriority = aPriority;
    }

    public void save(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        // update receipt header
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put("content", mContent);
            db.update("receipt_headers", contentValues, "id = ?", new String[]{String.valueOf(mId)});
        }

        db.close();
    }

    public static ArrayList<POSReceiptHeader> getAllReceiptHeaders(final Context aContext) {
        if (mReceiptHeaderCache == null) {
            mReceiptHeaderCache = new ArrayList<POSReceiptHeader>();

            AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
            SQLiteDatabase db = dbHelper.openDatabase();
            String[] columns = new String[]{"id", "content", "priority"};
            int[] columnIndexes = new int[columns.length];

            Cursor c = db.query("receipt_headers", columns, null, null, null, null, "priority");
            for (int i = 0; i < columns.length; i++) {
                columnIndexes[i] = c.getColumnIndex(columns[i]);
            }

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                Long id = c.getLong(columnIndexes[0]);
                String content = c.getString(columnIndexes[1]);
                Integer priority = c.getInt(columnIndexes[2]);

                mReceiptHeaderCache.add(new POSReceiptHeader(id, content, priority));
            }

            db.close();
        }

        return mReceiptHeaderCache;
    }

    public static void clearAllReceiptHeaders(final Context aContext) {
        POSReceiptHeader.mReceiptHeaderCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("receipt_headers", null, null);
    }

    public static POSReceiptHeader getReceiptHeaderForPriority(final Context aContext, final Integer aPriority) {
        ArrayList<POSReceiptHeader> receiptHeaders = getAllReceiptHeaders(aContext);

        for (POSReceiptHeader receiptHeader : receiptHeaders) {
            if (receiptHeader.getPriority() == aPriority) {
                return receiptHeader;
            }
        }

        return null;
    }

    public static POSReceiptHeader createReceiptHeader(final Context aContext, final String aContent,final Integer aPriority) {
        POSReceiptHeader.mReceiptHeaderCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        { // insert product
            ContentValues contentValues = new ContentValues();
            contentValues.put("content", aContent);
            contentValues.put("priority", aPriority);
            db.insert("receipt_headers", null, contentValues);
        }

        db.close();

        return getReceiptHeaderForPriority(aContext, aPriority);
    }
}
