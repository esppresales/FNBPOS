package com.gettingreal.bpos;

import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 11:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoginDialog extends DialogFragment implements TextView.OnEditorActionListener {

    private EditText mUsernameEditText, mPasswordEditText;

    public LoginDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_dialog, container);
        mUsernameEditText = (EditText) view.findViewById(R.id.txt_username);
        mUsernameEditText.setOnEditorActionListener(this);

        mUsernameEditText.setText("user");

        mPasswordEditText = (EditText) view.findViewById(R.id.txt_password);
        mPasswordEditText.setOnEditorActionListener(this);

        mPasswordEditText.setText("pass");

        getDialog().setTitle("Hello");

        mUsernameEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        Button loginButton = (Button) view.findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mUsernameEditText.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(mPasswordEditText.getWindowToken(), 0);

                login(mUsernameEditText.getText().toString(), mPasswordEditText.getText().toString());
            }
        });

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            // Return input text to activity
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            login(mUsernameEditText.getText().toString(), mPasswordEditText.getText().toString());
            return true;
        }
        return false;
    }

    private void login(String username, String password) {
        LoginDialogListener activity = (LoginDialogListener) getActivity();
        activity.onFinishLoginDialog(username, password);
        this.dismiss();
    }

    public interface LoginDialogListener {
        void onFinishLoginDialog(String username, String password);
    }
}
