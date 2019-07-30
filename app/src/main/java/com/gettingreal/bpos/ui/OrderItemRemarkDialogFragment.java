package com.gettingreal.bpos.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by ivanfoong on 11/6/14.
 */
public class OrderItemRemarkDialogFragment extends DialogFragment {
    private EditText input;
    private OrderItemRemarkDialogFragmentListener mListener = null;
    private String mInitialRemark = "";

    public void setListener(final OrderItemRemarkDialogFragmentListener aListener) {
        mListener = aListener;
    }

    public void setInitialRemark(final String aInitialRemark) {
        mInitialRemark = aInitialRemark;

        if (mInitialRemark != null && input != null) {
            input.setText(mInitialRemark);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        if (input == null) {
            input = new EditText(getActivity());
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(30);
            input.setFilters(filterArray);

            if (mInitialRemark != null) {
                input.setText(mInitialRemark);
            }
        }

        return new AlertDialog.Builder(getActivity())
            .setTitle("Enter Remark:")
            .setView(input)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

                        if (mListener != null) {
                            mListener.onRemark(input.getText().toString());
                        }
                    }
                }
            )
            .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    }
                }
            ).create();
    }

    public interface OrderItemRemarkDialogFragmentListener {
        void onRemark(String remark);
    }
}
