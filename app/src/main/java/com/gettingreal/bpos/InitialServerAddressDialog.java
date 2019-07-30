package com.gettingreal.bpos;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 5:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class InitialServerAddressDialog extends DialogFragment implements TextView.OnEditorActionListener {

    private EditText mAddressEditText;

    public InitialServerAddressDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_initial_server_address_dialog, container);

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        String address = settings.getString("address", "192.168.192.168");

        mAddressEditText = (EditText) view.findViewById(R.id.txt_address);
        mAddressEditText.setOnEditorActionListener(this);
        mAddressEditText.setText(address);

        getDialog().setTitle("Server Address Setting");

        mAddressEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        Button saveButton = (Button) view.findViewById(R.id.btn_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                savePrinter(mAddressEditText.getText().toString());
            }
        });

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            savePrinter(mAddressEditText.getText().toString());
            return true;
        }
        return false;
    }

    private void savePrinter(String address) {
        // Return input text to activity
        InitialServerAddressDialogListener activity = (InitialServerAddressDialogListener) getActivity();
        activity.onFinishInitialServerAddressDialog(address);
        this.dismiss();
    }

    public interface InitialServerAddressDialogListener {
        void onFinishInitialServerAddressDialog(String address);
    }
}
