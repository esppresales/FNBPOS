package com.gettingreal.bpos;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;
import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.report.ServerEODReport;
import com.gettingreal.bpos.api.report.ServerMonthlyReport;
import com.gettingreal.bpos.api.report.ServerReport;
import com.gettingreal.bpos.model.POSProduct;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by ivanfoong on 2/7/14.
 */
public class ReportListDetailFragment extends Fragment {
    private ServerAPIClient mServerAPIClient;
    private BarGraph mBarGraphTransactions, mBarGraphAmount, mBarGraphProducts;
    private String mSelectedReportType, mSelectedReportId;
    private TextView mTitleTextView, mDetail1TextView, mDetail2TextView, mDetail3TextView, mDetail4TextView;
    private Button mSaveReportButton;
    private ServerEODReport currentEODReport = null;
    private ServerMonthlyReport currentMonthlyReport = null;
    private static final int startOperationHour = 7;
    private static final int endOperationHour = 22;

    private BroadcastReceiver mReportSelectedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received report-selected broadcast");

            if (intent.hasExtra("report_type") && intent.hasExtra("report_id")) {
                mSelectedReportType = intent.getStringExtra("report_type");
                mSelectedReportId = intent.getStringExtra("report_id");

                updateReportDetails(context, mSelectedReportType, mSelectedReportId);
            }
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_list_detail, container, false);

        mBarGraphTransactions = (BarGraph) view.findViewById(R.id.bargraph_transactions);
        mBarGraphAmount = (BarGraph) view.findViewById(R.id.bargraph_amount);
        mBarGraphProducts = (BarGraph) view.findViewById(R.id.bargraph_products);

        mTitleTextView = (TextView) view.findViewById(R.id.txt_title);
        mDetail1TextView = (TextView) view.findViewById(R.id.txt_detail1);
        mDetail2TextView = (TextView) view.findViewById(R.id.txt_detail2);
        mDetail3TextView = (TextView) view.findViewById(R.id.txt_detail3);
        mDetail4TextView = (TextView) view.findViewById(R.id.txt_detail4);

        mSaveReportButton = (Button) view.findViewById(R.id.button_save_report);
        mSaveReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                exportReport();
            }
        });

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");

        String serverUrl = "http://" + address + "/";

        mServerAPIClient = new ServerAPIClient(serverUrl);

        clearReportDetails();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReportSelectedBroadcastReceiver,
                new IntentFilter("report-selected"));

        clearReportDetails();
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReportSelectedBroadcastReceiver);
        super.onPause();
    }

    private void updateReportDetails(final Context aContext, final String aReportType, final String aSelectedReportId) {
        try {
            if (aReportType.equals("monthly")) {
                SimpleDateFormat inputSDF = new SimpleDateFormat("yyyy-MM");
                Date date = inputSDF.parse(aSelectedReportId);
                SimpleDateFormat outputSDF = new SimpleDateFormat("MMM yyyy");
                mTitleTextView.setText(outputSDF.format(date) + " Report");
            }
            else {
                SimpleDateFormat inputSDF = new SimpleDateFormat("yyyy-MM-dd");
                Date date = inputSDF.parse(aSelectedReportId);
                SimpleDateFormat outputSDF = new SimpleDateFormat("dd MMM yyyy");
                mTitleTextView.setText(outputSDF.format(date) + " Report");
            }

        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        mSaveReportButton.setVisibility(View.VISIBLE);
        mServerAPIClient.getReport(new Callback<ServerReport>() {
            @Override
            public void success(ServerReport aServerReport, Response aResponse) {
                if (aReportType.equals("monthly")) {
                    if (aServerReport.monthly.containsKey(aSelectedReportId)) {
                        currentEODReport = null;
                        ServerMonthlyReport monthlyReport = aServerReport.monthly.get(aSelectedReportId);
                        currentMonthlyReport = monthlyReport;

                        mDetail1TextView.setText("Total Order Amt for Month: " + NumberFormat.getCurrencyInstance().format(monthlyReport.total_amount));
                        mDetail2TextView.setText("Total No. of Transactions for Month: " + NumberFormat.getIntegerInstance().format(monthlyReport.total_order_count));
                        mDetail3TextView.setText("Avg Order Amt Per Day: " + NumberFormat.getCurrencyInstance().format(monthlyReport.average_amount_per_day));
                        mDetail4TextView.setText("Avg No. of Transactions Per Day: " + NumberFormat.getIntegerInstance().format(monthlyReport.average_orders_per_day));
                        mDetail4TextView.setVisibility(View.VISIBLE);

                        final Resources resources = getResources();

                        final int daysInMonth = getNumberOfDaysInMonthYear(monthlyReport.month, monthlyReport.year);

                        // generate order count by days bar values
                        final HashMap<Integer, Integer> orderCounts = new HashMap<Integer, Integer>();
                        for (int i=1; i <= daysInMonth; i++) {
                            orderCounts.put(i, 0);
                        }

                        for (Integer day : monthlyReport.order_count_by_day.keySet()) {
                            Integer count = monthlyReport.order_count_by_day.get(day);
                            orderCounts.put(day, count);
                        }

                        final ArrayList<Bar> orderCountBars = new ArrayList<Bar>();
                        for (Integer day : asSortedList(orderCounts.keySet())) {
                            Integer count = orderCounts.get(day);

                            String title = String.format("%d",day);
                            Bar bar = new Bar();
                            bar.setColor(resources.getColor(R.color.epson));
                            bar.setSelectedColor(resources.getColor(R.color.epson_dark));
                            bar.setName(title);
                            bar.setValue(count);
                            bar.setValueString(String.format("%d",count));
                            orderCountBars.add(bar);
                        }

                        mBarGraphTransactions.setBars(orderCountBars);
                        mBarGraphTransactions.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

                            @Override
                            public void onClick(int index) {
                                Toast.makeText(getActivity(),
                                        orderCountBars.get(index).getValueString(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

                        // generate amount by hours bar values
                        final HashMap<Integer, Double> amounts = new HashMap<Integer, Double>();
                        for (int i=1; i <= daysInMonth; i++) {
                            amounts.put(i, 0.0);
                        }

                        for (Integer day : monthlyReport.amount_by_day.keySet()) {
                            Double amount = monthlyReport.amount_by_day.get(day);
                            amounts.put(day, amount);
                        }

                        final ArrayList<Bar> amountBars = new ArrayList<Bar>();
                        for (Integer day : asSortedList(amounts.keySet())) {
                            Double amount = amounts.get(day);

                            String title = String.format("%d",day);

                            Bar bar = new Bar();
                            bar.setColor(resources.getColor(R.color.epson));
                            bar.setSelectedColor(resources.getColor(R.color.epson_dark));
                            bar.setName(title);
                            bar.setValue(amount.floatValue());
                            bar.setValueString(String.format("%.2f",amount));
                            amountBars.add(bar);
                        }

                        mBarGraphAmount.setBars(amountBars);
                        mBarGraphAmount.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

                            @Override
                            public void onClick(int index) {
                                Toast.makeText(getActivity(),
                                        amountBars.get(index).getValueString(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

                        // generate product ordered bar values
                        final ArrayList<Bar> productsBars = new ArrayList<Bar>();
                        final int maxCount = 5;
                        int currentCount = 0;
                        for (String productUid : sortByValue(monthlyReport.product_ordered_count).keySet()) {
                            currentCount += 1;
                            Integer amount = monthlyReport.product_ordered_count.get(productUid);

                            POSProduct product = POSProduct.getProduct(aContext, productUid);
                            String title = product.getName();

                            Bar bar = new Bar();
                            bar.setColor(resources.getColor(R.color.epson));
                            bar.setSelectedColor(resources.getColor(R.color.epson_dark));
                            bar.setName(title);
                            bar.setValue(amount);
                            bar.setValueString(String.format("%d",amount));
                            productsBars.add(bar);

                            if (currentCount >= maxCount) {
                                break;
                            }
                        }

                        mBarGraphProducts.setBars(productsBars);
                        mBarGraphProducts.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

                            @Override
                            public void onClick(int index) {
                                Toast.makeText(getActivity(),
                                        productsBars.get(index).getValueString(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                }
                else {
                    if (aServerReport.eod.containsKey(aSelectedReportId)) {
                        currentMonthlyReport = null;
                        ServerEODReport eodReport = aServerReport.eod.get(aSelectedReportId);
                        currentEODReport = eodReport;

                        mDetail1TextView.setText("Total Order Amt: " + NumberFormat.getCurrencyInstance().format(eodReport.total_amount));
                        mDetail2TextView.setText("Total No. of Transactions: " + NumberFormat.getIntegerInstance().format(eodReport.total_order_count));
                        mDetail3TextView.setText("Avg Order Amt Per Transaction: " + NumberFormat.getCurrencyInstance().format(eodReport.average_amount_per_order));
                        mDetail4TextView.setText("");
                        mDetail4TextView.setVisibility(View.GONE);

                        final Resources resources = getResources();

                        // generate order count by hours bar values
                        final HashMap<Integer, Integer> orderCounts = new HashMap<Integer, Integer>();
                        for (int i=startOperationHour; i <= endOperationHour; i++) {
                            orderCounts.put(i, 0);
                        }

                        for (Integer hour : eodReport.order_count_by_hour.keySet()) {
                            Integer count = eodReport.order_count_by_hour.get(hour);
                            orderCounts.put(hour, count);
                        }

                        final ArrayList<Bar> orderCountBars = new ArrayList<Bar>();
                        for (Integer hour : asSortedList(orderCounts.keySet())) {
                            Integer count = orderCounts.get(hour);

                            String title = null;
                            if (hour == 12) {
                                title = "noon";
                            }
                            else if (hour == 0) {
                                title = "midnight";
                            }
                            else if (hour > 12) {
                                title = String.format("%dpm",hour-12);
                            }
                            else {
                                title = String.format("%dam",hour);
                            }

                            Bar bar = new Bar();
                            bar.setColor(resources.getColor(R.color.epson));
                            bar.setSelectedColor(resources.getColor(R.color.epson_dark));
                            bar.setName(title);
                            bar.setValue(count);
                            bar.setValueString(String.format("%d",count));
                            orderCountBars.add(bar);
                        }

                        mBarGraphTransactions.setBars(orderCountBars);
                        mBarGraphTransactions.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

                            @Override
                            public void onClick(int index) {
                                Toast.makeText(getActivity(),
                                        orderCountBars.get(index).getValueString(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

                        // generate amount by hours bar values
                        final HashMap<Integer, Double> amounts = new HashMap<Integer, Double>();
                        for (int i=startOperationHour; i <= endOperationHour; i++) {
                            amounts.put(i, 0.0);
                        }

                        for (Integer hour : eodReport.amount_by_hour.keySet()) {
                            Double amount = eodReport.amount_by_hour.get(hour);
                            amounts.put(hour, amount);
                        }

                        final ArrayList<Bar> amountBars = new ArrayList<Bar>();
                        for (Integer hour : asSortedList(amounts.keySet())) {
                            Double amount = amounts.get(hour);

                            String title = null;
                            if (hour == 12) {
                                title = "noon";
                            }
                            else if (hour == 0) {
                                title = "midnight";
                            }
                            else if (hour > 12) {
                                title = String.format("%dpm",hour-12);
                            }
                            else {
                                title = String.format("%dam",hour);
                            }

                            Bar bar = new Bar();
                            bar.setColor(resources.getColor(R.color.epson));
                            bar.setSelectedColor(resources.getColor(R.color.epson_dark));
                            bar.setName(title);
                            bar.setValue(amount.floatValue());
                            bar.setValueString(String.format("%.2f",amount));
                            amountBars.add(bar);
                        }

                        mBarGraphAmount.setBars(amountBars);
                        mBarGraphAmount.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

                            @Override
                            public void onClick(int index) {
                                Toast.makeText(getActivity(),
                                        amountBars.get(index).getValueString(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

                        // generate product ordered bar values
                        final ArrayList<Bar> productsBars = new ArrayList<Bar>();
                        final int maxCount = 5;
                        int currentCount = 0;
                        for (String productUid : sortByValue(eodReport.product_ordered_count).keySet()) {
                            currentCount += 1;
                            Integer amount = eodReport.product_ordered_count.get(productUid);

                            POSProduct product = POSProduct.getProduct(aContext, productUid);
                            String title = product.getName();

                            Bar bar = new Bar();
                            bar.setColor(resources.getColor(R.color.epson));
                            bar.setSelectedColor(resources.getColor(R.color.epson_dark));
                            bar.setName(title);
                            bar.setValue(amount);
                            bar.setValueString(String.format("%d",amount));
                            productsBars.add(bar);

                            if (currentCount >= maxCount) {
                                break;
                            }
                        }

                        mBarGraphProducts.setBars(productsBars);
                        mBarGraphProducts.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {

                            @Override
                            public void onClick(int index) {
                                Toast.makeText(getActivity(),
                                        productsBars.get(index).getValueString(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                    }
                }
            }

            @Override
            public void failure(RetrofitError aRetrofitError) {
                aRetrofitError.printStackTrace();
                final ArrayList<Bar> aBars = new ArrayList<Bar>();
                mBarGraphTransactions.setBars(aBars);
                mBarGraphAmount.setBars(aBars);
                mBarGraphProducts.setBars(aBars);
            }
        });
    }

    private void clearReportDetails() {
        final ArrayList<Bar> aBars = new ArrayList<Bar>();
        mBarGraphTransactions.setBars(aBars);
        mBarGraphAmount.setBars(aBars);
        mBarGraphProducts.setBars(aBars);

        mTitleTextView.setText("");
        mDetail1TextView.setText("");
        mDetail2TextView.setText("");
        mDetail3TextView.setText("");
        mDetail4TextView.setText("");

        mSaveReportButton.setVisibility(View.INVISIBLE);
    }

    private int getNumberOfDaysInMonthYear(int month, int year) {
        // Create a calendar object and set year and month
        Calendar calendar = new GregorianCalendar(year, month, 1);

        // Get the number of days in that month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        return daysInMonth;
    }

    private <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map ) {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>( map.entrySet() );
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        Collections.reverse(list);

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list)
        {
            result.put( entry.getKey(), entry.getValue() );
        }
        return result;
    }

    private void exportReport() {
        if (currentMonthlyReport != null) {
            StringBuffer reportSB = new StringBuffer();

            String subject = String.format("POS Report for %04d-%02d", currentMonthlyReport.year, currentMonthlyReport.month);

            reportSB.append(String.format("%04d-%02d\n", currentMonthlyReport.year, currentMonthlyReport.month));
            reportSB.append("Total Order Amt for Month: " + NumberFormat.getCurrencyInstance().format(currentMonthlyReport.total_amount));
            reportSB.append("\n");
            reportSB.append("Total No. of Transactions for Month: " + NumberFormat.getIntegerInstance().format(currentMonthlyReport.total_order_count));
            reportSB.append("\n");
            reportSB.append("Avg Order Amt Per Day: " + NumberFormat.getCurrencyInstance().format(currentMonthlyReport.average_amount_per_day));
            reportSB.append("\n");
            reportSB.append("Avg No. of Transactions Per Day: " + NumberFormat.getIntegerInstance().format(currentMonthlyReport.average_orders_per_day));
            reportSB.append("\n");
            reportSB.append("\n");

            final int daysInMonth = getNumberOfDaysInMonthYear(currentMonthlyReport.month, currentMonthlyReport.year);

            final HashMap<Integer, Double> amounts = new HashMap<Integer, Double>();
            final HashMap<Integer, Integer> orderCounts = new HashMap<Integer, Integer>();
            for (int i=1; i <= daysInMonth; i++) {
                amounts.put(i, 0.0);
                orderCounts.put(i, 0);
            }
            for (Integer day : currentMonthlyReport.amount_by_day.keySet()) {
                Double amount = currentMonthlyReport.amount_by_day.get(day);
                amounts.put(day, amount);
            }
            for (Integer day : currentMonthlyReport.order_count_by_day.keySet()) {
                Integer orderCount = currentMonthlyReport.order_count_by_day.get(day);
                orderCounts.put(day, orderCount);
            }
            reportSB.append("date,amount,order_count\n");
            for (Integer day : asSortedList(amounts.keySet())) {
                String date = String.format("%04d-%02d-%02d",currentMonthlyReport.year,currentMonthlyReport.month,day);
                reportSB.append(date + "," + amounts.get(day) + "," + orderCounts.get(day) + "\n");
            }
            reportSB.append("\n");

            reportSB.append("Top 5 products\n");
            final int maxCount = 5;
            int currentCount = 0;
            for (String productUid : sortByValue(currentMonthlyReport.product_ordered_count).keySet()) {
                currentCount += 1;
                reportSB.append(productUid + ": " + currentMonthlyReport.product_ordered_count.get(productUid) + "\n");
                if (currentCount >= maxCount) {
                    break;
                }
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, reportSB.toString());

            startActivity(Intent.createChooser(intent, "Send Email"));
        }

        if (currentEODReport != null) {
            StringBuffer reportSB = new StringBuffer();

            String subject = String.format("POS Report for %04d-%02d-%02d", currentEODReport.year, currentEODReport.month, currentEODReport.day);

            reportSB.append(String.format("%04d-%02d-%02d\n", currentEODReport.year, currentEODReport.month, currentEODReport.day));
            reportSB.append("\n");
            reportSB.append("Total Order Amt: " + NumberFormat.getCurrencyInstance().format(currentEODReport.total_amount));
            reportSB.append("\n");
            reportSB.append("Total No. of Transactions: " + NumberFormat.getIntegerInstance().format(currentEODReport.total_order_count));
            reportSB.append("\n");
            reportSB.append("Avg Order Amt Per Transaction: " + NumberFormat.getCurrencyInstance().format(currentEODReport.average_amount_per_order));
            reportSB.append("\n");
            reportSB.append("\n");

            final HashMap<Integer, Double> amounts = new HashMap<Integer, Double>();
            final HashMap<Integer, Integer> orderCounts = new HashMap<Integer, Integer>();
            for (int i=startOperationHour; i <= endOperationHour; i++) {
                amounts.put(i, 0.0);
                orderCounts.put(i, 0);
            }
            for (Integer hour : currentEODReport.amount_by_hour.keySet()) {
                Double amount = currentEODReport.amount_by_hour.get(hour);
                amounts.put(hour, amount);
            }
            for (Integer hour : currentEODReport.order_count_by_hour.keySet()) {
                Integer orderCount = currentEODReport.order_count_by_hour.get(hour);
                orderCounts.put(hour, orderCount);
            }
            reportSB.append("hour,amount,order_count\n");
            for (Integer hour : asSortedList(amounts.keySet())) {
                reportSB.append(String.format("%02d",hour) + "," + amounts.get(hour) + "," + orderCounts.get(hour) + "\n");
            }
            reportSB.append("\n");

            reportSB.append("Top 5 products\n");
            final int maxCount = 5;
            int currentCount = 0;
            for (String productUid : sortByValue(currentEODReport.product_ordered_count).keySet()) {
                currentCount += 1;
                reportSB.append(productUid + ": " + currentEODReport.product_ordered_count.get(productUid) + "\n");
                if (currentCount >= maxCount) {
                    break;
                }
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, reportSB.toString());

            startActivity(Intent.createChooser(intent, "Send Email"));
        }
    }
}
