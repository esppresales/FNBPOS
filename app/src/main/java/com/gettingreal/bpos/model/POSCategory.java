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
public class POSCategory {
    private String mUid, mName;
    private int mPriority;
    private boolean mDisabled;
    private ArrayList<String> mPrinterUids;

    public POSCategory(final String aUid, final String aName, final int aPriority, final boolean aDisabled, final ArrayList<String> aPrinterUids) {
        mUid = aUid;
        mName = aName;
        mPriority = aPriority;
        mDisabled = aDisabled;
        mPrinterUids = aPrinterUids;
    }

    public static POSCategory getCategory(final Context aContext, final String aCategoryUid) {
        ArrayList<POSCategory> categories = getAllCategories(aContext);
        for (POSCategory category : categories) {
            if (category.getUid().contentEquals(aCategoryUid)) {
                return category;
            }
        }
        return null;
    }

    public static ArrayList<POSCategory> getAllCategories(final Context aContext) {
        ArrayList<POSCategory> categories = new ArrayList<POSCategory>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        String[] columns = new String[]{"uid", "name", "priority", "disabled"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("categories", columns, null, null, null, null, "uid");
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String uid = c.getString(columnIndexes[0]), name = c.getString(columnIndexes[1]);
            int priority = c.getInt(columnIndexes[2]);
            boolean disabled = c.getInt(columnIndexes[3]) == 1;

            ArrayList<String> printerUids = getPrinterUidsForCategoryUid(aContext, uid);

            categories.add(new POSCategory(uid, name, priority, disabled, printerUids));
        }

        db.close();

        return categories;
    }

    public static ArrayList<POSCategory> getAllEnabledCategories(final Context aContext) {
        ArrayList<POSCategory> categories = new ArrayList<POSCategory>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        String[] columns = new String[]{"uid", "name", "priority", "disabled"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("categories", columns, "disabled = ?", new String[] {"0"}, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String uid = c.getString(columnIndexes[0]), name = c.getString(columnIndexes[1]);
            int priority = c.getInt(columnIndexes[2]);
            boolean disabled = c.getInt(columnIndexes[3]) == 1;
            ArrayList<String> printerUids = getPrinterUidsForCategoryUid(aContext, uid);

            categories.add(new POSCategory(uid, name, priority, disabled, printerUids));
        }

        db.close();

        return categories;
    }

    public static void clearAllCategories(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("categories", null, null);
    }

    public static POSCategory createCategory(final Context aContext, final String aUid, final String aName, final int aPriority, final boolean aDisabled) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        { // insert product
            ContentValues contentValues = new ContentValues();
            contentValues.put("uid", aUid);
            contentValues.put("name", aName);
            contentValues.put("priority", aPriority);
            contentValues.put("disabled", aDisabled);
            db.insert("categories", null, contentValues);
        }

        db.close();

        return getCategory(aContext, aUid);
    }

    private static ArrayList<String> getPrinterUidsForCategoryUid(final Context aContext, final String aCategoryUid) {
        ArrayList<String> printerUids = new ArrayList<String>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        String[] columns = new String[]{"printer_uid"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("category_printers", columns, "category_uid = ?", new String[]{aCategoryUid}, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String printerUid = c.getString(columnIndexes[0]);

            printerUids.add(printerUid);
        }

        db.close();

        return printerUids;
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

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(final int aPriority) {
        mPriority = aPriority;
    }

    public boolean isDisabled() {
        return mDisabled;
    }

    public void setDisabled(final boolean aDisabled) {
        mDisabled = aDisabled;
    }

    public ArrayList<String> getPrinterUids() {
        return mPrinterUids;
    }

    public void addPrinterUid(final String aPrinterUid) {
        if (!mPrinterUids.contains(aPrinterUid)) {
            mPrinterUids.add(aPrinterUid);
        }
    }

    public void removePrinterUid(final String aPrinterUid) {
        if (mPrinterUids.contains(aPrinterUid)) {
            mPrinterUids.remove(aPrinterUid);
        }
    }

    public void clearPrinterUids() {
        mPrinterUids.clear();
    }

    public void save(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        // update category
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", mName);
            contentValues.put("priority", 100);
            contentValues.put("disabled", mDisabled);
            db.update("categories", contentValues, "uid = ?", new String[]{mUid});
        }

        // update category's printer
        {
            db.delete("category_printers", "category_uid = ?", new String[]{mUid});

            for (String printerUid : mPrinterUids) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("category_uid", mUid);
                contentValues.put("printer_uid", printerUid);
                db.insert("category_printers", null, contentValues);
            }
        }

        db.close();
    }

    public void delete(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("categories", "uid = ?", new String[]{mUid});
        db.close();
    }
}
