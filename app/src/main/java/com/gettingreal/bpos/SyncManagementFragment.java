package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerCategory;
import com.gettingreal.bpos.api.ServerPrinter;
import com.gettingreal.bpos.api.ServerProduct;
import com.gettingreal.bpos.api.ServerReceiptHeader;
import com.gettingreal.bpos.api.ServerStatus;
import com.gettingreal.bpos.api.ServerSurcharge;
import com.gettingreal.bpos.api.ServerTable;
import com.gettingreal.bpos.model.POSCategory;
import com.gettingreal.bpos.model.POSPrinter;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSReceiptHeader;
import com.gettingreal.bpos.model.POSSurcharge;
import com.gettingreal.bpos.model.POSTable;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

/**
 * Created by ivanfoong on 26/5/14.
 */
public class SyncManagementFragment extends Fragment {
    EditText mServerAddressEditText;
    Button mRestoreButton, mSyncButton;
    ImageView mOnlineImageView;
    Picasso mPicasso;

    private boolean mIsServerOnline = false;
    private BroadcastReceiver mServerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received server-status broadcast");
            mIsServerOnline = intent.getBooleanExtra("online", false);

            int imageResourceId = R.drawable.ic_offline;

            if (mIsServerOnline) {
                imageResourceId = R.drawable.ic_online;
            }

