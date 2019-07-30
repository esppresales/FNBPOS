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
public class POSSurcharge {
    private static ArrayList<POSSurcharge> mSurchargeCache = null;
    private Long mId;
    private String mName;
    private Double mPercentage;
    private Integer mPriority;

    public POSSurcharge(final Long aId, final String aName, final Double aPercentage, final Integer aPriority) {
        mId = aId;
        mName = aName;
        mPercentage = aPercentage;
        mPriority = aPriority;
    }

    public Long getId() {
        return mId;
    }

    public void setId(final Long aId) {
        mId = aId;
    }

    public String getName() {
        return mName;
    }

    public void setName(final String aName) {
        mName = aName;
    }

    public Double getPercentage() {
        return mPercentage;
    }

    public void setPercentage(final Double aPercentage) {
        mPercentage = aPercentage;
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
            contentValues.put("name", mName);
            contentValues.put("percentage", mPercentage);
            db.update("surcharges", contentValues, "id = ?", new String[]{String.valueOf(mId)});
        }

        db.close();
    }

    public static ArrayList<POSSurcharge> getAllSurcharges(final Context aContext) {
        if (mSurchargeCache == null) {
            mSurchargeCache = new ArrayList<POSSurcharge>();

            AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
            SQLiteDatabase db = dbHelper.openDatabase();
            String[] columns = new String[]{"id", "name", "percentage", "priority"};
            int[] columnIndexes = new int[columns.length];

            Cursor c = db.query("surcharges", columns, null, null, null, null, "priority");
            for (int i = 0; i < columns.length; i++) {
                columnIndexes[i] = c.getColumnIndex(columns[i]);
            }

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                Long id = c.getLong(columnIndexes[0]);
                String name = c.getString(columnIndexes[1]);
                Double percentage = c.getDouble(columnIndexes[2]);
                Integer priority = c.getInt(columnIndexes[3]);

                mSurchargeCache.add(new POSSurcharge(id, name, percentage, priority));
            }

            db.close();
        }

        return mSurchargeCache;
    }

    public static void clearAllSurcharges(final Context aContext) {
        POSSurcharge.mSurchargeCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("surcharges", null, null);
    }

    public static POSSurcharge getSurchargeForPriority(final Context aContext, final Integer aPriority) {
        ArrayList<POSSurcharge> surcharges = getAllSurcharges(aContext);

        for (POSSurcharge surcharge : surcharges) {
            if (surcharge.getPriority() == aPriority) {
                return surcharge;
            }
        }

        return null;
    }

    public static POSSurcharge createSurcharge(final Context aContext, final String aName, final Double aPercentage, final Integer aPriority) {
        POSSurcharge.mSurchargeCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        { // insert product
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", aName);
            contentValues.put("percentage", aPercentage);
            contentValues.put("priority", aPriority);
            db.insert("surcharges", null, contentValues);
        }

        db.close();

        return getSurchargeForPriority(aContext, aPriority);
    }
}
