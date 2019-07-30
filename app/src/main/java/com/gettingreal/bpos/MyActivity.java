package com.gettingreal.bpos;

import android.os.Bundle;
import android.app.Activity;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.epson.epos2.linedisplay.LineDisplay;
import com.epson.epos2.linedisplay.ReceiveListener;
import com.epson.epos2.Epos2Exception;

public class MyActivity extends Activity implements  ReceiveListener {

    private Context mContext = null;
    private LineDisplay mLineDisplay = null;

    public MyActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mContext = this;
        runLineDisplaySequence();
    }


    private boolean runLineDisplaySequence() {
        if (!initializeObject()) {
            return false;
        }

        if (!createDisplayData()) {
            finalizeObject();
            return false;
        }

        if (!connectDisplay()) {
            finalizeObject();
            return false;
        }

        try {
            mLineDisplay.sendData();
        }
        catch (Exception e) {
            Log.e( "sendData", e + "");
            disconnectDisplay();
            return false;
        }

        return true;
    }

    private boolean initializeObject() {
        try {
            mLineDisplay = new LineDisplay(LineDisplay.DM_D110, mContext);
            Toast.makeText(MyActivity.this,"initialize", Toast.LENGTH_LONG).toString();
        }
        catch (Exception e) {
            Log.e( "sendData", e + "");

            return false;
        }

        mLineDisplay.setReceiveEventListener(this);

        return true;
    }

    private void finalizeObject() {
        if (mLineDisplay == null) {
            return;
        }

        mLineDisplay.clearCommandBuffer();

        mLineDisplay.setReceiveEventListener(null);

        mLineDisplay = null;
    }

    private boolean connectDisplay() {
        try {
            Toast.makeText(MyActivity.this, "Connnected", Toast.LENGTH_LONG).toString();
            mLineDisplay.connect("TCP:192.168.1.151[local_display]", LineDisplay.PARAM_DEFAULT);
           // mLineDisplay.addInitialize();
        }
        catch (Epos2Exception e) {
           Log.e(" connect -", e + "");

            return false;
        }

        return true;
    }

    private void disconnectDisplay() {
        if (mLineDisplay == null) {
            return;
        }

        try {
            mLineDisplay.disconnect();
        }
        catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    Log.e("disconnect ", e + "");
                }
            });
        }

        finalizeObject();
    }

    private boolean createDisplayData() {
        String method = "";


        if (mLineDisplay == null) {
            return false;
        }

        try {
            method = "addInitialize";
            mLineDisplay.addInitialize();

            method = "addSetCursorPosition";
            mLineDisplay.addSetCursorPosition(1, 1);



            method = "addText";
            mLineDisplay.addText("Hello you!");
            Toast.makeText(MyActivity.this," hey .. done.", Toast.LENGTH_LONG).toString();
        }
        catch (Exception e) {
            Log.e("create -", method + "");
            return false;
        }

        return true;
    }



    @Override
    public void onDispReceive(final LineDisplay displayObj, final int code) {
        runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        finalizeObject();
                        disconnectDisplay();
                    }
                }).start();
            }
        });
    }

}
