package com.gettingreal.bpos.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.epson.eposprint.DrawerOpenEventListener;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSReceipt;
import com.gettingreal.bpos.model.POSReceiptHeader;
import com.gettingreal.bpos.model.POSSurcharge;
import com.gettingreal.bpos.model.POSTable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by ivanfoong on 16/6/14.
 */
public class PrintHelper {
    private static Context context = null;
    private static Socket connection = null;
    private static BufferedReader reader = null;
    private static BufferedWriter writer = null;
    private static int socketConnectTimeout = 5000;
    private static String deviceIDprinter = null;
    private static String receiptContent = null;
    private static Thread onReceiveThread;
    private static boolean connecting = false;
    private static boolean disconnecting = false;
    private static ArrayList<PrintJob> printJobs = new ArrayList<PrintJob>();
    private static PrintJob currentPrintJob = null;
    private static PrintJob retryPrintJob = null;
    private static String failedPrintDescription = null;
    private static boolean retryPrintDialogShown = false;
    private static Timer timer = null;
    private static final int FONT_A_MAX_LINE_CHAR_NUM = 42;
    private static final int FONT_B_MAX_LINE_CHAR_NUM = 56;
    private static final int LEFT_PADDING_COUNT = 1;
    private static final int RIGHT_PADDING_COUNT = 1;
    private static final String PADDING_CHAR = " ";

    private static PrintHelper instance = null;

    public static PrintHelper getSharedInstance() {
        if (instance == null) {
            instance = new PrintHelper();
        }
        return instance;
    }

    public void printOrderItems(final Activity aActivity, final String ipAddress, final int port, final String deviceIDprinter, final POSOrder order, final ArrayList<POSOrderItem> orderItems) {
        context = aActivity;
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String currentTimestamp = df.format(Calendar.getInstance().getTime());

        String tableName = "-----------";
        if (order.getTableUid() != null) {
            POSTable selectedPOSTable = POSTable.getTable(context, order.getTableUid());
            tableName = selectedPOSTable.getName();
        }

        final StringBuilder headerSB = new StringBuilder();

        headerSB.append("<epos-print xmlns=\"http://www.epson-pos.com/schemas/2011/03/epos-print\">");
        headerSB.append("<text align=\"center\"/>");
        headerSB.append("<text font=\"font_b\"/>");

        ArrayList<String> items = new ArrayList<String>();
        items.add(currentTimestamp);
        items.add(String.format("Order #%d", order.getId()));
        headerSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");

        items = new ArrayList<String>();
        items.add(tableName);
        items.add(order.getOrderingMode() == POSOrder.OrderingMode.TAKE_AWAY ? "Take Away" : "Dine-In");
        headerSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");

        headerSB.append("<text>________________________________________________________&#10;</text>");

        final StringBuilder footerSB = new StringBuilder();
        footerSB.append("<text>&#10;</text>");
        footerSB.append("<cut type=\"feed\"/>");
        footerSB.append("</epos-print>");

        StringBuilder sb = new StringBuilder();
        for (POSOrderItem orderItem : orderItems) {
            POSProduct product = POSProduct.getProduct(context, orderItem.getProductUid());

            ArrayList<String> printItems = new ArrayList<String>();
            printItems.add("Qty " + orderItem.getQuantityOrdered());
            printItems.add(product.getName());
            printItems.add("");
            sb.append("<text>" + formatItemsForFontB(printItems) + "</text>");

            if (orderItem.getRemark() != null && !orderItem.getRemark().isEmpty()) {
                ArrayList<String> remarkPrintItems = new ArrayList<String>();
                remarkPrintItems.add(orderItem.getRemark());
                sb.append("<text>");
                sb.append(formatItemsForMaxCharacterCount(remarkPrintItems, FONT_B_MAX_LINE_CHAR_NUM, 11, RIGHT_PADDING_COUNT, PADDING_CHAR));
                sb.append("</text>");
            }
        }

        final String orderContent = headerSB.toString() + sb.toString() + footerSB.toString();

        addPrintJob(aActivity, ipAddress, port, deviceIDprinter, orderContent);
    }

