package com.gettingreal.bpos;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.gettingreal.bpos.model.POSProduct;
import com.jess.ui.TwoWayAbsListView;
import com.jess.ui.TwoWayGridView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 12:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class MenuFragment extends Fragment {

    private TwoWayGridView mMenuGridView;
    private MenuAdapter mMenuAdapter;
    private Picasso mPicasso;

    private BroadcastReceiver mChangedCategoryBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received changed_category broadcast");
            SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
            String selectedCategoryUid = settings.getString("category_uid", null);

            if (selectedCategoryUid != null) {
                ArrayList<POSProduct> products = POSProduct.getAllProductsForCategoryUid(getActivity(), selectedCategoryUid);
                mMenuAdapter = new MenuAdapter(getActivity(), products);
                mMenuGridView.setAdapter(mMenuAdapter);
            } else {
                ArrayList<POSProduct> products = POSProduct.getAllEnabledProducts(getActivity());
                mMenuAdapter = new MenuAdapter(getActivity(), products);
                mMenuGridView.setAdapter(mMenuAdapter);
            }

            if (mMenuAdapter.getCount() > 0) {
                POSProduct product = (POSProduct) mMenuAdapter.getItem(0);
                if (product != null) {
                    Intent detailIntent = new Intent("select-product");
                    // add data
                    detailIntent.putExtra("product_uid", product.getUid());
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(detailIntent);
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        mMenuGridView = (TwoWayGridView) view.findViewById(R.id.gridview_menu);

        mPicasso = ((MyApplication)getActivity().getApplication()).getPicasso();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        ((MyApplication)getActivity().getApplication()).getCache().evictAll();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mChangedCategoryBroadcastReceiver,
            new IntentFilter("changed_category"));

        SharedPreferences settings = getActivity().getSharedPreferences(getActivity().getPackageName(), 0);
        String selectedCategoryUid = settings.getString("category_uid", null);

        if (selectedCategoryUid != null) {
            ArrayList<POSProduct> products = POSProduct.getAllProductsForCategoryUid(getActivity(), selectedCategoryUid);
            mMenuAdapter = new MenuAdapter(getActivity(), products);
            mMenuGridView.setAdapter(mMenuAdapter);
        } else {
            ArrayList<POSProduct> products = POSProduct.getAllProducts(getActivity());
            mMenuAdapter = new MenuAdapter(getActivity(), products);
            mMenuGridView.setAdapter(mMenuAdapter);
        }

        if (mMenuAdapter.getCount() > 0) {
            POSProduct product = (POSProduct) mMenuAdapter.getItem(0);
            if (product != null) {
                Intent detailIntent = new Intent("select-product");
                // add data
                detailIntent.putExtra("product_uid", product.getUid());
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(detailIntent);
            }
        }
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mChangedCategoryBroadcastReceiver);
        super.onPause();
    }

    class MenuAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<POSProduct> mPOSProducts;


        // Constructor
        public MenuAdapter(Context context, ArrayList<POSProduct> aPOSProducts) {
            mContext = context;
            mPOSProducts = aPOSProducts;
        }

        @Override
        public int getCount() {
            return mPOSProducts.size();
        }

        @Override
        public Object getItem(int position) {
            return mPOSProducts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            POSProduct POSProduct = mPOSProducts.get(position);
            ImageView imageView = null;
            if (convertView != null) {
                imageView = (ImageView) convertView;
                mPicasso.cancelRequest(imageView);
            }
            if (imageView == null) {
                imageView = new ImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setLayoutParams(new TwoWayAbsListView.LayoutParams(180, 180));
            }

            mPicasso.load(POSProduct.getImageFile()).fit().placeholder(R.drawable.placeholder)
                .into(imageView);
            imageView.setTag(POSProduct.getName());
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View aView) {
                    String productName = (String) aView.getTag();
                    for (POSProduct POSProduct : mPOSProducts) {
                        if (POSProduct.getName().equalsIgnoreCase(productName)) {
                            Intent intent = new Intent("select-product");
                            // add data
                            intent.putExtra("product_uid", POSProduct.getUid());
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                            break;
                        }
                    }
                }
            });
            return imageView;
        }
    }
}
