package com.gettingreal.bpos;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.squareup.picasso.Picasso;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class DetailFragment extends Fragment {

    private ImageView mImageView;
    private TextView mNameTextView, mPriceTextView, mDescriptionTextView;
    private POSProduct mPOSProduct;
    private BroadcastReceiver mSelectProductBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received select-product broadcast");
            String uid = intent.getStringExtra("product_uid");

            for (POSProduct product : POSProduct.getAllEnabledProducts(context)) {
                if (product.getUid().contentEquals(uid)) {
                    mPOSProduct = product;
                    break;
                }
            }

            updateProduct();
        }
    };
    private Button mMinusQuantityButton, mPlusQuantityButton;
    private Picasso mPicasso;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        mImageView = (ImageView) view.findViewById(R.id.img_item);
        mNameTextView = (TextView) view.findViewById(R.id.txt_name);
        mPriceTextView = (TextView) view.findViewById(R.id.txt_price);
        mDescriptionTextView = (TextView) view.findViewById(R.id.txt_description);
        mDescriptionTextView.setMovementMethod(new ScrollingMovementMethod());
        mMinusQuantityButton = (Button) view.findViewById(R.id.btn_quantity_minus);
        mPlusQuantityButton = (Button) view.findViewById(R.id.btn_quantity_plus);

        mMinusQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                onMinusQuantityPressed();
            }
        });

        mPlusQuantityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                onPlusQuantityPressed();
            }
        });

        mPicasso = ((MyApplication)getActivity().getApplication()).getPicasso();

        updateProduct();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (POSProduct.getAllEnabledProducts(activity).size() > 0) {
            mPOSProduct = POSProduct.getAllEnabledProducts(activity).get(0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mSelectProductBroadcastReceiver,
            new IntentFilter("select-product"));

        updateProduct();
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mSelectProductBroadcastReceiver);
        super.onPause();
    }

    public void updateProduct() {
        if (mPOSProduct != null) {
            mPicasso.load(mPOSProduct.getImageFile()).fit().skipMemoryCache().into(mImageView);
            mNameTextView.setText(mPOSProduct.getName());
            mDescriptionTextView.setText(TextUtils.join("\n", mPOSProduct.getDescriptions().toArray()));
            mPriceTextView.setText(String.format("$%.2f", mPOSProduct.getPrice()));
        }
    }

    public void onMinusQuantityPressed() {
        if (mPOSProduct != null) {
            POSOrderItem.removePendingOrderItem(getActivity(), mPOSProduct.getUid(), 1);
        }
    }

    public void onPlusQuantityPressed() {
        if (mPOSProduct != null) {
            POSOrderItem.addPendingOrderItem(getActivity(), mPOSProduct.getUid(), 1);
        }
    }
}