    public void printReceipt(final Activity aActivity, final String ipAddress, final int port, final String deviceIDprinter, final POSReceipt receipt) {
        context = aActivity;

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String currentTimestamp = df.format(Calendar.getInstance().getTime());

        if (receipt.getOrders().size() > 0) {
            Hashtable<String, POSProduct> productsLookupTable = new Hashtable<String, POSProduct>();
            Hashtable<String, Integer> orderProductQuantity = new Hashtable<String, Integer>();
            ArrayList<POSOrderItem> totalOrderItems = new ArrayList<POSOrderItem>();

            for (POSOrder order : receipt.getOrders()) {
                for (POSOrderItem item : order.getOrderItems()) {
                    totalOrderItems.add(item);
                    if (!productsLookupTable.containsKey(item.getProductUid())) {
                        POSProduct product = POSProduct.getProduct(context, item.getProductUid());
                        productsLookupTable.put(item.getProductUid(), product);
                    }

                    if (!orderProductQuantity.containsKey(item.getProductUid())) {
                        orderProductQuantity.put(item.getProductUid(), item.getQuantityOrdered());
                    } else {
                        Integer existingQuantity = orderProductQuantity.get(item.getProductUid());
                        Integer newQuantity = existingQuantity + item.getQuantityOrdered();
                        orderProductQuantity.put(item.getProductUid(), newQuantity);
                    }
                }
            }

            String orderingMode = (receipt.getOrders().get(0).getOrderingMode() == POSOrder.OrderingMode.TAKE_AWAY ? "Take Away" : "Dine-In");

            String tableName = "-----------";
            if (receipt.getOrders().get(0).getTableUid() != null) {
                POSTable selectedPOSTable = POSTable.getTable(context, receipt.getOrders().get(0).getTableUid());
                tableName = selectedPOSTable.getName();
            }

            BigDecimal subtotal = PaymentHelper.calculateSubTotalForOrder(context, totalOrderItems);

            subtotal = subtotal.subtract(receipt.getDiscountAmount());

            BigDecimal totalAmount = PaymentHelper.calculateTotalForSubTotal(context, subtotal);

            final StringBuilder headerSB = new StringBuilder();
            headerSB.append("<epos-print xmlns=\"http://www.epson-pos.com/schemas/2011/03/epos-print\">");
            headerSB.append("<text align=\"center\"/>");

            int headerIndex = 0;

            for (POSReceiptHeader receiptHeader : POSReceiptHeader.getAllReceiptHeaders(context)) {
                if (headerIndex==0) {
                    headerSB.append("<text reverse=\"false\" ul=\"false\" em=\"true\" color=\"color_1\"/>");
                    headerSB.append("<text font=\"font_a\"/>");
                    headerSB.append("<text>" + receiptHeader.getContent() + "&#10;</text>");
                    headerSB.append("<text font=\"font_b\"/>");
                    headerSB.append("<text reverse=\"false\" ul=\"false\" em=\"false\" color=\"color_1\"/>");
                }
                else {
                    headerSB.append("<text>" + receiptHeader.getContent() + "&#10;</text>");
                }
                headerIndex++;
            }
            headerSB.append("<text>________________________________________________________&#10;</text>");

            ArrayList<String> items = new ArrayList<String>();
            items.add(currentTimestamp);
            items.add(String.format("Receipt #%d", receipt.getId()));
            headerSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");

            items = new ArrayList<String>();
            items.add(tableName);
            items.add(orderingMode);
            headerSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");

            headerSB.append("<text>________________________________________________________&#10;</text>");

            final StringBuilder surchargeSB = new StringBuilder();

            BigDecimal totalForCalculation = subtotal;
            for (POSSurcharge surcharge : POSSurcharge.getAllSurcharges(context)) {
                BigDecimal surchargeAmount = totalForCalculation.multiply(BigDecimal.valueOf(surcharge.getPercentage()/100.0));

                items = new ArrayList<String>();
                items.add("");
                items.add(surcharge.getName());
                items.add(String.format("%.2f", surchargeAmount));
                surchargeSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");

                totalForCalculation = totalForCalculation.add(surchargeAmount);
            }

            final StringBuilder footerSB = new StringBuilder();
            footerSB.append("<text>          __________________________________________&#10;</text>");

            if (receipt.getDiscountAmount().doubleValue() > 0.0) {
                items = new ArrayList<String>();
                items.add("");
                items.add("Discount");
                items.add(String.format("-%.2f", receipt.getDiscountAmount()));
                footerSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");
            }

            footerSB.append(surchargeSB.toString());
            footerSB.append("<text reverse=\"false\" ul=\"false\" em=\"true\" color=\"color_1\"/>");

            items = new ArrayList<String>();
            items.add("");
            items.add("TOTAL");
            items.add(String.format("%.2f", totalAmount));
            footerSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");

            footerSB.append("<text font=\"font_b\"/>");
            footerSB.append("<text reverse=\"false\" ul=\"false\" em=\"false\" color=\"color_1\"/>");

            items = new ArrayList<String>();
            items.add("");
            items.add("CASH");
            items.add(String.format("%.2f", receipt.getPaidAmount()));
            footerSB.append("<text>" + formatItemsForFontB(items) + "&#10;</text>");

            footerSB.append("<text font=\"font_a\"/>");
            footerSB.append("<text reverse=\"false\" ul=\"false\" em=\"true\" color=\"color_1\"/>");

            items = new ArrayList<String>();
            items.add("");
            items.add("CHANGE");
            items.add(String.format("%.2f", receipt.getPaidAmount().subtract(totalAmount)));
            footerSB.append("<text>" + formatItemsForFontA(items) + "&#10;</text>");

            footerSB.append("<cut type=\"feed\"/>");
            footerSB.append("</epos-print>");

            StringBuilder sb = new StringBuilder();
            for (String productUid : orderProductQuantity.keySet()) {
                POSProduct product = productsLookupTable.get(productUid);

                sb.append("<text>");
                items = new ArrayList<String>();
                items.add(String.format("Qty %d", orderProductQuantity.get(productUid)));
                items.add(product.getName());
                items.add(String.format("%.2f", product.getPrice() * orderProductQuantity.get(productUid)));
                sb.append(formatItemsForFontB(items));
                sb.append("&#10;</text>");
            }

            final String receiptContent = headerSB.toString() + sb.toString() + footerSB.toString();

            addPrintJob(aActivity, ipAddress, port, deviceIDprinter, receiptContent);
        } else {
            // TODO prompt unable to print receipt for no orders
        }
    }

