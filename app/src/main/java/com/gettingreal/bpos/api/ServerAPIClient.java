package com.gettingreal.bpos.api;

import android.util.Log;

import com.gettingreal.bpos.api.report.ServerReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.http.Body;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

/**
 * Created by ivanfoong on 26/5/14.
 */
public class ServerAPIClient implements IServerAPI {
    private RestAdapter mRestAdapter;
    private IServerAPI mServerAPI;
    private String mServerUrl;

    public ServerAPIClient() {
    }

    public ServerAPIClient(String aServerUrl) {
        this();
        mServerUrl = aServerUrl;
    }

    @Override
    public Collection<ServerProduct> getProducts() {
        Collection<ServerProduct> serverProducts = getServerAPI().getProducts();
        for (ServerProduct serverProduct : serverProducts) {
            Log.d("Product", serverProduct.name);
        }

        return serverProducts;
    }

    @Override
    public void deleteProducts(Callback<String> aCallback) {
        getServerAPI().deleteProducts(aCallback);
    }

    @Override
    public void createProduct(ServerProduct product, TypedFile aImage, Callback<String> aCallback) {
        getServerAPI().createProduct(product, aImage, aCallback);
    }

    @Override
    public Collection<ServerCategory> getCategories() {
        Collection<ServerCategory> serverCategories = getServerAPI().getCategories();
        for (ServerCategory serverCategory : serverCategories) {
            Log.d("Category", serverCategory.name);
        }

        return serverCategories;
    }

    @Override
    public void deleteCategories(Callback<String> aCallback) {
        getServerAPI().deleteCategories(aCallback);
    }

    @Override
    public void createCategory(ServerCategory product, Callback<String> aCallback) {
        getServerAPI().createCategory(product, aCallback);
    }

    @Override
    public Collection<ServerPrinter> getPrinters() {
        Collection<ServerPrinter> serverPrinters = getServerAPI().getPrinters();
        for (ServerPrinter serverPrinter : serverPrinters) {
            Log.d("Printer", serverPrinter.name);
        }

        return serverPrinters;
    }

    @Override
    public void deletePrinters(Callback<String> aCallback) {
        getServerAPI().deletePrinters(aCallback);
    }

    @Override
    public void createPrinter(ServerPrinter printer, Callback<String> aCallback) {
        getServerAPI().createPrinter(printer, aCallback);
    }

    @Override
    public Collection<ServerTable> getTables() {
        Collection<ServerTable> serverTables = getServerAPI().getTables();
        for (ServerTable serverTable : serverTables) {
            Log.d("Table", serverTable.name);
        }

        return serverTables;
    }

    @Override
    public void deleteTables(Callback<String> aCallback) {
        getServerAPI().deleteTables(aCallback);
    }

    @Override
    public void createTable(ServerTable table, Callback<String> aCallback) {
        getServerAPI().createTable(table, aCallback);
    }

    @Override
    public Collection<ServerOrder> getOrders() {
        Collection<ServerOrder> serverOrders = getServerAPI().getOrders();
        for (ServerOrder serverOrder : serverOrders) {
            Log.d("Order", String.valueOf(serverOrder.id));
        }

        return serverOrders;
    }

    @Override
    public void getOpenOrders(Callback<Collection<ServerOrder>> aCallback) {
        getServerAPI().getOpenOrders(aCallback);
    }

    @Override
    public void getClosedOrders(Callback<Collection<ServerOrder>> aCallback) {
        getServerAPI().getClosedOrders(aCallback);
    }

    @Override
    public void getUndeliveredOrders(Callback<Collection<ServerOrder>> aCallback) {
        getServerAPI().getUndeliveredOrders(aCallback);
    }

    @Override
    public void getUnpaidOrders(Callback<Collection<ServerOrder>> aCallback) {
        getServerAPI().getUnpaidOrders(aCallback);
    }

    @Override
    public void createOrder(ServerOrder order, Callback<ServerPostOrderResponse> aCallback) {
        getServerAPI().createOrder(order, aCallback);
    }

    @Override
    public void getOrderItems(Callback<Collection<ServerOrderItem>> aCallback) {
        getServerAPI().getOrderItems(aCallback);
    }

