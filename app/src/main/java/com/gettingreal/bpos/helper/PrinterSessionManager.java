package com.gettingreal.bpos.helper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by  on 14/1/16.
 */
public class PrinterSessionManager {
    // LogCat tag
    private static String TAG = PrinterSessionManager.class.getSimpleName();

    // Shared Preferences
    public  static SharedPreferences pref;

    SharedPreferences.Editor editor;
    Context _context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "PrinterIPURL";
    private static final String KEY_URL_NAME="url";

    private static final String KEY_IS_URL="isURL_in";
    private static final String KEY_URL_ID="URL_id";

    private static final String KEY_SS = "e_pos";
    private static final String KEY_DISPLAY="display";
    private static final String KEY_DRAWER="drawer";
    private static final String KEY_CASHIRE_RECEIPT="cashiersize";
    private static final String KEY_ORDER_RECEIPT="ordersize";
    private static final String KEY_URL_NAME_KEY ="url_key";

    public PrinterSessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }


    public void setURL_Name(String url){
        editor.putString(KEY_URL_NAME, url);
        editor.commit();
    }
    public void setURL_NameKEY(String url){
        editor.putString(KEY_URL_NAME_KEY, url);
        editor.commit();
    }
    public void setDISPLAY(String display){
        editor.putString(KEY_DISPLAY, display);
        editor.commit();
    }

    public void setDRAWER(String drawer){
        editor.putString(KEY_DRAWER, drawer);
        editor.commit();
    }

    public void setCashierprintSize(String cashier){
        editor.putString(KEY_CASHIRE_RECEIPT,cashier);
        editor.commit();
    }

    public void setOrderPrintSize(String order){
        editor.putString(KEY_ORDER_RECEIPT,order);
        editor.commit();
    }

   public void set_SSURL(String surl){
        editor.putString(KEY_SS,surl);
        editor.commit();
    }

    public void setUrlin(boolean isurlin,String url_id){
        editor.putBoolean(KEY_IS_URL, isurlin);
        editor.putString(KEY_URL_ID, url_id);
        editor.commit();

    }

    public boolean isURLIn(){
        return pref.getBoolean(KEY_IS_URL,false);
    }

    public void getRemoveURL(){
        editor.remove(KEY_URL_NAME);
        editor.clear();
        editor.commit();
    }
    public static String getSSURL(){
        return pref.getString(KEY_SS,"");
    }

    public static String getPrintURL(){
        return pref.getString(KEY_URL_NAME,"");
    }
    public static String getDisplay(){
        return pref.getString(KEY_DISPLAY,"");
    }
    public static String getCashierDrawer(){
        return pref.getString(KEY_DRAWER,"");
    }

    public static String getCashierPrintSize(){
        return pref.getString(KEY_CASHIRE_RECEIPT,"");
    }

    public static String getOrderPrintSize(){
        return pref.getString(KEY_ORDER_RECEIPT,"");
    }
    public static String getPrintURL_KEY(){
        return pref.getString(KEY_URL_NAME_KEY,"");
    }
}
