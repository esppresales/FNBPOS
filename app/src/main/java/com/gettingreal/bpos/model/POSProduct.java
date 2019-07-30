package com.gettingreal.bpos.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.gettingreal.bpos.AssetDatabaseOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by ivanfoong on 25/3/14.
 */

public class POSProduct {

    private static ArrayList<POSProduct> mProductCache = null;
    private String mUid, mName;
    private ArrayList<String> mDescriptions;
    private Float mPrice;
    private boolean mDisabled;
    private File mImageFile;
    private ArrayList<String> mCategoryUids;

    public POSProduct(final String aUid, final String aName, final ArrayList<String> aDescriptions, final Float aPrice, final boolean aDisabled, final File aImageFile, final ArrayList<String> aCategoryUids) {
        mUid = aUid;
        mName = aName;
        mDescriptions = aDescriptions;
        mPrice = aPrice;
        mDisabled = aDisabled;
        mImageFile = aImageFile;
        mCategoryUids = aCategoryUids;
    }

    public static void updateProductCategories(Context aContext, String aProductUid, ArrayList<String> aCategoryUids) {
        POSProduct.mProductCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        db.delete("category_products", "product_uid = ?", new String[]{aProductUid});

        for (String categoryUid : aCategoryUids) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("category_uid", categoryUid);
            contentValues.put("product_uid", aProductUid);
            contentValues.put("product_priority", 1);
            db.insert("category_products", null, contentValues);
        }

