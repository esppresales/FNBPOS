package com.gettingreal.bpos;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gettingreal.bpos.helper.ImageHelper;
import com.gettingreal.bpos.model.POSCategory;
import com.gettingreal.bpos.model.POSProduct;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Created by ivanfoong on 25/3/14.
 */
public class ProductAddFragment extends Fragment {

    private static int RESULT_LOAD_IMAGE = 1;

    private Button mBackButton, mAddButton;
    private ImageView mImageView;
    private EditText mNameEditText, mPriceEditText, mDescriptionEditText;
    private Spinner mCategorySpinner;
    private CategorySpinnerAdapter mCategorySpinnerAdapter;
    private Bitmap mBitmap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_add, container, false);

        mBackButton = (Button) view.findViewById(R.id.button_back);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                Fragment fragment = new ProductManagementFragment();
                FragmentManager fm = getFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.layout_content, fragment);
                transaction.commit();
            }
        });

        mAddButton = (Button) view.findViewById(R.id.button_add);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                if (mNameEditText.getText().toString().equals("")) {
                    Toast.makeText(aView.getContext(), "Product name must not be empty!", Toast.LENGTH_LONG).show();
                }
                else if (mPriceEditText.getText().toString().equals("")) {
                    Toast.makeText(aView.getContext(), "Product price must not be empty!", Toast.LENGTH_LONG).show();
                }
                else if (mBitmap == null) {
                    Toast.makeText(aView.getContext(), "Product image must be assigned!", Toast.LENGTH_LONG).show();
                }
                else {
                    final String name = mNameEditText.getText().toString();
                    final String uid = name.toLowerCase().replace(" ", "_");
                    final ArrayList<String> descriptions = new ArrayList<String>();
                    String[] descriptionLines = mDescriptionEditText.getText().toString().split("\n");
                    for (int i = 0; i < descriptionLines.length; i++) {
                        descriptions.add(descriptionLines[i]);
                    }
                    final float price = Float.parseFloat(mPriceEditText.getText().toString());
                    final POSCategory selectedPOSCategory = (POSCategory) mCategorySpinner.getSelectedItem();
                    ArrayList<String> categoryUids = new ArrayList<String>();

                    categoryUids.add(selectedPOSCategory.getUid());

                    POSProduct product = POSProduct.createProduct(aView.getContext(), uid, name, descriptions, price, false, mBitmap, categoryUids);

                    Fragment fragment = new ProductManagementFragment();
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                    transaction.replace(R.id.layout_content, fragment);
                    transaction.commit();

                    Toast.makeText(aView.getContext(), product.getName() + " added!", Toast.LENGTH_LONG).show();

                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
                }
            }
        });

        mImageView = (ImageView) view.findViewById(R.id.image_view);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMAGE);
            }
        });

        mNameEditText = (EditText) view.findViewById(R.id.edit_text_name);

        mPriceEditText = (EditText) view.findViewById(R.id.edit_text_price);

        mDescriptionEditText = (EditText) view.findViewById(R.id.edit_text_description);

        mCategorySpinner = (Spinner) view.findViewById(R.id.spinner_category);
        mCategorySpinnerAdapter = new CategorySpinnerAdapter(getActivity());
        mCategorySpinner.setAdapter(mCategorySpinnerAdapter);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        System.gc();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            try {
                mBitmap = null;
                System.gc();

                mBitmap = ImageHelper.decodeBitmapUri(getActivity(), selectedImage);
                mBitmap = ImageHelper.squareBitmap(mBitmap, 400);
                mImageView.setImageBitmap(mBitmap);
            } catch (FileNotFoundException e) {

            }
        }
    }

    public class CategorySpinnerAdapter implements SpinnerAdapter {
        private Context mContext;
        private ArrayList<POSCategory> mCategories;

        public CategorySpinnerAdapter(final Context aContext) {
            mContext = aContext;
            mCategories = POSCategory.getAllCategories(mContext);
        }

        @Override
        public View getDropDownView(final int i, final View aView, final ViewGroup aViewGroup) {
            View view = aView;
            if (view == null) {
                view = View.inflate(mContext, android.R.layout.simple_spinner_dropdown_item, null);
            }

            POSCategory POSCategory = mCategories.get(i);

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(POSCategory.getName());

            return view;
        }

        @Override
        public void registerDataSetObserver(final DataSetObserver aDataSetObserver) {

        }

        @Override
        public void unregisterDataSetObserver(final DataSetObserver aDataSetObserver) {

        }

        @Override
        public int getCount() {
            return mCategories.size();
        }

        @Override
        public Object getItem(final int i) {
            return mCategories.get(i);
        }

        @Override
        public long getItemId(final int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(final int i, final View aView, final ViewGroup aViewGroup) {
            View view = aView;
            if (view == null) {
                view = View.inflate(mContext, android.R.layout.simple_spinner_item, null);
            }

            POSCategory POSCategory = mCategories.get(i);

            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(POSCategory.getName());

            return view;
        }

        @Override
        public int getItemViewType(final int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}