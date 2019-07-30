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
import android.widget.TextView;

import com.gettingreal.bpos.model.POSReceiptHeader;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 20/6/14.
 */
public class ReceiptHeaderManagementFragment extends Fragment {

    ListView mReceiptHeaderListView;
    private ReceiptHeaderListAdapter mReceiptHeaderListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_receipt_header_management, container, false);

        mReceiptHeaderListView = (ListView) view.findViewById(R.id.list_view_receipt_headers);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mReceiptHeaderListAdapter = new ReceiptHeaderListAdapter(getActivity());
        mReceiptHeaderListView.setAdapter(mReceiptHeaderListAdapter);
    }

    public class ReceiptHeaderListAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<POSReceiptHeader> mReceiptHeaders;

        public ReceiptHeaderListAdapter(final Context aContext) {
            mContext = aContext;
            mReceiptHeaders = POSReceiptHeader.getAllReceiptHeaders(mContext);
        }

        @Override
        public int getCount() {
            return mReceiptHeaders.size();
        }

        @Override
        public Object getItem(int i) {
            return mReceiptHeaders.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = View.inflate(mContext, R.layout.receipt_header_item, null);

            final TextView receiptHeaderTextView = (TextView) view.findViewById(R.id.text_view_receipt_header);
            final EditText receiptHeaderEditText = (EditText) view.findViewById(R.id.edit_text_receipt_header);

            final POSReceiptHeader receiptHeader = mReceiptHeaders.get(i);

            receiptHeaderTextView.setText("Header " + String.valueOf(i+1) + ": ");
            receiptHeaderEditText.setText(receiptHeader.getContent());

            receiptHeaderEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence aCharSequence, final int i, final int i2, final int i3) {

                }

                @Override
                public void onTextChanged(final CharSequence aCharSequence, final int i, final int i2, final int i3) {

                }

                @Override
                public void afterTextChanged(final Editable aEditable) {
                    receiptHeader.setContent(aEditable.toString());
                    receiptHeader.save(mContext);
                }
            });

            return view;
        }
    }
}