            mOnlineImageView.setImageResource(imageResourceId);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sync_management, container, false);

        mServerAddressEditText = (EditText) view.findViewById(R.id.edit_text_server_address);

        mRestoreButton = (Button) view.findViewById(R.id.button_restore);
        mRestoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                restoreFromServer(aView.getContext());
            }
        });
        mRestoreButton.setEnabled(true);

        mSyncButton = (Button) view.findViewById(R.id.button_sync);
        mSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                syncToServer(aView.getContext());
            }
        });
        mSyncButton.setEnabled(true);

        mOnlineImageView = (ImageView)view.findViewById(R.id.img_online);

        mPicasso = ((MyApplication)getActivity().getApplication()).getPicasso();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");
        //String port = settings.getString("port", "80");
        mServerAddressEditText.setText(address);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));
    }

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("address", mServerAddressEditText.getText().toString());
        editor.commit();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mServerStatusBroadcastReceiver);
    }

    private void restoreFromServer(final Context aContext) {
      /*  if (mIsServerOnline) {*/
            SharedPreferences settings = aContext.getSharedPreferences(aContext.getPackageName(), 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("address", mServerAddressEditText.getText().toString());
            editor.commit();

            String serverUrl = "http://" + mServerAddressEditText.getText().toString() + "/";
         //   String serverUrl = "http://192.168.1.158/bpos_server/";
            mRestoreButton.setEnabled(false);
            new RestoreDataTask(aContext).execute(serverUrl);
        /*}
        else {
            new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Restore not available")
                    .setMessage("Service is not online")
                    .setNegativeButton("Dismiss", null)
                    .show();
        }*/
    }

    private void syncToServer(final Context aContext) {
        if (mIsServerOnline) {
            SharedPreferences settings = aContext.getSharedPreferences(aContext.getPackageName(), 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("address", mServerAddressEditText.getText().toString());
            editor.commit();

            String serverUrl = "http://" + mServerAddressEditText.getText().toString() + "/";

            mSyncButton.setEnabled(false);
            new UploadDataTask(aContext).execute(serverUrl);
        }
        else {
            new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Sync not available")
                    .setMessage("Service is not online")
                    .setNegativeButton("Dismiss", null)
                    .show();
        }
    }

    public Bitmap getBitmapFromURL(String src) {
        try {
            return mPicasso.load(src).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private class RestoreDataTask extends AsyncTask<String, Void, Boolean> {

        private Context mContext;
        private ProgressDialog mProgressDialog;
        private String mServerAddress;

        private RestoreDataTask(final Context aContext) {
            mContext = aContext;
            mProgressDialog = new ProgressDialog(mContext);
        }

        private boolean restoreProducts(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            Collection<ServerProduct> serverProducts = null;

            try {
                serverProducts = aServerAPIClient.getProducts();

                POSProduct.clearAllProducts(mContext);
                for (ServerProduct product : serverProducts) {
                    ArrayList<String> descriptions = new ArrayList(Arrays.asList(product.descriptions));
                    ArrayList<String> categoryUids = new ArrayList(Arrays.asList(product.category_uids));

                    Bitmap bitmap = null;
                    if (product.images != null && product.images.length > 0) {
                        String bitmapUrl = aServerAPIClient.getServerUrl().substring(0, aServerAPIClient.getServerUrl().length() - 1) + product.images[0];
                        bitmap = getBitmapFromURL(bitmapUrl);
                    }

                    POSProduct.createProduct(mContext, product.uid, product.name, descriptions, product.price, product.disabled == 1, bitmap, categoryUids);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            return serverProducts != null && serverProducts.size() > 0;
        }

        private boolean restoreCategories(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            Collection<ServerCategory> serverCategories = null;
            try {
                serverCategories = aServerAPIClient.getCategories();

                POSCategory.clearAllCategories(mContext);
                for (ServerCategory category : serverCategories) {
                    POSCategory.createCategory(mContext, category.uid, category.name, category.priority, category.disabled == 1);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            return serverCategories != null && serverCategories.size() > 0;
        }

        private boolean restorePrinters(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            Collection<ServerPrinter> serverPrinters = null;
            try {
                serverPrinters = aServerAPIClient.getPrinters();

                POSPrinter.clearAllPrinters(mContext);
                for (ServerPrinter printer : serverPrinters) {
                    ArrayList<String> categoryUids = new ArrayList<String>();
                    if (printer.category_uids != null) {
                        categoryUids = new ArrayList(Arrays.asList(printer.category_uids));
                    }

                    POSPrinter.createPrinter(mContext, printer.uid, printer.name, printer.priority, printer.disabled == 1, categoryUids);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            return serverPrinters != null && serverPrinters.size() > 0;
        }

        private boolean restoreTables(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            Collection<ServerTable> serverTables = null;
            try {
                serverTables = aServerAPIClient.getTables();

                POSTable.clearAllTables(mContext);
                for (ServerTable table : serverTables) {
                    POSTable.createTable(mContext, table.uid, table.name, table.status, table.disabled == 1);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            return serverTables != null && serverTables.size() > 0;
        }

        private boolean restoreReceiptHeaders(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            Collection<ServerReceiptHeader> serverReceiptHeaders = null;
            try {
                serverReceiptHeaders = aServerAPIClient.getReceiptHeaders();

                POSReceiptHeader.clearAllReceiptHeaders(mContext);
                for (ServerReceiptHeader receiptHeader : serverReceiptHeaders) {
                    POSReceiptHeader.createReceiptHeader(mContext, receiptHeader.content, receiptHeader.priority);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            return serverReceiptHeaders != null && serverReceiptHeaders.size() > 0;
        }

        private boolean restoreSurcharges(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            Collection<ServerSurcharge> serverSurcharges = null;
            try {
                serverSurcharges = aServerAPIClient.getSurcharges();

                POSSurcharge.clearAllSurcharges(mContext);
                for (ServerSurcharge serverSurcharge : serverSurcharges) {
                    POSSurcharge.createSurcharge(mContext, serverSurcharge.name, serverSurcharge.percentage, serverSurcharge.priority);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }

            return serverSurcharges != null && serverSurcharges.size() > 0;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.setMessage("Restoring...");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(final String... aStrings) {
            if (aStrings.length > 0) {
                mServerAddress = aStrings[0];
                ServerAPIClient serverAPIClient = new ServerAPIClient(mServerAddress);
                boolean tableRestoreResult = false;
                boolean categoryRestoreResult = false;
                boolean printerRestoreResult = false;
                boolean productRestoreResult = false;
                boolean receiptHeaderRestoreResult = false;
                boolean surchargeRestoreResult = false;
                try {
                    tableRestoreResult = restoreTables(serverAPIClient);

                    categoryRestoreResult = restoreCategories(serverAPIClient);

                    printerRestoreResult = restorePrinters(serverAPIClient);

                    productRestoreResult = restoreProducts(serverAPIClient);

                    receiptHeaderRestoreResult = restoreReceiptHeaders(serverAPIClient);

                    surchargeRestoreResult = restoreSurcharges(serverAPIClient);
                } catch (RetrofitError e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    return tableRestoreResult && categoryRestoreResult && printerRestoreResult && productRestoreResult && receiptHeaderRestoreResult && surchargeRestoreResult;
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            // TODO present status of syncing
            mProgressDialog.dismiss();
            if (result) {
                Toast.makeText(mContext, "Data restore successful", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "Problem restoring data from server, " + mServerAddress, Toast.LENGTH_LONG).show();
            }

            mSyncButton.setEnabled(true);
        }
    }

    private class UploadDataTask extends AsyncTask<String, Void, Boolean> {

        private Context mContext;
        private ProgressDialog mProgressDialog;
        private String mServerAddress;

        private UploadDataTask(final Context aContext) {
            mContext = aContext;
            mProgressDialog = new ProgressDialog(mContext);
        }

        private boolean uploadProducts(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            final ConditionVariable conditionVariable = new ConditionVariable(false);
            aServerAPIClient.deleteProducts(new Callback<String>() {
                @Override
                public void success(final String s, final Response aResponse) {
                    conditionVariable.open();
                }

                @Override
                public void failure(final RetrofitError aRetrofitError) {
                    conditionVariable.open();
                }
            });

            if (conditionVariable.block(5 * 1000)) {
                ArrayList<POSProduct> products = POSProduct.getAllProducts(mContext);
                for (POSProduct product : products) {
                    ServerProduct serverProduct = new ServerProduct();
                    serverProduct.uid = product.getUid();
                    serverProduct.name = product.getName();
                    serverProduct.descriptions = product.getDescriptions().toArray(new String[]{});
                    serverProduct.price = product.getPrice();
                    serverProduct.disabled = product.isDisabled() ? 1 : 0;
                    //serverProduct.images;
                    //serverProduct.category_uids;

                    aServerAPIClient.createProduct(serverProduct, new TypedFile("application/octet-stream", product.getImageFile()), new Callback<String>() {
                        @Override
                        public void success(final String s, final Response aResponse) {
                        }

                        @Override
                        public void failure(final RetrofitError aRetrofitError) {
                        }
                    });
                }

                return true;
            }
            return false;
        }

        private boolean uploadCategories(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            final ConditionVariable conditionVariable = new ConditionVariable(false);
            aServerAPIClient.deleteCategories(new Callback<String>() {
                @Override
                public void success(final String s, final Response aResponse) {
                    conditionVariable.open();
                }

                @Override
                public void failure(final RetrofitError aRetrofitError) {
                    conditionVariable.open();
                }
            });

            if (conditionVariable.block(5 * 1000)) {
                ArrayList<POSCategory> categories = POSCategory.getAllCategories(mContext);
                for (POSCategory category : categories) {
                    ServerCategory serverCategory = new ServerCategory();
                    serverCategory.uid = category.getUid();
                    serverCategory.name = category.getName();
                    serverCategory.priority = category.getPriority();
                    serverCategory.disabled = category.isDisabled() ? 1 : 0;

                    aServerAPIClient.createCategory(serverCategory, new Callback<String>() {
                        @Override
                        public void success(final String s, final Response aResponse) {

                        }

                        @Override
                        public void failure(final RetrofitError aRetrofitError) {

                        }
                    });
                }

                return true;
            }
            return false;
        }

        private boolean uploadPrinters(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            final ConditionVariable conditionVariable = new ConditionVariable(false);
            aServerAPIClient.deletePrinters(new Callback<String>() {
                @Override
                public void success(final String s, final Response aResponse) {
                    conditionVariable.open();
                }

                @Override
                public void failure(final RetrofitError aRetrofitError) {
                    conditionVariable.open();
                }
            });

            if (conditionVariable.block(5 * 1000)) {
                ArrayList<POSPrinter> printers = POSPrinter.getAllPrinters(mContext);
                for (POSPrinter printer : printers) {
                    ServerPrinter serverPrinter = new ServerPrinter();
                    serverPrinter.uid = printer.getUid();
                    serverPrinter.name = printer.getName();
                    serverPrinter.priority = printer.getPriority();
                    serverPrinter.disabled = printer.isDisabled() ? 1 : 0;
                    //serverPrinter.category_uids

                    aServerAPIClient.createPrinter(serverPrinter, new Callback<String>() {
                        @Override
                        public void success(final String s, final Response aResponse) {

                        }

                        @Override
                        public void failure(final RetrofitError aRetrofitError) {

                        }
                    });
                }

                return true;
            }
            return false;
        }

        private boolean uploadTables(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            final ConditionVariable conditionVariable = new ConditionVariable(false);
            aServerAPIClient.deleteTables(new Callback<String>() {
                @Override
                public void success(final String s, final Response aResponse) {
                    conditionVariable.open();
                }

                @Override
                public void failure(final RetrofitError aRetrofitError) {
                    conditionVariable.open();
                }
            });

            if (conditionVariable.block(5 * 1000)) {
                ArrayList<POSTable> tables = POSTable.getAllTables(mContext);
                for (POSTable table : tables) {
                    ServerTable serverTable = new ServerTable();
                    serverTable.uid = table.getUid();
                    serverTable.name = table.getName();
                    serverTable.status = table.getStatus();
                    serverTable.disabled = table.isDisabled() ? 1 : 0;
                    //serverPrinter.category_uids

                    aServerAPIClient.createTable(serverTable, new Callback<String>() {
                        @Override
                        public void success(final String s, final Response aResponse) {

                        }

                        @Override
                        public void failure(final RetrofitError aRetrofitError) {

                        }
                    });
                }

                return true;
            }
            return false;
        }

        private boolean uploadReceiptHeaders(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            final ConditionVariable conditionVariable = new ConditionVariable(false);
            aServerAPIClient.deleteReceiptHeaders(new Callback<String>() {
                @Override
                public void success(final String s, final Response aResponse) {
                    conditionVariable.open();
                }

                @Override
                public void failure(final RetrofitError aRetrofitError) {
                    conditionVariable.open();
                }
            });

            if (conditionVariable.block(5 * 1000)) {
                ArrayList<POSReceiptHeader> receiptHeaders = POSReceiptHeader.getAllReceiptHeaders(mContext);
                for (POSReceiptHeader receiptHeader : receiptHeaders) {
                    ServerReceiptHeader serverReceiptHeader = new ServerReceiptHeader();
                    serverReceiptHeader.id = receiptHeader.getId();
                    serverReceiptHeader.content = receiptHeader.getContent();
                    serverReceiptHeader.priority = receiptHeader.getPriority();

                    aServerAPIClient.createReceiptHeader(serverReceiptHeader, new Callback<Collection<ServerReceiptHeader>>() {
                        @Override
                        public void success(final Collection<ServerReceiptHeader> aServerReceiptHeaders, final Response aResponse) {

                        }

                        @Override
                        public void failure(final RetrofitError aRetrofitError) {

                        }
                    });
                }

                return true;
            }
            return false;
        }

        private boolean uploadSurcharges(ServerAPIClient aServerAPIClient) throws RetrofitError, Exception {
            final ConditionVariable conditionVariable = new ConditionVariable(false);
            aServerAPIClient.deleteSurcharges(new Callback<String>() {
                @Override
                public void success(final String s, final Response aResponse) {
                    conditionVariable.open();
                }

                @Override
                public void failure(final RetrofitError aRetrofitError) {
                    conditionVariable.open();
                }
            });

            if (conditionVariable.block(5 * 1000)) {
                ArrayList<POSSurcharge> surcharges = POSSurcharge.getAllSurcharges(mContext);
                for (POSSurcharge surcharge : surcharges) {
                    ServerSurcharge serverSurcharge = new ServerSurcharge();
                    serverSurcharge.id = surcharge.getId();
                    serverSurcharge.name = surcharge.getName();
                    serverSurcharge.percentage = surcharge.getPercentage();
                    serverSurcharge.priority = surcharge.getPriority();

                    aServerAPIClient.createSurcharge(serverSurcharge, new Callback<Collection<ServerSurcharge>>() {
                        @Override
                        public void success(final Collection<ServerSurcharge> aServerSurcharges, final Response aResponse) {

                        }

                        @Override
                        public void failure(final RetrofitError aRetrofitError) {

                        }
                    });
                }

                return true;
            }
            return false;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.setMessage("Uploading...");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(final String... aStrings) {
            if (aStrings.length > 0) {
                mServerAddress = aStrings[0];
                ServerAPIClient serverAPIClient = new ServerAPIClient(mServerAddress);
                boolean tableUploadResult = false;
                boolean categoryUploadResult = false;
                boolean printerUploadResult = false;
                boolean productUploadResult = false;
                boolean receiptHeaderUploadResult = false;
                boolean surchargeUploadResult = false;
                try {
                    tableUploadResult = uploadTables(serverAPIClient);

                    categoryUploadResult = uploadCategories(serverAPIClient);

                    printerUploadResult = uploadPrinters(serverAPIClient);

                    productUploadResult = uploadProducts(serverAPIClient);

                    receiptHeaderUploadResult = uploadReceiptHeaders(serverAPIClient);

                    surchargeUploadResult = uploadSurcharges(serverAPIClient);
                } catch (RetrofitError e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    return tableUploadResult && categoryUploadResult && printerUploadResult && productUploadResult && receiptHeaderUploadResult && surchargeUploadResult;
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            // TODO present status of syncing
            mProgressDialog.dismiss();
            if (result) {
                Toast.makeText(mContext, "Data upload successful", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, "Problem uploading data from server, " + mServerAddress, Toast.LENGTH_LONG).show();
            }

            mSyncButton.setEnabled(true);
        }
    }
}
