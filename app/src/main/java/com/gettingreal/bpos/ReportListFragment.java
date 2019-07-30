package com.gettingreal.bpos;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.report.ServerEODReport;
import com.gettingreal.bpos.api.report.ServerMonthlyReport;
import com.gettingreal.bpos.api.report.ServerReport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by ivanfoong on 2/7/14.
 */
public class ReportListFragment extends Fragment {
    private ToggleButton mEODReportButton, mMonthlyReportButton, mLastButton;
    private ListView mListView;
    private EODReportListAdapter mEODReportListAdapter;
    private MonthlyReportListAdapter mMonthlyReportListAdapter;
    private ServerAPIClient mServerAPIClient;
    private ServerReport mServerReportCache = null;
    private View.OnClickListener mOnReportTypeTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View aView) {
            mLastButton = (ToggleButton) aView;

            if (aView == mMonthlyReportButton) {
                mMonthlyReportButton.setChecked(true);
                mEODReportButton.setChecked(false);

                if (mServerReportCache == null) {
                    mServerAPIClient.getReport(new Callback<ServerReport>() {
                        @Override
                        public void success(ServerReport aServerReport, Response aResponse) {
                            mServerReportCache = aServerReport;

                            ArrayList<String> monthlyReportIds = new ArrayList<String>();
                            ArrayList<ServerMonthlyReport> monthlyReports = new ArrayList<ServerMonthlyReport>();
                            for (String monthlyReportId : asReverseSortedList(aServerReport.monthly.keySet())) {
                                monthlyReportIds.add(monthlyReportId);
                                ServerMonthlyReport monthlyReport = aServerReport.monthly.get(monthlyReportId);
                                monthlyReports.add(monthlyReport);
                            }

                            mMonthlyReportListAdapter = new MonthlyReportListAdapter(aView.getContext(), monthlyReportIds, monthlyReports);
                            mListView.setAdapter(mMonthlyReportListAdapter);
                            mListView.setOnItemClickListener(mMonthlyReportListViewItemClickListener);
                            if (mListView.getChildCount() > 0) {
                                mListView.performItemClick(mListView.getChildAt(0), 0, mListView.getFirstVisiblePosition());
                            }
                        }

                        @Override
                        public void failure(RetrofitError aRetrofitError) {
                            aRetrofitError.printStackTrace();
                            ArrayList<String> monthlyReportIds = new ArrayList<String>();
                            ArrayList<ServerMonthlyReport> monthlyReports = new ArrayList<ServerMonthlyReport>();
                            mMonthlyReportListAdapter = new MonthlyReportListAdapter(aView.getContext(), monthlyReportIds, monthlyReports);
                            mListView.setAdapter(mMonthlyReportListAdapter);
                            mListView.setOnItemClickListener(mMonthlyReportListViewItemClickListener);
                        }
                    });
                }
                else {
                    ArrayList<String> monthlyReportIds = new ArrayList<String>();
                    ArrayList<ServerMonthlyReport> monthlyReports = new ArrayList<ServerMonthlyReport>();
                    for (String monthlyReportId : asReverseSortedList(mServerReportCache.monthly.keySet())) {
                        monthlyReportIds.add(monthlyReportId);
                        ServerMonthlyReport monthlyReport = mServerReportCache.monthly.get(monthlyReportId);
                        monthlyReports.add(monthlyReport);
                    }

                    mMonthlyReportListAdapter = new MonthlyReportListAdapter(aView.getContext(), monthlyReportIds, monthlyReports);
                    mListView.setAdapter(mMonthlyReportListAdapter);
                    mListView.setOnItemClickListener(mMonthlyReportListViewItemClickListener);
                    if (mListView.getChildCount() > 0) {
                        mListView.performItemClick(mListView.getChildAt(0), 0, mListView.getFirstVisiblePosition());
                    }
                }
            } else {
                mMonthlyReportButton.setChecked(false);
                mEODReportButton.setChecked(true);

                if (mServerReportCache == null) {
                    mServerAPIClient.getReport(new Callback<ServerReport>() {
                        @Override
                        public void success(ServerReport aServerReport, Response aResponse) {
                            mServerReportCache = aServerReport;

                            ArrayList<String> eodReportIds = new ArrayList<String>();
                            ArrayList<ServerEODReport> eodReports = new ArrayList<ServerEODReport>();
                            for (String eodReportId : asReverseSortedList(aServerReport.eod.keySet())) {
                                eodReportIds.add(eodReportId);
                                ServerEODReport eodReport = aServerReport.eod.get(eodReportId);
                                eodReports.add(eodReport);
                            }

                            mEODReportListAdapter = new EODReportListAdapter(aView.getContext(), eodReportIds, eodReports);
                            mListView.setAdapter(mEODReportListAdapter);
                            mListView.setOnItemClickListener(mEODReportListViewItemClickListener);
                            if (mListView.getChildCount() > 0) {
                                mListView.performItemClick(mListView.getChildAt(0), 0, mListView.getFirstVisiblePosition());
                            }
                        }

                        @Override
                        public void failure(RetrofitError aRetrofitError) {
                            aRetrofitError.printStackTrace();
                            ArrayList<String> eodReportIds = new ArrayList<String>();
                            ArrayList<ServerEODReport> eodReports = new ArrayList<ServerEODReport>();
                            mEODReportListAdapter = new EODReportListAdapter(aView.getContext(), eodReportIds, eodReports);
                            mListView.setAdapter(mEODReportListAdapter);
                            mListView.setOnItemClickListener(mEODReportListViewItemClickListener);
                        }
                    });
                }
                else {
                    ArrayList<String> eodReportIds = new ArrayList<String>();
                    ArrayList<ServerEODReport> eodReports = new ArrayList<ServerEODReport>();
                    for (String eodReportId : asReverseSortedList(mServerReportCache.eod.keySet())) {
                        eodReportIds.add(eodReportId);
                        ServerEODReport eodReport = mServerReportCache.eod.get(eodReportId);
                        eodReports.add(eodReport);
                    }

                    mEODReportListAdapter = new EODReportListAdapter(aView.getContext(), eodReportIds, eodReports);
                    mListView.setAdapter(mEODReportListAdapter);
                    mListView.setOnItemClickListener(mEODReportListViewItemClickListener);
                    if (mListView.getChildCount() > 0) {
                        mListView.performItemClick(mListView.getChildAt(0), 0, mListView.getFirstVisiblePosition());
                    }
                }
            }
        }
    };

    private AdapterView.OnItemClickListener mEODReportListViewItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
            String eodReportId = mEODReportListAdapter.getReportId(i);
            Intent intent = new Intent("report-selected");
            // add data
            intent.putExtra("report_type", "eod");
            intent.putExtra("report_id", eodReportId);

            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
    };

    private AdapterView.OnItemClickListener mMonthlyReportListViewItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
            String monthlyReportId = mMonthlyReportListAdapter.getReportId(i);
            Intent intent = new Intent("report-selected");
            // add data
            intent.putExtra("report_type", "monthly");
            intent.putExtra("report_id", monthlyReportId);

            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
    };

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_list, container, false);

        mEODReportButton = (ToggleButton) view.findViewById(R.id.btn_eod_report);
        mMonthlyReportButton = (ToggleButton) view.findViewById(R.id.btn_monthly_report);
        mListView = (ListView) view.findViewById(R.id.list_view_reports);

        mEODReportButton.setOnClickListener(mOnReportTypeTabClickListener);
        mMonthlyReportButton.setOnClickListener(mOnReportTypeTabClickListener);

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");

        String serverUrl = "http://" + address + "/";

        mServerAPIClient = new ServerAPIClient(serverUrl);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLastButton == null) {
            mEODReportButton.performClick();
        } else {
            mLastButton.performClick();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mServerReportCache = null;
    }

    private class EODReportListAdapter extends BaseAdapter {

        Context mContext;
        List<String> mEODReportIds;
        List<ServerEODReport> mEODReports;

        public EODReportListAdapter(Context context, List<String> aEODReportIds, List<ServerEODReport> aEODReports) {
            mContext = context;
            mEODReportIds = aEODReportIds;
            mEODReports = aEODReports;
        }

        @Override
        public Object getItem(final int i) {
            return mEODReports.get(i);
        }

        @Override
        public int getCount() {
            return mEODReports.size();
        }

        @Override
        public long getItemId(final int i) {
            return i;
        }

        public String getReportId(final int i) {
            if (mEODReportIds.size() > i) {
                return mEODReportIds.get(i);
            }
            return "";
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.text_list_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(R.id.text);
            try {
                SimpleDateFormat inputSDF = new SimpleDateFormat("yyyy-MM-dd");
                Date date = inputSDF.parse(mEODReportIds.get(position));
                SimpleDateFormat outputSDF = new SimpleDateFormat("dd MMM yyyy");
                textView.setText(outputSDF.format(date));
            }
            catch (ParseException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }

    private class MonthlyReportListAdapter extends BaseAdapter {

        Context mContext;
        List<String> mMonthlyReportIds;
        List<ServerMonthlyReport> mMonthlyReports;

        public MonthlyReportListAdapter(Context context, List<String> aMonthlyReportIds, List<ServerMonthlyReport> aMonthlyReports) {
            mContext = context;
            mMonthlyReportIds = aMonthlyReportIds;
            mMonthlyReports = aMonthlyReports;
        }

        @Override
        public Object getItem(final int i) {
            return mMonthlyReports.get(i);
        }

        @Override
        public int getCount() {
            return mMonthlyReports.size();
        }

        @Override
        public long getItemId(final int i) {
            return i;
        }

        public String getReportId(final int i) { return mMonthlyReportIds.get(i); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.text_list_item, parent, false);
            }

            TextView textView = (TextView) convertView.findViewById(R.id.text);
            try {
                SimpleDateFormat inputSDF = new SimpleDateFormat("yyyy-MM");
                Date date = inputSDF.parse(mMonthlyReportIds.get(position));
                SimpleDateFormat outputSDF = new SimpleDateFormat("MMM yyyy");
                textView.setText(outputSDF.format(date));
            }
            catch (ParseException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }

    private <T extends Comparable<? super T>> List<T> asReverseSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        java.util.Collections.reverse(list);
        return list;
    }
}