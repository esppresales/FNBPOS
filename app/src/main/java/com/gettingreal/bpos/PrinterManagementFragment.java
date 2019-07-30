package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.epson.eposprint.Builder;
import com.gettingreal.bpos.api.ServerAPIClient;
import com.gettingreal.bpos.api.ServerOrder;
import com.gettingreal.bpos.api.ServerOrderItem;
import com.gettingreal.bpos.api.ServerPostOrderResponse;
import com.gettingreal.bpos.helper.PaymentHelper;
import com.gettingreal.bpos.helper.PrintPOS2Help;
import com.gettingreal.bpos.helper.PrinterSessionManager;
import com.gettingreal.bpos.helper.ShowMsg;
import com.gettingreal.bpos.model.POSCategory;
import com.gettingreal.bpos.model.POSOrder;
import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSPrinter;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSReceipt;
import com.gettingreal.bpos.model.POSTable;

/*import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;*/

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by ivanfoong on 24/3/14.
 */
public class PrinterManagementFragment extends Fragment implements ReceiveListener{

    static ListView mPrinterListView;

    Button mAddPrinterButton;

    private static PrinterListAdapter mPrinterListAdapter;

    Spinner printerSpinner;

    private ArrayList<POSPrinter> mPrinters;

    ArrayList<String> printerlist=new ArrayList<String>();

    PrinterSessionManager printerSessionManager;

    String selectmasterPrinter="";

    ArrayAdapter<String> printerListadapter;

    CheckBox checkdisplay,checkdrawer;

    RadioGroup cashierRadioGroup,orderRadioGroup;

    RadioButton radioCashier58MM,radioCashier88MM,radioOrder58MM,radioOrder88MM;

    ArrayList<String> printerlistip=new ArrayList<String>();

    HashMap<String,String> hasmapPrinter = new HashMap<String, String>();

    private boolean isTakeAway = false;

    public String receiptType;

    private Spinner mTableNumberSpinner;

    private CheckoutFragment.TableNumberAdapter mTableNumberAdapter;

    private POSOrder mOrder;

    String thelastPrintIP;

    String masterPrinter;

    ArrayList<POSOrderItem> orderItems;

    private Printer  mPrinter = null;

    Hashtable<String, ArrayList<POSOrderItem>> orderItemsForPrinterUid;

    private ArrayList<POSTable> mPOSTables;

    private boolean mIsServerOnline  = false;

    private Context mContext;

