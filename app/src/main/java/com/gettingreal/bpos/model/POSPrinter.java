package com.gettingreal.bpos.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gettingreal.bpos.AssetDatabaseOpenHelper;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 4/6/14.
 */
public class POSPrinter {
    private String mUid, mName;
    private int mPriority;
    private boolean mDisabled;
    private ArrayList<String> mCategoryUids;

    public POSPrinter(final String aUid, final String aName, final int aPriority, final boolean aDisabled, final ArrayList<String> aCategoryUids) {
        mUid = aUid;
        mName = aName;
        mPriority = aPriority;
        mDisabled = aDisabled;
        mCategoryUids = aCategoryUids;
    }

    public static void updatePrinterCategories(Context aContext, String aPrinterUid, ArrayList<String> aCategoryUids) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        db.delete("category_printers", "printer_uid = ?", new String[]{aPrinterUid});

        for (String categoryUid : aCategoryUids) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("category_uid", categoryUid);
            contentValues.put("printer_uid", aPrinterUid);
            db.insert("category_printers", null, contentValues);
        }

        db.close();
    }

    public static POSPrinter getPrinter(final Context aContext, final String aPrinterUid) {
        ArrayList<POSPrinter> printers = getAllPrinters(aContext);
        for (POSPrinter printer : printers) {
            if (printer.getUid().contentEquals(aPrinterUid)) {
                return printer;
            }
        }
        return null;
    }

    public static ArrayList<POSPrinter> getAllPrinters(final Context aContext) {
        ArrayList<POSPrinter> printers = new ArrayList<POSPrinter>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        String[] columns = new String[]{"uid", "name", "priority", "disabled"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("printers", columns, null, null, null, null, "uid");
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String uid = c.getString(columnIndexes[0]),
                name = c.getString(columnIndexes[1]);
            int priority = c.getInt(columnIndexes[2]);
            boolean disabled = c.getInt(columnIndexes[3]) == 1;

            ArrayList<String> categoryUids = getCategoryUidsForPrinterUid(aContext, uid);

            printers.add(new POSPrinter(uid, name, priority, disabled, categoryUids));
        }

        db.close();

        return printers;
    }

    public static void clearAllPrinters(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("printers", null, null);
    }

    public static void deletePrinter(final Context aContext, final String aUid) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("printers", "uid=?", new String[]{aUid});
    }

    public static POSPrinter createPrinter(final Context aContext, final String aUid, final String aName, final int aPriority, final boolean aDisabled, final ArrayList<String> aCategoryUids) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        { // insert product
            ContentValues contentValues = new ContentValues();
            contentValues.put("uid", aUid);
            contentValues.put("name", aName);
            contentValues.put("priority", aPriority);
            contentValues.put("disabled", aDisabled);
            db.insert("printers", null, contentValues);
        }

        db.close();

        // save printers's category
        updatePrinterCategories(aContext, aUid, aCategoryUids);

        return getPrinter(aContext, aUid);
    }

    private static ArrayList<String> getCategoryUidsForPrinterUid(final Context aContext, final String aPrinterUid) {
        ArrayList<String> categoryUids = new ArrayList<String>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        String[] columns = new String[]{"category_uid"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("category_printers", columns, "printer_uid = ?", new String[]{aPrinterUid}, null, null, null);
        for (int i = 0; i < columns.length; i++) {
            columnIndexes[i] = c.getColumnIndex(columns[i]);
        }

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            String categoryUid = c.getString(columnIndexes[0]);

            categoryUids.add(categoryUid);
        }

        db.close();

        return categoryUids;
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

    public ArrayList<String> getCategoryUids() {
        return mCategoryUids;
    }

    public void addCategoryUid(final String aCategoryUid) {
        if (!mCategoryUids.contains(aCategoryUid)) {
            mCategoryUids.add(aCategoryUid);
        }
    }

    public void removeCategoryUid(final String aCategoryUid) {
        if (mCategoryUids.contains(aCategoryUid)) {
            mCategoryUids.remove(aCategoryUid);
        }
    }

    public void clearCategoryUids() {
        mCategoryUids.clear();
    }

    public void save(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        // update product
        {
            /*
            mUid = aUid;
            mName = aName;
            mAddress = aAddress;
            mPort = aPort;
            mPriority = aPriority;
            mDisabled = aDisabled;
             */
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", getName());
            contentValues.put("priority", getPriority());
            db.update("printers", contentValues, "uid = ?", new String[]{getUid()});
        }

        db.close();

        // update product's category
        updatePrinterCategories(aContext, mUid, mCategoryUids);
    }
}
