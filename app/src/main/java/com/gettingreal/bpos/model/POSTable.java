package com.gettingreal.bpos.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gettingreal.bpos.AssetDatabaseOpenHelper;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 26/3/14.
 */
public class POSTable {
    private String mUid, mName, mStatus;
    private boolean mDisabled;

    public POSTable(final String aUid, final String aName, final String aStatus, final boolean aDisabled) {
        mUid = aUid;
        mName = aName;
        mStatus = aStatus;
        mDisabled = aDisabled;
    }

    public static ArrayList<POSTable> getAllTables(final Context aContext) {
        ArrayList<POSTable> POSTables = new ArrayList<POSTable>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        String[] columns = new String[]{"uid", "name", "status", "disabled"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("tables", columns, null, null, null, null, "uid");
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String uid = c.getString(columnIndexes[0]), name = c.getString(columnIndexes[1]), status = c.getString(columnIndexes[2]);
            boolean disabled = c.getInt(columnIndexes[3]) == 1;

            POSTables.add(new POSTable(uid, name, status, disabled));
        }

        db.close();

        return POSTables;
    }

    public static POSTable getTable(final Context aContext, final String aUid) {
        ArrayList<POSTable> POSTables = getAllTables(aContext);
        for (POSTable POSTable : POSTables) {
            if (POSTable.getUid() != null && POSTable.getUid().contentEquals(aUid)) {
                return POSTable;
            }
        }
        return null;
    }

    public static void clearAllTables(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("tables", null, null);
    }

    public static POSTable createTable(final Context aContext, final String aUid, final String aName, final String aStatus, final boolean aDisabled) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        { // insert product
            ContentValues contentValues = new ContentValues();
            contentValues.put("uid", aUid);
            contentValues.put("name", aName);
            contentValues.put("status", aStatus);
            contentValues.put("disabled", aDisabled);
            db.insert("tables", null, contentValues);
        }

        db.close();

        return getTable(aContext, aUid);
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(final String aUid) {
        mUid = aUid;
    }

    public String getName() {
        return mName;
    }

    public void setName(final String aName) {
        mName = aName;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(final String aStatus) {
        mStatus = aStatus;
    }

    public boolean isDisabled() {
        return mDisabled;
    }

    public void setDisabled(final boolean aDisabled) {
        mDisabled = aDisabled;
    }
}
