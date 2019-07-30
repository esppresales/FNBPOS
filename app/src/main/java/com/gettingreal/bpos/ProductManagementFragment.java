package com.gettingreal.bpos;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;

import com.gettingreal.bpos.model.POSProduct;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 25/3/14.
 */
public class ProductManagementFragment extends Fragment {
    GridView mProductGridView;
    ProductAdapter mProductAdapter;
    Button mAddProductButton;
    Picasso mPicasso;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_management, container, false);

        mProductGridView = (GridView) view.findViewById(R.id.grid_view_products);

        mAddProductButton = (Button) view.findViewById(R.id.button_add);
        mAddProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                Fragment fragment = new ProductAddFragment();
                FragmentManager fm = getFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.layout_content, fragment);
                transaction.commit();
            }
        });

        mPicasso = ((MyApplication)getActivity().getApplication()).getPicasso();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        ((MyApplication)getActivity().getApplication()).getCache().evictAll();

        mProductAdapter = new ProductAdapter(getActivity());
        mProductGridView.setAdapter(mProductAdapter);
        mProductGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
                POSProduct POSProduct = mProductAdapter.mPOSProducts.get(i);

                Fragment fragment = new ProductEditFragment(POSProduct);
                FragmentManager fm = getFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.layout_content, fragment);
                transaction.commit();
            }
        });
    }

    public class ProductAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<POSProduct> mPOSProducts;


        public ProductAdapter(final Context aContext) {
            mContext = aContext;
            mPOSProducts = POSProduct.getAllProducts(mContext);
        }

        @Override
        public int getCount() {
            return mPOSProducts.size();
        }

        @Override
        public Object getItem(final int i) {
            return mPOSProducts.get(i);
        }

        @Override
        public long getItemId(final int i) {
            return i;
        }

        @Override
        public View getView(final int i, final View aView, final ViewGroup aViewGroup) {
            SquareImageView view = (SquareImageView) aView;
            if (aView == null) {
                view = new SquareImageView(mContext);
            }

            POSProduct POSProduct = mPOSProducts.get(i);
            mPicasso.load(POSProduct.getImageFile()).fit().into(view);

            return view;
        }
    }
}