    @Override
    public void getOrderItemsForOrderId(Long orderId, Callback<Collection<ServerOrderItem>> aCallback) {
        getServerAPI().getOrderItemsForOrderId(orderId, aCallback);
    }

    @Override
    public void updateOrderItemForOrderId(@Body ServerOrderItem aServerOrderItem, Callback<Collection<ServerOrderItem>> aCallback) {
        getServerAPI().updateOrderItemForOrderId(aServerOrderItem, aCallback);
    }

    @Override
    public void deleteOrderItemForOrderId(@Query("order_id") Long orderId, @Query("product_uid") String productUid, Callback<Collection<ServerOrderItem>> aCallback) {
        getServerAPI().deleteOrderItemForOrderId(orderId, productUid, aCallback);
    }

    @Override
    public Collection<ServerReceipt> getReceipts() {
        Collection<ServerReceipt> serverReceipts = getServerAPI().getReceipts();
        for (ServerReceipt serverReceipt : serverReceipts) {
            Log.d("Receipt", String.valueOf(serverReceipt.id));
        }

        return serverReceipts;
    }

    @Override
    public void createReceipt(ServerReceipt receipt, Callback<ServerPostReceiptResponse> aCallback) {
        getServerAPI().createReceipt(receipt, aCallback);
    }

    @Override
    public Collection<ServerReceiptHeader> getReceiptHeaders() {
        Collection<ServerReceiptHeader> serverReceiptHeaders = getServerAPI().getReceiptHeaders();
        for (ServerReceiptHeader serverReceiptHeader : serverReceiptHeaders) {
            Log.d("Receipt header", String.valueOf(serverReceiptHeader.id));
        }

        return serverReceiptHeaders;
    }

    @Override
    public void deleteReceiptHeaders(Callback<String> aCallback) {
        getServerAPI().deleteReceiptHeaders(aCallback);
    }

    @Override
    public void createReceiptHeader(@Body ServerReceiptHeader aReceiptHeader, Callback<Collection<ServerReceiptHeader>> aCallback) {
        getServerAPI().createReceiptHeader(aReceiptHeader, aCallback);
    }

    @Override
    public Collection<ServerSurcharge> getSurcharges() {
        Collection<ServerSurcharge> serverSurcharges = getServerAPI().getSurcharges();
        for (ServerSurcharge serverSurcharge : serverSurcharges) {
            Log.d("Surcharge", String.valueOf(serverSurcharge.name));
        }

        return serverSurcharges;
    }

    @Override
    public void deleteSurcharges(Callback<String> aCallback) {
        getServerAPI().deleteSurcharges(aCallback);
    }

    @Override
    public void createSurcharge(@Body ServerSurcharge aSurcharge, Callback<Collection<ServerSurcharge>> aCallback) {
        getServerAPI().createSurcharge(aSurcharge, aCallback);
    }

    @Override
    public ServerReport getReport() {
        return getServerAPI().getReport();
    }

    @Override
    public void getReport(Callback<ServerReport> aCallback) {
        getServerAPI().getReport(aCallback);
    }

    @Override
    public ServerReport getReport(String startDate, String endDate) {
        return getServerAPI().getReport(startDate, endDate);
    }

    @Override
    public void getOnline(Callback<ServerStatus> aCallback) {
        getServerAPI().getOnline(aCallback);
    }

    public ServerReport getReport(Date startDate, Date endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("Y-m-d");
        return getReport(sdf.format(startDate), sdf.format(endDate));
    }

    public String getServerUrl() {
        if (mServerUrl == null) {
            mServerUrl = "http://192.168.192.168/";
        }
        return mServerUrl;
    }

    public RestAdapter getRestAdapter() {
        if (mRestAdapter == null) {
            OkClient httpClient = new OkClient();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .create();

            mRestAdapter = new RestAdapter.Builder()
                .setEndpoint(getServerUrl()) // The base API endpoint.
                .setClient(httpClient)
                .setConverter(new GsonConverter(gson))
                .build();
        }
        return mRestAdapter;
    }

    public IServerAPI getServerAPI() {
        if (mServerAPI == null) {
            mServerAPI = getRestAdapter().create(IServerAPI.class);
        }
        return mServerAPI;
    }
}
