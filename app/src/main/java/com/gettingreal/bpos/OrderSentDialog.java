package com.gettingreal.bpos;

import android.app.DialogFragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 6/3/14
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */

public class OrderSentDialog extends DialogFragment {

    private Button mDoneButton;

    public OrderSentDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));

        View view = inflater.inflate(R.layout.fragment_order_sent_dialog, container);

        mDoneButton = (Button) view.findViewById(R.id.btn_done);

        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                OrderSentDialog.this.dismiss();

            }
        });

        return view;
    }
}
