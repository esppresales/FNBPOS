package com.gettingreal.bpos;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.gettingreal.bpos.model.POSSurcharge;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 20/6/14.
 */
public class SurchargeManagementFragment extends Fragment {
    ListView mSurchargeListView;
    private SurchargeListAdapter mSurchargeListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surcharge_management, container, false);

        mSurchargeListView = (ListView) view.findViewById(R.id.list_view_surcharges);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mSurchargeListAdapter = new SurchargeListAdapter(getActivity());
        mSurchargeListView.setAdapter(mSurchargeListAdapter);
    }

    public class SurchargeListAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<POSSurcharge> mSurcharges;

        public SurchargeListAdapter(final Context aContext) {
            mContext = aContext;
            mSurcharges = POSSurcharge.getAllSurcharges(mContext);
        }

        @Override
        public int getCount() {
            return mSurcharges.size();
        }

        @Override
        public Object getItem(int i) {
            return mSurcharges.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = View.inflate(mContext, R.layout.surcharge_item, null);

            final EditText surchargeNameEditText = (EditText) view.findViewById(R.id.edit_text_surcharge_name);
            final EditText surchargePercentageEditText = (EditText) view.findViewById(R.id.edit_text_surcharge_percentage);

            final POSSurcharge surcharge = mSurcharges.get(i);

            surchargeNameEditText.setText(surcharge.getName());
            surchargeNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence aCharSequence, final int i, final int i2, final int i3) {

                }

                @Override
                public void onTextChanged(final CharSequence aCharSequence, final int i, final int i2, final int i3) {

                }

                @Override
                public void afterTextChanged(final Editable aEditable) {
                    surcharge.setName(aEditable.toString());
                    surcharge.save(mContext);
                }
            });

            surchargePercentageEditText.setText(String.valueOf(surcharge.getPercentage()));
            surchargePercentageEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence aCharSequence, final int i, final int i2, final int i3) {

                }

                @Override
                public void onTextChanged(final CharSequence aCharSequence, final int i, final int i2, final int i3) {

                }

                @Override
                public void afterTextChanged(final Editable aEditable) {
                    try {
                        surcharge.setPercentage(Double.valueOf(aEditable.toString()));
                        surcharge.save(mContext);
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            });

            return view;
        }
    }
}
