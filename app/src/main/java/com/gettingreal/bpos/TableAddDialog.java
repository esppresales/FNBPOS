package com.gettingreal.bpos;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gettingreal.bpos.model.POSTable;

/**
 * Created by ivanfoong on 30/6/14.
 */
public class TableAddDialog extends DialogFragment {

    private Button mCancelButton, mAddButton;
    private EditText mTableUidEditText, mTableNameEditText;
    private DialogInterface.OnDismissListener mOnDismissListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_table_add, container);

        getDialog().setTitle("Add new table");

        mCancelButton = (Button) view.findViewById(R.id.button_cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                dismiss();
            }
        });

        mAddButton = (Button) view.findViewById(R.id.button_add);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {

                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mTableNameEditText.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(mTableUidEditText.getWindowToken(), 0);

                final String name = mTableNameEditText.getText().toString();
                final String uid = mTableUidEditText.getText().toString();
                POSTable newTable = POSTable.createTable(aView.getContext(), uid, name, "empty", false);

                if (newTable != null) {
                    Toast.makeText(aView.getContext(), "Table " + newTable.getName() + " added!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(aView.getContext(), "Failed to create Table " + newTable.getName(), Toast.LENGTH_LONG).show();
                }

                dismiss();
            }
        });

        mTableUidEditText = (EditText) view.findViewById(R.id.edit_text_table_uid);
        mTableNameEditText = (EditText) view.findViewById(R.id.edit_text_table_name);

        return view;
    }

    public DialogInterface.OnDismissListener getOnDismissListener() {
        return mOnDismissListener;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        mOnDismissListener = onDismissListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss(dialog);
        }
    }
}