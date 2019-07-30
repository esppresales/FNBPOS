package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.gettingreal.bpos.ui.OrderItemRemarkDialogFragment;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 6/3/14
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class OrderEditDialog extends DialogFragment {

    private String mProductUid;
    private int mQuantity;
    private String mRemark;
    private TextView mQuantityTextView;
    private Button mQuantityMinusButton, mQuantityPlusButton;
    private Button mAddRemarksButton, mVoidItemButton;
    private OrderEditDialogListener mListener;

    public OrderEditDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));

        View view = inflater.inflate(R.layout.fragment_order_edit_dialog, container);

        mQuantityTextView = (TextView) view.findViewById(R.id.txt_quantity);
        mQuantityMinusButton = (Button) view.findViewById(R.id.btn_quantity_minus);
        mQuantityPlusButton = (Button) view.findViewById(R.id.btn_quantity_plus);
        mAddRemarksButton = (Button) view.findViewById(R.id.btn_add_remarks);
        mVoidItemButton = (Button) view.findViewById(R.id.btn_void_item);

        mQuantityTextView.setText(String.valueOf(mQuantity));

        mQuantityMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                quantityMinusButtonPressed();
            }
        });

        mQuantityPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                quantityPlusButtonPressed();
            }
        });

        mAddRemarksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                OrderItemRemarkDialogFragment fragment = new OrderItemRemarkDialogFragment();

                fragment.setInitialRemark(mRemark);

                fragment.show(getFragmentManager(), "order_item_remark_dialog_fragment");

                final String finalProductUid = mProductUid;

                fragment.setListener(new OrderItemRemarkDialogFragment.OrderItemRemarkDialogFragmentListener() {
                    @Override
                    public void onRemark(final String remark) {
                        if (mListener != null) {
                            mListener.onRemarkChanged(finalProductUid, remark);
                        }

                        dismiss();
                    }
                });
            }
        });

        mVoidItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                confirmVoidItem();
            }
        });

        return view;
    }

    public String getProductUid() {
        return mProductUid;
    }

    public void setProductUid(final String aProductUid) {
        mProductUid = aProductUid;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public void setQuantity(int aQuantity) {
        mQuantity = aQuantity;
    }

    public void setListener(OrderEditDialogListener aListener) {
        mListener = aListener;
    }

    public String getRemark() {
        return mRemark;
    }

    public void setRemark(final String aRemark) {
        mRemark = aRemark;
    }

    public void quantityMinusButtonPressed() {
        if (mQuantity > 1) { // prevent reducing quantity to 0
            mQuantity--;
        } else if (mQuantity == 1) {
            // TODO check if user wants to void item instead?
        }
        mQuantityTextView.setText(String.valueOf(mQuantity));
        if (mListener != null) {
            mListener.onOrderQuantityChanged(mProductUid, mQuantity);
        }
    }

    public void quantityPlusButtonPressed() {
        mQuantity++;
        mQuantityTextView.setText(String.valueOf(mQuantity));
        if (mListener != null) {
            mListener.onOrderQuantityChanged(mProductUid, mQuantity);
        }
    }

    private void confirmVoidItem() {
        new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Confirmation")
            .setMessage("Void item?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mListener != null) {
                        mListener.onVoidItem(mProductUid);
                    }
                    OrderEditDialog.this.dismiss();
                }

            })
            .setNegativeButton("No", null)
            .show();
    }

    public interface OrderEditDialogListener {
        void onOrderQuantityChanged(String aProductUid, int quantity);

        void onRemarkChanged(String aProductUid, String remark);

        void onVoidItem(String aProductUid);
    }
}
