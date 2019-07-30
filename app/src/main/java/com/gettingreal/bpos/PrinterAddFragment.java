package com.gettingreal.bpos;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.discovery.DeviceInfo;
import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.FilterOption;
import com.gettingreal.bpos.helper.PrinterSessionManager;
import com.gettingreal.bpos.helper.ShowMsg;
import com.gettingreal.bpos.model.POSPrinter;
import com.jess.ui.TwoWayAdapterView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ivanfoong on 27/3/14.
 */
public class PrinterAddFragment extends Fragment {

    private Button mBackButton, mAddButton;
    private EditText mNameEditText;

    private Context mContext = null;
    private ArrayList<HashMap<String, String>> mPrinterList = null;
    private FilterOption mFilterOption = null;
    private SimpleAdapter mPrinterListAdapter = null;
    private SimpleAdapter mSpnPrinterListAdapter = null;
    ListView printer_list;
    private Button mPrinterSearchButton;
    private Boolean mPrintSerarchBoolean=false;
    private ArrayList<String> spnList=new ArrayList<String>();
    String select_Position;
    PrinterSessionManager printerSessionManager;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        printerSessionManager=new PrinterSessionManager(getActivity());
        View view = inflater.inflate(R.layout.fragment_printer_add, container, false);

        mBackButton = (Button) view.findViewById(R.id.button_back);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                Fragment fragment = new PrinterManagementFragment();
                FragmentManager fm = getFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.layout_content, fragment);
                transaction.commit();
            }
        });

        mAddButton = (Button) view.findViewById(R.id.button_add);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                getActivity().getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

                if (mNameEditText.getText().toString().equals("")) {
                    Toast.makeText(aView.getContext(), "Printer name must not be empty!", Toast.LENGTH_LONG).show();
                }
                else {
                    final String name = mNameEditText.getText().toString();
                    final String uid = name.toLowerCase().replace(' ', '_');
                    POSPrinter newPrinter = POSPrinter.createPrinter(aView.getContext(), uid, name, 0, false, new ArrayList<String>());
                  //  printerSessionManager.setURL_Name(name);

                    Fragment fragment = new PrinterManagementFragment();
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                    transaction.replace(R.id.layout_content, fragment);
                    transaction.commit();

                    if (newPrinter != null) {
                        Toast.makeText(aView.getContext(), "Printer " + newPrinter.getName() + " added!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(aView.getContext(), "Failed to create Printer " + newPrinter.getName(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        mNameEditText = (EditText) view.findViewById(R.id.edit_text_name);

        /**
         * SearchPrinter List
         * Min Thein Win.
         * Code2Lab.co on 16/August/16.
         */

        mPrinterSearchButton=(Button)view.findViewById(R.id.btnDiscovery);
        printer_list = (ListView)view.findViewById(R.id.lstReceiveData);

        mPrinterList = new ArrayList<HashMap<String, String>>();
        mPrinterSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPrintSerarchBoolean==true) {
                    while (mPrintSerarchBoolean == true) {
                        try {
                            Discovery.stop();
                        } catch (Epos2Exception e) {
                            if (e.getErrorStatus() != Epos2Exception.ERR_PROCESSING) {
                                return;
                            }
                        }
                    }
                    mPrinterList.clear();
                    mPrinterListAdapter.notifyDataSetChanged();

                    mFilterOption = new FilterOption();
                    mFilterOption.setDeviceType(Discovery.TYPE_PRINTER);
                    mFilterOption.setEpsonFilter(Discovery.FILTER_NAME);

                    try {
                        Discovery.start(getActivity(), mFilterOption, mDiscoveryListener);
                        mPrintSerarchBoolean = true;
                    }
                    catch (Exception e) {
                        ShowMsg.showException(e, "stop", mContext);
                    }

                }else {
                    mFilterOption = new FilterOption();
                    mFilterOption.setDeviceType(Discovery.TYPE_PRINTER);
                    mFilterOption.setEpsonFilter(Discovery.FILTER_NAME);
                    try {
                        Discovery.start(getActivity(), mFilterOption, mDiscoveryListener);
                        mPrintSerarchBoolean = true;
                    } catch (Exception e) {
                        ShowMsg.showException(e, "start", mContext);
                    }
                }
            }
        });

        mPrinterListAdapter = new SimpleAdapter(getActivity(), mPrinterList, R.layout.printer_search_list_at,
                new String[] { "PrinterName", "Target" },
                new int[] { R.id.PrinterName, R.id.Target });
        printer_list.setAdapter(mPrinterListAdapter);
        printer_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                   @Override
                   public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                       HashMap<String, String> item = mPrinterList.get(i);
                        mNameEditText.setText(item.get("Target")+","+item.get("PrinterName"));
                       Log.e("PrinterItem", item.get("Target"));
                   }
               });

        return view;

    }

    private DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
        @Override
        public void onDiscovery(final DeviceInfo deviceInfo) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    HashMap<String, String> item = new HashMap<String, String>();
                    item.put("PrinterName", deviceInfo.getDeviceName());
                    item.put("Target", deviceInfo.getTarget());
                    mPrinterList.add(item);
                    mPrinterListAdapter.notifyDataSetChanged();
                }
            });

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        while (true) {
            try {
                Discovery.stop();
                break;
            }
            catch (Epos2Exception e) {
                if (e.getErrorStatus() != Epos2Exception.ERR_PROCESSING) {
                    break;
                }
            }
        }

        mFilterOption = null;
    }

    @Override
    public void onResume() {
        super.onResume();

    }
}