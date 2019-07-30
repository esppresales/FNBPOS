package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gettingreal.bpos.model.POSOrderItem;
import com.gettingreal.bpos.model.POSProduct;
import com.gettingreal.bpos.model.POSTable;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ivanfoong
 * Date: 5/3/14
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class CartFragment extends Fragment implements OrderEditDialog.OrderEditDialogListener {

    private ListView mListView;
    private TextView mSubtotalTextView;
    private TableNumberAdapter mTableNumberAdapter;
    private Picasso mPicasso;
    private Button mCheckoutButton;

    private BroadcastReceiver mCartUpdatedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received cart-updated broadcast");
            updateCart();
        }
    };

    private boolean mIsServerOnline = false;
    private BroadcastReceiver mServerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("info", "Received server-status broadcast");
            mIsServerOnline = intent.getBooleanExtra("online", false);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        mListView = (ListView) view.findViewById(R.id.listView);
        mSubtotalTextView = (TextView) view.findViewById(R.id.txt_subtotal_amount);

        mCheckoutButton = (Button) view.findViewById(R.id.btn_checkout);
        mCheckoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View aView) {
                checkout();
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final POSOrderItem item = (POSOrderItem) parent.getItemAtPosition(position);
                showOrderEditDialog(item.getProductUid(), item.getQuantityOrdered(), item.getRemark());
//                view.animate().setDuration(2000).alpha(0)
//                        .withEndAction(new Runnable() {
//                            @Override
//                            public void run() {
//                                list.remove(item);
//                                adapter.notifyDataSetChanged();
//                                view.setAlpha(1);
//                            }
//                        });
            }

        });

        mPicasso = ((MyApplication)getActivity().getApplication()).getPicasso();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mCartUpdatedBroadcastReceiver,
                new IntentFilter("cart-updated"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mServerStatusBroadcastReceiver,
                new IntentFilter("server-status"));

        updateCart();
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mCartUpdatedBroadcastReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mServerStatusBroadcastReceiver);
        super.onPause();
    }

    private void updateCart() {
        ArrayList<POSOrderItem> pendingOrderItems = POSOrderItem.getPendingOrderItems(getActivity());
        final CartItemAdapter adapter = new CartItemAdapter(getActivity(), pendingOrderItems);
        mListView.setAdapter(adapter);

        mCheckoutButton.setEnabled(pendingOrderItems.size() > 0);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (POSOrderItem orderItem : pendingOrderItems) {
            POSProduct product = POSProduct.getProduct(getActivity(), orderItem.getProductUid());
            if (product != null) {
                subtotal = subtotal.add(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(orderItem.getQuantityOrdered())));
            }
        }
        mSubtotalTextView.setText(String.format("%.2f", subtotal));
    }

    public void showOrderEditDialog(final String aProductUid, final int aQuantity, final String aRemark) {
        FragmentManager fm = getFragmentManager();
        OrderEditDialog orderEditDialog = new OrderEditDialog();
        orderEditDialog.setProductUid(aProductUid);
        orderEditDialog.setQuantity(aQuantity);
        orderEditDialog.setRemark(aRemark);
        orderEditDialog.setListener(this);
        orderEditDialog.show(fm, "fragment_order_edit_dialog");
    }

    @Override
    public void onOrderQuantityChanged(final String aProductUid, final int aQuantity) {
        POSOrderItem orderItem = POSOrderItem.getPendingOrderItemForProductUid(getActivity(), aProductUid);
        orderItem.setQuantityOrdered(aQuantity);
        orderItem.save(getActivity());
        Toast.makeText(getActivity(), "Quantity changed to " + String.valueOf(aQuantity), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemarkChanged(final String aProductUid, final String remark) {
        POSOrderItem orderItem = POSOrderItem.getPendingOrderItemForProductUid(getActivity(), aProductUid);
        orderItem.setRemark(remark);
        orderItem.save(getActivity());
        Toast.makeText(getActivity(), "Remark saved!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVoidItem(final String aProductUid) {
        POSOrderItem.deletePendingOrderItem(getActivity(), aProductUid);
    }

    public void checkout() {
        if (mListView.getAdapter().getCount() > 0) {
        /*    if (mIsServerOnline) {
                Intent intent = new Intent("checkout");
                // add data
                intent.putExtra("message", "data");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
            else {
                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Checkout not available")
                        .setMessage("Service is not online")
                        .setNegativeButton("Dismiss", null)
                        .show();
            }*/

            Intent intent = new Intent("checkout");
            // add data
            intent.putExtra("message", "data");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
    }

    private class CartItemAdapter extends ArrayAdapter<POSOrderItem> {

        Context mContext;
        List<POSOrderItem> mOrderItems;

        public CartItemAdapter(Context context, List<POSOrderItem> aPOSOrderItems) {
            super(context, R.layout.cart_item, aPOSOrderItems);
            mContext = context;
            mOrderItems = aPOSOrderItems;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.cart_item, parent, false);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.img_item);
            TextView nameTextView = (TextView) rowView.findViewById(R.id.txt_name);
            TextView priceTextView = (TextView) rowView.findViewById(R.id.txt_price);
            TextView quantityTextView = (TextView) rowView.findViewById(R.id.txt_quantity);

            POSOrderItem orderItem = mOrderItems.get(position);
            POSProduct product = POSProduct.getProduct(mContext, orderItem.getProductUid());

            if (product != null) {
                nameTextView.setText(product.getName());
                priceTextView.setText(String.format("$%.2f", product.getPrice() * orderItem.getQuantityOrdered()));

                mPicasso.load(product.getImageFile()).fit().into(imageView);
            }

            if (orderItem.getQuantityOrdered() > 1) {
                quantityTextView.setVisibility(View.VISIBLE);
                quantityTextView.setText(String.valueOf(orderItem.getQuantityOrdered()));
            } else {
                quantityTextView.setVisibility(View.INVISIBLE);
            }

            return rowView;
        }
    }

    public class TableNumberAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<POSTable> mPOSTables;

        public TableNumberAdapter(final Context aContext, final ArrayList<POSTable> aPOSTables) {
            mContext = aContext;
            mPOSTables = aPOSTables;
        }

        @Override
        public int getCount() {
            return mPOSTables.size() + 1;
        }

        @Override
        public Object getItem(final int i) {
            if (i == 0) {
                return new Object(); // placeholder for takeaway option
            }
            return mPOSTables.get(i - 1);
        }

        @Override
        public long getItemId(final int i) {
            return i - 1;
        }

        @Override
        public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(android.R.layout.simple_spinner_dropdown_item, null);
            }

            TextView textView = (TextView) view.findViewById(android.R.id.text1);

            Object object = getItem(position);
            if (object instanceof POSTable) {
                POSTable POSTable = (POSTable) object;
                textView.setText(POSTable.getName());
            } else {
                textView.setText("Take Away");
            }

            return view;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(android.R.layout.simple_spinner_dropdown_item, null);
            }

            TextView textView = (TextView) view.findViewById(android.R.id.text1);

            Object object = getItem(position);
            if (object instanceof POSTable) {
                POSTable POSTable = (POSTable) object;
                textView.setText(POSTable.getName());
            } else {
                textView.setText("Take Away");
            }

            return view;
        }
    }
}