        db.close();
    }

    public static POSProduct getProduct(final Context aContext, final String aProductUid) {
        ArrayList<POSProduct> POSProducts = getAllProducts(aContext);
        for (POSProduct POSProduct : POSProducts) {
            if (POSProduct.getUid().contentEquals(aProductUid)) {
                return POSProduct;
            }
        }
        return null;
    }

    public static ArrayList<POSProduct> getAllProducts(final Context aContext) {
        if (mProductCache == null) {
            mProductCache = new ArrayList<POSProduct>();

            AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
            SQLiteDatabase db = dbHelper.openDatabase();
            String[] columns = new String[]{"uid", "name", "description", "price", "disabled"};
            int[] columnIndexes = new int[columns.length];

            Cursor c = db.query("products", columns, null, null, null, null, "uid");
            for (int i = 0; i < columns.length; i++) {
                columnIndexes[i] = c.getColumnIndex(columns[i]);
            }

            for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                String uid = c.getString(columnIndexes[0]), name = c.getString(columnIndexes[1]), description = c.getString(columnIndexes[2]);
                float price = c.getFloat(columnIndexes[3]);
                boolean disabled = c.getInt(columnIndexes[4]) == 1;

                ArrayList<String> descriptions = new ArrayList<String>();
                String[] descriptionLines = description.split("\\\\n|\n");
                for (int i = 0; i < descriptionLines.length; i++) {
                    descriptions.add(descriptionLines[i]);
                }

                File imageFile = null;
                try {
                    imageFile = getImageFileForProductUID(aContext, uid);
                } catch (Resources.NotFoundException e) {

                }

                ArrayList<String> categoryUids = getCategoryUidsForProductUid(aContext, uid);

                mProductCache.add(new POSProduct(uid, name, descriptions, price, disabled, imageFile, categoryUids));
            }

            db.close();
        }

        return mProductCache;
    }

    public static ArrayList<POSProduct> getAllEnabledProducts(final Context aContext) {
        if (mProductCache == null) {
            mProductCache = getAllProducts(aContext);
        }

        ArrayList<POSProduct> enabledProducts = new ArrayList<POSProduct>();
        for (POSProduct product : mProductCache) {
            if (!product.isDisabled()) {
                enabledProducts.add(product);
            }
        }

        return enabledProducts;
    }

    public static ArrayList<POSProduct> getAllProductsForCategoryUid(final Context aContext, final String aCategoryUid) {
        ArrayList<POSProduct> products = getAllProducts(aContext);
        ArrayList<POSProduct> productsOfCategory = new ArrayList<POSProduct>();


        for (POSProduct product : products) {
            if (product.getCategoryUids().contains(aCategoryUid)) {
                productsOfCategory.add(product);
            }
        }

        return productsOfCategory;
    }

    public static void clearAllProducts(final Context aContext) {
        POSProduct.mProductCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("products", null, null);
    }

    public static POSProduct createProduct(final Context aContext, final String aUid, final String aName, final ArrayList<String> aDescriptions, final Float aPrice, final boolean aDisabled, final Bitmap aBitmap, final ArrayList<String> aCategoryUids) {
        POSProduct.mProductCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        { // insert product
            ContentValues contentValues = new ContentValues();
            contentValues.put("uid", aUid);
            contentValues.put("name", aName);
            contentValues.put("description", TextUtils.join("\n", aDescriptions.toArray()));
            contentValues.put("price", aPrice);
            contentValues.put("disabled", aDisabled);
            db.insert("products", null, contentValues);
        }

        db.close();

        { // save image
            final String targetPath = aContext.getFilesDir().getAbsolutePath() + "/product_images/" + aUid + "/1";
            overwriteBitmapFile(aContext, aBitmap, targetPath);
        }

        // save product's category
        updateProductCategories(aContext, aUid, aCategoryUids);

        return getProduct(aContext, aUid);
    }

    private static ArrayList<String> getCategoryUidsForProductUid(final Context aContext, final String aProductUid) {
        ArrayList<String> categoryUids = new ArrayList<String>();

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        String[] columns = new String[]{"category_uid"};
        int[] columnIndexes = new int[columns.length];

        Cursor c = db.query("category_products", columns, "product_uid = ?", new String[]{aProductUid}, null, null, null);
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

    private static File getImageFileForProductUID(final Context aContext, final String uid) throws Resources.NotFoundException {
        File file = new File(aContext.getFilesDir().getAbsolutePath() + "/product_images/" + uid);
        File[] imageFiles = file.listFiles();

        if (imageFiles != null && imageFiles.length > 0) {
            return imageFiles[0];
        }
        throw new Resources.NotFoundException("Image not found " + "product_images/" + uid);
    }

    private static void overwriteBitmapFile(final Context aContext, Bitmap aBitmap, String targetPath) {
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "overwriteFile()");

            newFileName = targetPath;
            File newFile = new File(newFileName);
            newFile.getParentFile().mkdirs();

            out = new FileOutputStream(newFile);
            aBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);

            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e("tag", "Exception in overwriteFile() of " + newFileName);
            Log.e("tag", "Exception in overwriteFile() " + e.toString());
        }

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

    public ArrayList<String> getDescriptions() {
        return mDescriptions;
    }

    public void setDescriptions(final ArrayList<String> aDescriptions) {
        mDescriptions = aDescriptions;
    }

    public Float getPrice() {
        return mPrice;
    }

    public void setPrice(final Float aPrice) {
        mPrice = aPrice;
    }

    public boolean isDisabled() {
        return mDisabled;
    }

    public void setDisabled(final boolean aDisabled) {
        mDisabled = aDisabled;
    }

    public File getImageFile() {
        return mImageFile;
    }

    public void setImageFile(final File aImageFile) {
        mImageFile = aImageFile;
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
        POSProduct.mProductCache = null;

        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();

        // update product
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", mName);
            contentValues.put("description", TextUtils.join("\n", mDescriptions.toArray()));
            contentValues.put("price", getPrice());
            contentValues.put("disabled", mDisabled);
            db.update("products", contentValues, "uid = ?", new String[]{mUid});
        }

        db.close();

        // update product's category
        updateProductCategories(aContext, mUid, mCategoryUids);
    }

    public void replaceImage(final Context aContext, final Bitmap aBitmap) {
        final String targetPath = aContext.getFilesDir().getAbsolutePath() + "/product_images/" + mUid + "/1";
        overwriteBitmapFile(aContext, aBitmap, targetPath);
    }

    public void delete(final Context aContext) {
        AssetDatabaseOpenHelper dbHelper = new AssetDatabaseOpenHelper(aContext);
        SQLiteDatabase db = dbHelper.openDatabase();
        db.delete("products", "uid = ?", new String[]{mUid});
        db.close();

        POSProduct.mProductCache = null;
    }
}
