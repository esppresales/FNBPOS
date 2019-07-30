package com.gettingreal.bpos.api;

import com.gettingreal.bpos.api.report.ServerReport;

import java.util.Collection;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Query;
import retrofit.mime.TypedFile;

/**
 * Created by ivanfoong on 26/5/14.
 */
public interface IServerAPI {
    @GET("/api/v1/products.php")
    public Collection<ServerProduct> getProducts();

    @DELETE("/api/v1/products.php")
    public void deleteProducts(Callback<String> aCallback);

    @Multipart
    @POST("/api/v1/products.php")
    public void createProduct(@Part("product") ServerProduct product, @Part("image") TypedFile aImage, Callback<String> aCallback);

    @GET("/api/v1/categories.php")
    public Collection<ServerCategory> getCategories();

    @DELETE("/api/v1/categories.php")
    public void deleteCategories(Callback<String> aCallback);

    @POST("/api/v1/categories.php")
    public void createCategory(@Body ServerCategory category, Callback<String> aCallback);

    @GET("/api/v1/printers.php")
    public Collection<ServerPrinter> getPrinters();

    @DELETE("/api/v1/printers.php")
    public void deletePrinters(Callback<String> aCallback);

    @POST("/api/v1/printers.php")
    public void createPrinter(@Body ServerPrinter printer, Callback<String> aCallback);

    @GET("/api/v1/tables.php")
    public Collection<ServerTable> getTables();

    @DELETE("/api/v1/tables.php")
    public void deleteTables(Callback<String> aCallback);

    @POST("/api/v1/tables.php")
    public void createTable(@Body ServerTable table, Callback<String> aCallback);

    @GET("/api/v1/orders.php")
    public Collection<ServerOrder> getOrders();

    @GET("/api/v1/orders.php?state=open")
    public void getOpenOrders(Callback<Collection<ServerOrder>> aCallback);

    @GET("/api/v1/orders.php?state=closed")
    public void getClosedOrders(Callback<Collection<ServerOrder>> aCallback);

    @GET("/api/v1/orders.php?state=undelivered")
    public void getUndeliveredOrders(Callback<Collection<ServerOrder>> aCallback);

    @GET("/api/v1/orders.php?state=unpaid")
    public void getUnpaidOrders(Callback<Collection<ServerOrder>> aCallback);

    @POST("/api/v1/orders.php")
    public void createOrder(@Body ServerOrder order, Callback<ServerPostOrderResponse> aCallback);

    @GET("/api/v1/order_items.php")
    public void getOrderItems(Callback<Collection<ServerOrderItem>> aCallback);
     @GET("/api/v1/order_items.php")
    public void getOrderItemsForOrderId(@Query("order_id") Long orderId, Callback<Collection<ServerOrderItem>> aCallback);

    @PATCH("/api/v1/order_items.php")
    public void updateOrderItemForOrderId(@Body ServerOrderItem aServerOrderItem, Callback<Collection<ServerOrderItem>> aCallback);

    @DELETE("/api/v1/order_items.php")
    public void deleteOrderItemForOrderId(@Query("order_id") Long orderId, @Query("product_uid") String productUid, Callback<Collection<ServerOrderItem>> aCallback);

    @GET("/api/v1/receipts.php")
    public Collection<ServerReceipt> getReceipts();

    @POST("/api/v1/receipts.php")
    public void createReceipt(@Body ServerReceipt receipt, Callback<ServerPostReceiptResponse> aCallback);

    @GET("/api/v1/receipt_headers.php")
    public Collection<ServerReceiptHeader> getReceiptHeaders();

    @DELETE("/api/v1/receipt_headers.php")
    public void deleteReceiptHeaders(Callback<String> aCallback);

    @POST("/api/v1/receipt_headers.php")
    public void createReceiptHeader(@Body ServerReceiptHeader aReceiptHeader, Callback<Collection<ServerReceiptHeader>> aCallback);

    @GET("/api/v1/surcharges.php")
    public Collection<ServerSurcharge> getSurcharges();

    @DELETE("/api/v1/surcharges.php")
    public void deleteSurcharges(Callback<String> aCallback);

    @POST("/api/v1/surcharges.php")
    public void createSurcharge(@Body ServerSurcharge aSurcharge, Callback<Collection<ServerSurcharge>> aCallback);

    @GET("/api/v1/reports.php")
    public ServerReport getReport();

    @GET("/api/v1/reports.php")
    public void getReport(Callback<ServerReport> aCallback);

    @GET("/api/v1/reports.php")
    public ServerReport getReport(@Query("start_time") String startDate, @Query("end_time") String endDate);

    @GET("/api/v1/online.php")
    public void getOnline(Callback<ServerStatus> aCallback);
}