    public void addPrintJob(final Activity activity, final String ipAddress, final int port, final String deviceIDprinter, final String xmlContent) {
        PrintJob printJob = new PrintJob(activity, ipAddress, port, deviceIDprinter, xmlContent);
        printJobs.add(printJob);
        startPrintJobQueue();
    }

    public class PrintJob {
        public Activity activity;
        public String ipAddress;
        public int port;
        public String deviceIDprinter;
        public String xmlContent;

        public PrintJob(final Activity activity, final String ipAddress, final int port, final String deviceIDprinter, final String xmlContent) {
            this.activity = activity;
            this.ipAddress = ipAddress;
            this.port = port;
            this.deviceIDprinter = deviceIDprinter;
            this.xmlContent = xmlContent;
        }
    }

    public void startPrintJobQueue() {
        if (timer == null) {
            timer = new Timer();
            //timer.schedule(new PrintJobTimerTask(), 0, 600);
        }
    }

    public void retryPrintJob() {
        if (retryPrintJob != null) {
            currentPrintJob = retryPrintJob;
            retryPrintJob = null;
            failedPrintDescription = null;

            PrintTaskParam printTaskParam = new PrintTaskParam(currentPrintJob.activity, currentPrintJob.ipAddress, currentPrintJob.port, currentPrintJob.deviceIDprinter, currentPrintJob.xmlContent);
            new PrintTask().execute(printTaskParam);
        }
    }

    class PrintJobTimerTask extends TimerTask {