    PrintPOS2Help printPOS2Help;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_printer_management, container, false);
        printerSessionManager=new PrinterSessionManager(getActivity().getApplicationContext());
        mPrinterListView = (ListView) view.findViewById(R.id.list_view_printers);
        printerSpinner = (Spinner) view.findViewById(R.id.spinner_printerlist);
        checkdisplay=(CheckBox)view.findViewById(R.id.display_check);
        checkdrawer=(CheckBox)view.findViewById(R.id.drawer_check);
        cashierRadioGroup=(RadioGroup)view.findViewById(R.id.CashierRadioGroup);
        orderRadioGroup=(RadioGroup)view.findViewById(R.id.OrderRadioGroup);
        radioCashier58MM=(RadioButton)view.findViewById(R.id.Receipt58MM);
        radioCashier88MM=(RadioButton)view.findViewById(R.id.Receipt88MM);
        radioOrder58MM=(RadioButton)view.findViewById(R.id.order58MM);
        radioOrder88MM=(RadioButton)view.findViewById(R.id.order88MM);

        mAddPrinterButton = (Button) view.findViewById(R.id.button_add);
        mAddPrinterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                Fragment fragment = new PrinterAddFragment();
                FragmentManager fm = getFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.layout_content, fragment);
                transaction.commit();
            }
        });

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mPrinters = POSPrinter.getAllPrinters(getActivity());
        // remove master printer from selection
        POSPrinter masterPrinter = null;
        for (POSPrinter printer : mPrinters) {
            if (printer.getUid().contentEquals("master")) {
                masterPrinter = printer;
            }
        }
        if (masterPrinter != null) {
            mPrinters.remove(masterPrinter);
        }

        for (int i=0;i<mPrinters.size();i++){
            POSPrinter printer = mPrinters.get(i);
            printerlist.add(printer.getName());
            printerlistip.add(printer.getUid());
            hasmapPrinter.put(printer.getName(), printer.getUid());
        }

        printerListadapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, printerlist);
        printerListadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        printerSpinner.setAdapter(printerListadapter);
        printerListadapter.notifyDataSetChanged();

        if (printerSessionManager.getPrintURL_KEY()!=""){
            ArrayAdapter printAdap = (ArrayAdapter) printerSpinner.getAdapter();
            int spinnerPosition = printAdap.getPosition(printerSessionManager.getPrintURL_KEY());
            printerSpinner.setSelection(spinnerPosition);
            Log.e("PrintPosition", String.valueOf(spinnerPosition));
            Log.e("PrinterNmae",printerSessionManager.getPrintURL_KEY());

            }

        printerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
                //printerSpinner.setSelection(printerListadapter.getPosition(selectmasterPrinter));
                selectmasterPrinter = aAdapterView.getItemAtPosition(i).toString();
                printerSessionManager.setURL_Name(hasmapPrinter.get(selectmasterPrinter));
                printerSessionManager.setURL_NameKEY(selectmasterPrinter);
                Log.e("SelectMasterPrintName", selectmasterPrinter);
                Log.e("Select MasterPrinter", hasmapPrinter.get(selectmasterPrinter));
                Log.e("Get MasterPrinter", printerSessionManager.getPrintURL());

            }

            @Override
            public void onNothingSelected(final AdapterView<?> aAdapterView) {
            return;
            }
        });

        if (printerSessionManager.getDisplay()!=null) {
            if (printerSessionManager.getDisplay().equalsIgnoreCase("true")) {
                checkdisplay.setChecked(true);
            }else {
                checkdisplay.setChecked(false);
            }
            }

        if (printerSessionManager.getCashierDrawer()!=null) {
            if (printerSessionManager.getCashierDrawer().equalsIgnoreCase("true")) {
                checkdrawer.setChecked(true);
            }else {
                checkdrawer.setChecked(false);
            }
        }

        checkdisplay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ischeck) {
                if (ischeck) {
                    printerSessionManager.setDISPLAY("true");
                } else {
                    printerSessionManager.setDISPLAY("false");
                }
            }
        });

        checkdrawer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean ischeck) {
                if (ischeck) {
                    printerSessionManager.setDRAWER("true");
                } else {
                    printerSessionManager.setDRAWER("false");
                }
            }
        });

        if (printerSessionManager.getCashierPrintSize()!=null&&printerSessionManager.getCashierPrintSize()!=""){
            if (printerSessionManager.getCashierPrintSize().equalsIgnoreCase("88MM")){
                radioCashier88MM.setChecked(true);
            }else if (printerSessionManager.getCashierPrintSize().equalsIgnoreCase("58MM")){
                radioCashier58MM.setChecked(true);
            }
        }else {
            printerSessionManager.setCashierprintSize("88MM");
            radioCashier88MM.setChecked(true);
        }

        if (printerSessionManager.getOrderPrintSize()!=null&&printerSessionManager.getOrderPrintSize()!=""){
            if (printerSessionManager.getOrderPrintSize().equalsIgnoreCase("88MM")){
                radioOrder88MM.setChecked(true);
            }else if (printerSessionManager.getOrderPrintSize().equalsIgnoreCase("58MM")){
                radioOrder58MM.setChecked(true);
            }
        }else {
            printerSessionManager.setOrderPrintSize("88MM");
            radioOrder88MM.setChecked(true);
        }

        cashierRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int check) {
                if (check==R.id.Receipt58MM){
                    printerSessionManager.setCashierprintSize("58MM");
                }else if (check==R.id.Receipt88MM){
                    printerSessionManager.setCashierprintSize("88MM");
                }

            }
        });

        orderRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int check) {
                if (check==R.id.order58MM){
                    printerSessionManager.setOrderPrintSize("58MM");
                }else if (check==R.id.order88MM){
                    printerSessionManager.setOrderPrintSize("88MM");
                }
            }
            });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mPrinterListAdapter = new PrinterListAdapter(getActivity());
        mPrinterListView.setAdapter(mPrinterListAdapter);

       /* printerListadapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, printerlist);
        printerListadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        printerSpinner.setAdapter(printerListadapter);
        printerListadapter.notifyDataSetChanged(); */
    }

    private void testPrint(String printerUid) {
        String printerIP;
        String printerName;
        if (PrintPOS2Help.containsDigitPinterStr(printerUid)){
            StringTokenizer namEIP = new StringTokenizer(printerUid,",");
            ArrayList<String> strNew = new ArrayList<String>();
            while (namEIP.hasMoreTokens()){
                strNew.add(namEIP.nextToken());
            }
            printerIP = strNew.get(0);
            printerName = strNew.get(1);

        } else {
            //set Default Printer Model and IP;
            printerIP = "TCP:192.168.192.168";
            printerName = "TM-T88V";
        }

        if (!initializeObject(printerName)) {
            return;
        }

        // Printing ....
        if (printerSessionManager.getOrderPrintSize() != null){
            if (printerSessionManager.getOrderPrintSize().equalsIgnoreCase("88MM")) {
                if(!testPrint80MMThaiItem()) {
                    finalizeObject();
                    return;
                }
            } else if(printerSessionManager.getOrderPrintSize().equalsIgnoreCase("58MM")) {
                if (!testPrint58MMThaiItem()) {
                    finalizeObject();
                    return;
                }
            }
        }
        if (!printData(printerIP)) {
            finalizeObject();
            return;
        }
    }

    public boolean printData(String receiptPrintIP) {

        if (mPrinter == null) {
            return false;
        }

        if (!connectPrinter(receiptPrintIP)) {
            return false;
        }

        PrinterStatusInfo status = mPrinter.getStatus();
        if (!isPrintable(status)) {
            ShowMsg.showMsg(printPOS2Help.makeErrorMessage(status), getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "sendData", getActivity());
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                // Do nothing
            }
            return false;
        }

        return true;
    }

    public boolean isPrintable(PrinterStatusInfo status) {
        if (status == null) {
            return false;
        }

        if (status.getConnection() == Printer.FALSE) {
            return false;
        }
        else if (status.getOnline() == Printer.FALSE) {
            return false;
        }
        else {
            //print available
        }

        return true;
    }

    public boolean initializeObject(String printerName) {

        try {

            if (printerName.equalsIgnoreCase("TM-T88V")) {
                mPrinter = new Printer(Printer.TM_T88, Printer.LANG_EN, getActivity());
            }else if (printerName.equalsIgnoreCase("TM-T82")){
                mPrinter = new Printer(Printer.TM_T82, Printer.LANG_EN, getActivity());
            }else if (printerName.equalsIgnoreCase("TM-T81")){
                mPrinter = new Printer(Printer.TM_T81, Printer.LANG_EN, getActivity());
            }else {
                mPrinter = new Printer(Printer.TM_T88, Printer.LANG_EN, getActivity());
            }
        }
        catch (Exception e) {
            ShowMsg.showException(e, "Printer", getActivity());
            return false;
        }
        mPrinter.setReceiveEventListener(this);
        return true;
    }
    public void finalizeObject() {
        if (mPrinter == null) {
            return;
        }

        mPrinter.clearCommandBuffer();

        mPrinter.setReceiveEventListener(null);

        mPrinter = null;
    }

    public boolean connectPrinter(String receiptPrintIP) {
        boolean isBeginTransaction = false;
        if (mPrinter == null) {
            return false;
        }

        try {
            mPrinter.connect(receiptPrintIP, Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            ShowMsg.showException(e, "connect", getActivity());
            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {
            ShowMsg.showException(e, "beginTransaction", getActivity());
        }

        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            }
            catch (Epos2Exception e) {
                // Do nothing
                return false;
            }
        }

        return true;
    }

    @Override
    public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                disconnectPrinter();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        disconnectPrinter();
                    }
                }).start();
            }
        });
    }

    private void disconnectPrinter() {
        if (mPrinter == null) {
            return;
        }

        try {
            mPrinter.endTransaction();
        } catch (final Exception e) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "endTransaction", getActivity());
                }
            });
        }

        try {
            mPrinter.disconnect();
        } catch (final Exception e) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    //ShowMsg.showException(e, "disconnect", getActivity());
                }
            });
        }

        finalizeObject();
    }

    // 58MM Printing Test Kitchen
    private boolean testPrint58MMThaiItem() {
        if (mPrinter == null) {
            return false;
        }
        try {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            String dateTime = df.format(Calendar.getInstance().getTime());

            mPrinter.addTextFont(Builder.FONT_B);
            mPrinter.addTextLang(Printer.LANG_TH);
            mPrinter.addTextSize(1, 1);

            String dateTimeSpace = "";
            for (int i = 0; i < (42 - dateTime.length()) / 2; i++) {
                dateTimeSpace += " ";
            }

            mPrinter.addText(dateTimeSpace + dateTime);
            mPrinter.addFeedLine(1);

            String testString = "Printing Test";
            String testStringSpace = "";
            for (int i = 0; i < (42 - testString.length()) / 2; i++) {
                testStringSpace += " ";
            }

            mPrinter.addText(testStringSpace + testString);
            mPrinter.addFeedLine(1);
            mPrinter.addText("------------------------------------------\n");
            mPrinter.addCut(Printer.CUT_FEED);
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // 88MM Printing Test Kitchen
    private boolean testPrint80MMThaiItem() {
        if (mPrinter == null) {
            return false;
        }
        try {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            StringBuilder dateTime = new StringBuilder(df.format(Calendar.getInstance().getTime()));
            StringBuilder testString = new StringBuilder("Printing Test");

            mPrinter.addTextFont(Builder.FONT_A);
            mPrinter.addTextLang(Printer.LANG_TH);
            mPrinter.addTextAlign(Printer.ALIGN_CENTER);
            mPrinter.addTextSize(1, 1);

            mPrinter.addText(dateTime.toString());
            mPrinter.addFeedLine(1);

            mPrinter.addText(testString.toString());
            mPrinter.addFeedLine(1);
            mPrinter.addText("------------------------------------------\n");
            mPrinter.addCut(Printer.CUT_FEED);
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public class PrinterListAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<POSPrinter> mPrinters;

        public PrinterListAdapter(final Context aContext) {
            mContext = aContext;
            mPrinters = POSPrinter.getAllPrinters(mContext);
        }

        @Override
        public int getCount() {
            return mPrinters.size();
        }

        @Override
        public Object getItem(int i) {
            return mPrinters.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = View.inflate(mContext, R.layout.printer_item, null);

            final EditText nameEditText = (EditText) view.findViewById(R.id.edit_text_name);
            final TextView deviceIdTextView = (TextView) view.findViewById(R.id.txt_device_id);
            final Button deleteButton = (Button) view.findViewById(R.id.button_delete);
            final Button testPrintButton = (Button) view.findViewById(R.id.button_test_print);

            final POSPrinter printer = mPrinters.get(i);

            nameEditText.setText(printer.getName());
            nameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View aView, boolean b) {
                    if (!printer.getName().contentEquals(nameEditText.getText().toString())) {
                        if (nameEditText.getText().toString().equals("")) {
                            Toast.makeText(mContext, "Category name cannot be empty", Toast.LENGTH_LONG).show();
                            nameEditText.setText(printer.getName());
                        }
                        else {
                            printer.setName(nameEditText.getText().toString());
                            printer.save(mContext);
                        }
                    }
                }
            });

            deviceIdTextView.setText(printer.getUid());

            // disable deletion of master printer
            if (printer.getUid().equals("master")) {
                deleteButton.setVisibility(View.INVISIBLE);
            } else {
                deleteButton.setVisibility(View.VISIBLE);
            }

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View aView) {
                    Toast.makeText(aView.getContext(), "Printer " + printer.getName() + " is deleted!", Toast.LENGTH_LONG).show();
                    POSPrinter.deletePrinter(aView.getContext(), printer.getUid());
                    mPrinterListAdapter = new PrinterListAdapter(getActivity());
                    mPrinterListView.setAdapter(mPrinterListAdapter);
                }
            });

            testPrintButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View aView) {
//                    SharedPreferences settings = aView.getContext().getSharedPreferences(aView.getContext().getPackageName(), 0);
//                    String address = settings.getString("address", "192.168.192.168");
//                    new TestPrintTask(aView.getContext(), address, printer.getUid()).execute();
//                    Log.e(address,printer.getUid());
                    testPrint(printer.getName());
                }
            });
            return view;
        }
    }

    private class TestPrintTask extends AsyncTask<Void, Void, Void> {

        private Context mContext;
        private String mAddress, mDeviceId;
        private ProgressDialog mProgressDialog;

        private TestPrintTask(final Context aContext, final String aAddress, final String aDeviceId) {
            mContext = aContext;
            mAddress = aAddress;
            mDeviceId = aDeviceId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = ProgressDialog.show(mContext, "Sending",
                "Please wait...", true);
        }

        @Override
        protected Void doInBackground(Void... urls) {
            try {
                String url = "http://" + mAddress +
                    "/cgi-bin/epos/service.cgi?devid=" + mDeviceId + "&timeout=10000";

                // Send print document - this portion is epos XML, no longer in used - 12 March 2019 Teo Chee Kern
                //HttpClient httpclient = new DefaultHttpClient();
               // HttpPost httppost = new HttpPost(url);

                String req =
                    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                        "<s:Body>" +
                        "<epos-print xmlns='http://www.epson-pos.com/schemas/2011/03/epos-print'>" +
                        "<text lang='en' smooth='true'>Intelligent Printer&#10;</text>" +
                        "<barcode type='ean13' width='2' height='48'>201234567890</barcode>" +
                        "<feed unit='24' />" +
                        "<image width='8' height='48'>8PDw8A8PDw/w8PDwDw8PD/Dw8PAPDw8P8PDw8A8PDw/w8PDwDw8PD/Dw8PAPDw8P</image>" +
                        "<cut />" +
                        "</epos-print>" +
                        "</s:Body>" +
                        "</s:Envelope>";

                //StringEntity entity = new StringEntity(req, HTTP.UTF_8);
                //entity.setContentType("text/xml charset=utf-8");
                //httppost.setEntity(entity);

                //HttpResponse response = httpclient.execute(httppost);
                //End of Portion - 12 march 2019 Teo Chee Kern

//            // Receive response document
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//
//            // Parse response document(DOM)
//            Document doc = builder.parse(response.getEntity().getContent());
//            Element el = (Element) doc.getElementsByTagName("response").item(0);

//                AlertDialog.Builder dlg = new AlertDialog.Builder(getActivity());
//                dlg.setTitle("Checkout");
//                dlg.setMessage(el.getAttribute("success"));
//                dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dlg , int whichbutton) {
//                        //setResult(Activity.RESULT_OK);
//                    }
//                });
//                dlg.create();
//                dlg.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onPause() {
        super.onPause();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        printerListadapter.clear();
        printerListadapter.notifyDataSetChanged();
    }

}