        @Override
        public void run() {
            Log.d("job count", String.format("%d", printJobs.size()));
            if (retryPrintJob != null) {
                if (!retryPrintDialogShown) {
                    retryPrintDialogShown = true;
                    retryPrintJob.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(context)
                                    .setTitle("Failed to print!")
                                    .setMessage(failedPrintDescription)
                                    .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface aDialogInterface, int i) {
                                            retryPrintJob = null;
                                            failedPrintDescription = null;
                                            retryPrintDialogShown = false;
                                        }
                                    })
                                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            retryPrintJob();
                                            retryPrintDialogShown = false;
                                        }
                                    })
                                    .setCancelable(false)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    });
                }
            }
            else if (currentPrintJob == null && printJobs.size() > 0) {
                currentPrintJob = printJobs.get(0);
                printJobs.remove(0);

                PrintTaskParam printTaskParam = new PrintTaskParam(currentPrintJob.activity, currentPrintJob.ipAddress, currentPrintJob.port, currentPrintJob.deviceIDprinter, currentPrintJob.xmlContent);
                new PrintTask().execute(printTaskParam);
            }
        }
    }

    private void showErrorDialog(final String errorMessage) {
        new AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage(errorMessage)
                .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private String repeatStringForCount(final String str, final int count) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private String formatItemsForMaxCharacterCount(final List<String> items, final int maxCharacterCount, final int leftPaddingCount, final int rightPaddingCount, final String paddingChar) throws UnsupportedOperationException {
        String line = "";
        if (items.size() == 1) {
            final String firstItem = items.get(0);
            final String leftPadding = repeatStringForCount(paddingChar, leftPaddingCount);
            final String rightPadding = repeatStringForCount(paddingChar, rightPaddingCount);

            StringBuilder sb = new StringBuilder();
            sb.append(leftPadding);
            sb.append(firstItem);

            final int existingCharCount = (sb.length() + rightPadding.length());
            final int paddingCount = maxCharacterCount - existingCharCount;

            sb.append(repeatStringForCount(paddingChar, paddingCount));
            sb.append(rightPadding);

            line = sb.toString();
        }
        else if (items.size() == 2) {
            final String firstItem = items.get(0);
            final String secondItem = items.get(1);

            final String leftPadding = repeatStringForCount(paddingChar, leftPaddingCount);
            final String rightPadding = repeatStringForCount(paddingChar, rightPaddingCount);

            StringBuilder sb = new StringBuilder();
            sb.append(leftPadding);
            sb.append(firstItem);

            final int existingCharCount = (sb.length() + secondItem.length() + rightPadding.length());
            final int innerPaddingCount = maxCharacterCount - existingCharCount;

            sb.append(repeatStringForCount(paddingChar, innerPaddingCount));
            sb.append(secondItem);
            sb.append(rightPadding);

            line = sb.toString();
        }
        else if (items.size() == 3) {
            final String firstItem = items.get(0);
            final String secondItem = items.get(1);
            final String thirdItem = items.get(2);

            final String leftPadding = repeatStringForCount(paddingChar, leftPaddingCount);
            final String rightPadding = repeatStringForCount(paddingChar, rightPaddingCount);

            StringBuilder sb = new StringBuilder();
            sb.append(leftPadding);
            sb.append(firstItem);

            int firstItemFixedCharCount;
            if (maxCharacterCount == FONT_A_MAX_LINE_CHAR_NUM) {
                firstItemFixedCharCount = 7;
            }
            else {
                firstItemFixedCharCount = 10;
            }

            final int firstPaddingCount = firstItemFixedCharCount - sb.length();
            sb.append(repeatStringForCount(paddingChar, firstPaddingCount));

            sb.append(secondItem);

            final int existingCharCount = (sb.toString().length() + thirdItem.length() + rightPadding.length());
            final int innerPaddingCount = maxCharacterCount - existingCharCount;

            sb.append(repeatStringForCount(paddingChar, innerPaddingCount));
            sb.append(thirdItem);
            sb.append(rightPadding);

            line = sb.toString();
        }
        else {
            throw new UnsupportedOperationException("Item size not supported (item size given = " + items.size() + ")");
        }

        return line;
    }

    private String formatItemsForFontA(final List<String> items) {
        return formatItemsForMaxCharacterCount(items, FONT_A_MAX_LINE_CHAR_NUM, LEFT_PADDING_COUNT, RIGHT_PADDING_COUNT, PADDING_CHAR);
    }

    private String formatItemsForFontB(final List<String> items) {
        return formatItemsForMaxCharacterCount(items, FONT_B_MAX_LINE_CHAR_NUM, LEFT_PADDING_COUNT, RIGHT_PADDING_COUNT, PADDING_CHAR);
    }

    class PrintTaskParam {
        private Activity activity = null;
        private String ipAddress = null;
        private int port = 0;
        private String deviceId = null;
        private String xmlContent = null;

        PrintTaskParam(Activity aActivity, String aIpAddress, int aPort, String aDeviceId, String aXmlContent) {
            activity = aActivity;
            ipAddress = aIpAddress;
            port = aPort;
            deviceId = aDeviceId;
            xmlContent = aXmlContent;
        }

        public Activity getActivity() {
            return activity;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getXmlContent() {
            return xmlContent;
        }
    }

    class PrintTask extends AsyncTask<PrintTaskParam, Void, String> {

        private Socket connection = null;
        private BufferedReader reader = null;
        private BufferedWriter writer = null;
        private ArrayList<String> errorMessages = new ArrayList<String>();

        @Override
        protected String doInBackground(PrintTaskParam... aPrintTaskParams) {
            String error = null;
            for (PrintTaskParam printTaskParam : aPrintTaskParams) {
                // connect socket
                if (this.connect(printTaskParam.getIpAddress(), printTaskParam.getPort())) {
                    // send "open device" command
                    if (this.openDevice(printTaskParam.getDeviceId())) {
                        // send "print" command
                        String errorMessage = this.print(printTaskParam.getDeviceId(), printTaskParam.getXmlContent());
                        if (errorMessage != null) {
                            error = errorMessage;
                        }

                        // send "close device" command
                        this.closeDevice(printTaskParam.getDeviceId());
                    }
                    else {
                        error = "Unable to open device";
                    }
                }
                else {
                    error = "Unable to connect to server";
                }

                // close socket
                this.closeSocket();
            }

            return error;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                retryPrintJob = currentPrintJob;
                failedPrintDescription = result;
            }
            currentPrintJob = null;
        }

        private boolean connect(final String aIpAddress, final int aPort) {
            InetSocketAddress serverAddress = new InetSocketAddress(aIpAddress, aPort);

            try {
                connection = new Socket();
                connection.connect(serverAddress, socketConnectTimeout);
            } catch (UnknownHostException e) {
                //appendConsole("Connecting to server failed.");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
                connection = null;
                //enableButton(true);
                return false;
            } catch (IOException e) {
                //appendConsole("Connecting to server failed.");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
                connection = null;
                //enableButton(true);
                return false;
            }

            try {
                reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(
                        connection.getOutputStream()));

                connection.setSoTimeout(5000);

                // Receive reply message from server
                int chr;
                StringBuffer buffer = new StringBuffer();
                while ((chr = reader.read()) != 0) {
                    buffer.append((char) chr);
                }

                Log.d("buffer1", buffer.toString());

                // Parse recieved xml document(DOM)
                DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(buffer.toString()
                        .getBytes("UTF-8")));
                String firstNode = doc.getFirstChild().getNodeName();

                // Response of connect request
                if (firstNode.equals("connect")) {
                    //appendConsole("Connect to server success.");
                    return true;
                } else {
//                appendConsole("Connect to server failed.");
                    errorMessages.add("Failed to connect to server");
                }
            } catch (SocketTimeoutException e) {
//            appendConsole("Read message timed out.");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (IOException e) {
//            appendConsole("Disconnected.");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (ParserConfigurationException e) {
//            appendConsole("XML parse error.");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (SAXException e) {
//            appendConsole("XML parse error.");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            }
            return false;
        }

        private boolean openDevice(final String aDeviceId) {
            String req = "<open_device>" + "<device_id>" + aDeviceId
                    + "</device_id>" + "<data>" + "<type>type_printer</type>"
                    + "</data>" + "</open_device>" + "\0";

            try {
                Log.d("write1", req);
                writer.write(req);
                writer.flush();

                int chr;
                StringBuffer buffer = new StringBuffer();
                while ((chr = reader.read()) > 0) {
                    buffer.append((char) chr);
                }

                Log.d("buffer2", buffer.toString());

                // Parse recieved xml document(DOM)
                DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(buffer.toString()
                        .getBytes("UTF-8")));
                String firstNode = doc.getFirstChild().getNodeName();

                // Response of open_device request
                if (firstNode.equals("open_device")) {
                    String id = getChildren(doc, "device_id");
                    String code = getChildren(doc, "code");
//                    appendConsole(id + " : " + code);

                    if (id.equals(aDeviceId) && code.equals("OK")) {
                        return true;
                    }
                    else {
                        // TODO failed to open device
                    }
                }

            } catch (IOException e) {
//            appendConsole("Disconnected");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (SAXException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            }
            return false;
        }

        private boolean closeDevice(final String aDeviceId) {
            String req = "<close_device>" + "<device_id>" + aDeviceId
                    + "</device_id></close_device>" + "\0";

            try {
                Log.d("write1", req);
                writer.write(req);
                writer.flush();

                int chr;
                StringBuffer buffer = new StringBuffer();
                while ((chr = reader.read()) > 0) {
                    buffer.append((char) chr);
                }

                Log.d("buffer2", buffer.toString());

                // Parse recieved xml document(DOM)
                DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(buffer.toString()
                        .getBytes("UTF-8")));
                String firstNode = doc.getFirstChild().getNodeName();

                // Response of close_device request
                if (firstNode.equals("close_device")) {
                    String id = getChildren(doc, "device_id");
                    String code = getChildren(doc, "code");
//                    appendConsole(id + " : " + code);

                    if (id.equals(deviceIDprinter) && code.equals("OK")) {
                        return true;
                    }
                    else {
                        // TODO failed to close device
                    }
                }

            } catch (IOException e) {
//            appendConsole("Disconnected");
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (SAXException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            }
            return false;
        }

        private String print(final String aDeviceId, final String aPrintData) {
            String req = "<device_data>"
                    + "<sequence>100</sequence>"
                    + "<device_id>"
                    + aDeviceId
                    + "</device_id>"
                    + "<data>"
                    + "<type>print</type>"
                    + "<timeout>10000</timeout>"
                    + "<printdata>"
                    + aPrintData
                    + "</printdata>" + "</data>"
                    + "</device_data>" + "\0";

            try {
                writer.write(req);
                writer.flush();

                int chr;
                StringBuffer buffer = new StringBuffer();
                while ((chr = reader.read()) > 0) {
                    buffer.append((char) chr);
                }

                Log.d("buffer2", buffer.toString());

                // Parse recieved xml document(DOM)
                DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(buffer.toString()
                        .getBytes("UTF-8")));
                String firstNode = doc.getFirstChild().getNodeName();

                // Response of print request
                if (firstNode.equals("device_data")) {
                    Node response = doc.getElementsByTagName("response").item(0);
                    Node success = response.getAttributes().getNamedItem("success");
                    Node code = response.getAttributes().getNamedItem("code");

                    if (success.getNodeValue().equalsIgnoreCase("true")) {
                        return null;
                    }
                    else if (code != null) {
                        String codeValue = code.getNodeValue();
                        if (codeValue.equalsIgnoreCase("EPTR_AUTOMATICAL")) {
                            return "An automatically recoverable error occurred";
                        }
                        else if (codeValue.equalsIgnoreCase("EPTR_COVER_OPEN")) {
                            return "A cover open error occurred";
                        }
                        else if (codeValue.equalsIgnoreCase("EPTR_CUTTER")) {
                            return "An autocutter error occurred";
                        }
                        else if (codeValue.equalsIgnoreCase("EPTR_MECHANICAL")) {
                            return "A mechanical error occurred";
                        }
                        else if (codeValue.equalsIgnoreCase("EPTR_REC_EMPTY")) {
                            return "No paper in roll paper end sensor";
                        }
                        else if (codeValue.equalsIgnoreCase("EPTR_UNRECOVERABLE")) {
                            return "An unrecoverable error occurred";
                        }
                        else if (codeValue.equalsIgnoreCase("SchemaError")) {
                            return "The request document contains a syntax error";
                        }
                        else if (codeValue.equalsIgnoreCase("DeviceNotFound")) {
                            return "The printer with the specified device ID does not exist";
                        }
                        else if (codeValue.equalsIgnoreCase("PrintSystemError")) {
                            return "An error occurred on the printing system";
                        }
                        else if (codeValue.equalsIgnoreCase("EX_BADPORT")) {
                            return "An error was detected on the communication port";
                        }
                        else if (codeValue.equalsIgnoreCase("EX_TIMEOUT")) {
                            return "A print timeout occurred";
                        }
                    }
                    else {
                        return "Bad response from server";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            } catch (SAXException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            }
            return "Failed to send print job";
        }

        private void closeSocket() {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            }
            reader = null;

            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            }
            writer = null;

            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                errorMessages.add(e.getLocalizedMessage());
            }
            connection = null;
        }

        /**
         * Get value of child node from specified Document object and TagName.
         *
         * @param doc     specified Document object
         * @param tagName specified xml tagname
         * @return String of node value. If there is no such tagname, return null.
         */
        private String getChildren(Document doc, String tagName) {
            NodeList list = doc.getElementsByTagName(tagName);
            if (list.getLength() == 0) {
                return null;
            } else {
                try {
                    Node node = list.item(0);
                    return node.getFirstChild().getNodeValue();
                } catch (NullPointerException e) {
                    return null;
                }
            }
        }

    }

}
