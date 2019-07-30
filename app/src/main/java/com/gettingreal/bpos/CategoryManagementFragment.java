package com.gettingreal.bpos;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gettingreal.bpos.helper.PrinterSessionManager;
import com.gettingreal.bpos.model.POSCategory;
import com.gettingreal.bpos.model.POSPrinter;
import com.gettingreal.bpos.model.POSProduct;

import java.util.ArrayList;

/**
 * Created by ivanfoong on 27/3/14.
 */
public class CategoryManagementFragment extends Fragment {

    ListView mCategoryListView;
    Button mAddCategoryButton;
    private CategoryListAdapter mCategoryListAdapter;
    public static String printer_IP;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_management, container, false);

        mCategoryListView = (ListView) view.findViewById(R.id.list_view_categories);
        mAddCategoryButton = (Button) view.findViewById(R.id.button_add);
        mAddCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                Fragment fragment = new CategoryAddFragment();
                FragmentManager fm = getFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                transaction.replace(R.id.layout_content, fragment);
                transaction.commit();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mCategoryListAdapter = new CategoryListAdapter(getActivity());
        mCategoryListView.setAdapter(mCategoryListAdapter);
    }

    public class CategoryListAdapter extends BaseAdapter {

        Context mContext;
        ArrayList<POSCategory> mCategories;
        PrinterSessionManager printerSessionManager;
        public CategoryListAdapter(final Context aContext) {
            mContext = aContext;
            mCategories = POSCategory.getAllCategories(mContext);
            printerSessionManager=new PrinterSessionManager(mContext);
        }

        @Override
        public int getCount() {
            return mCategories.size();
        }

        @Override
        public Object getItem(int i) {
            return mCategories.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = View.inflate(mContext, R.layout.category_item, null);

            final EditText categoryTextView = (EditText) view.findViewById(R.id.edit_text_category);
            final Spinner printerSpinner = (Spinner) view.findViewById(R.id.spinner_printer);
            final Button disableButton = (Button) view.findViewById(R.id.button_disable);
            final Button deleteButton = (Button) view.findViewById(R.id.button_delete);

            final POSCategory category = mCategories.get(i);

            PrinterSpinnerAdapter printerSpinnerAdapter = new PrinterSpinnerAdapter(getActivity());
            printerSpinner.setAdapter(printerSpinnerAdapter);
            for (int n = 0; n < printerSpinnerAdapter.getCount(); n++) {
                POSPrinter printer = (POSPrinter) printerSpinnerAdapter.getItem(n);

                if (category.getPrinterUids().contains(printer.getUid())) {
                    printerSpinner.setSelection(n);
                    break;
                }
            }

            printerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> aAdapterView, final View aView, final int i, final long l) {
                    category.clearPrinterUids();
                    POSPrinter mSelectedPrinter = (POSPrinter) printerSpinner.getSelectedItem();
                    category.addPrinterUid(mSelectedPrinter.getUid());
                    category.save(aView.getContext());
                }

                @Override
                public void onNothingSelected(final AdapterView<?> aAdapterView) {
                    category.clearPrinterUids();
                }
            });

            categoryTextView.setText(category.getName());
            categoryTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView aTextView, int i, KeyEvent aKeyEvent) {
                    return true;
                }
            });
            categoryTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View aView, boolean b) {
                    if (!category.getName().contentEquals(categoryTextView.getText().toString())) {
                        if (categoryTextView.getText().toString().equals("")) {
                            Toast.makeText(mContext, "Category name cannot be empty", Toast.LENGTH_LONG).show();
                            categoryTextView.setText(category.getName());
                        }
                        else {
                            category.setName(categoryTextView.getText().toString());
                            category.save(mContext);
                        }
                    }
                }
            });

            disableButton.setText(category.isDisabled()? "Disabled" : "Enabled");
            disableButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View aView) {
                    Button button = (Button) aView;
                    if (button.getText().equals("Disabled")) {
                        Toast.makeText(aView.getContext(), "Category " + category.getName() + " is enabled!", Toast.LENGTH_LONG).show();
                        category.setDisabled(false);
                        category.save(aView.getContext());
                    } else {
                        Toast.makeText(aView.getContext(), "Category " + category.getName() + " is disabled!", Toast.LENGTH_LONG).show();
                        category.setDisabled(true);
                        category.save(aView.getContext());
                    }

                    mCategoryListAdapter = new CategoryListAdapter(getActivity());
                    mCategoryListView.setAdapter(mCategoryListAdapter);
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View aView) {
                    // only allow delete of category if no items
                    ArrayList<POSProduct> products = POSProduct.getAllProductsForCategoryUid(aView.getContext(), category.getUid());
                    if (products.size() > 0) {
                        StringBuilder productNameSB = new StringBuilder();
                        for (POSProduct product : products) {
                            if (productNameSB.length() > 0) {
                                productNameSB.append(", ");
                            }
                            productNameSB.append(product.getName());
                        }
                        new AlertDialog.Builder(aView.getContext())
                                .setTitle("Unable to delete category")
                                .setMessage("Has products linked to this category (" + productNameSB.toString() + ")")
                                .setNegativeButton("Dismiss", null)
                                .setCancelable(false)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                    else {
                        new AlertDialog.Builder(getActivity())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Confirm Deletion")
                                .setMessage("Confirm Deletion of category, " + category.getName())
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface aDialogInterface, int i) {
                                        category.delete(aView.getContext());
                                        mCategoryListAdapter = new CategoryListAdapter(getActivity());
                                        mCategoryListView.setAdapter(mCategoryListAdapter);
                                    }
                                })
                                .show();
                    }
                }
            });

            return view;
        }
    }

    public class PrinterSpinnerAdapter implements SpinnerAdapter {
        private Context mContext;
        private ArrayList<POSPrinter> mPrinters;
        public PrinterSpinnerAdapter(final Context aContext) {
            mContext = aContext;
            mPrinters = POSPrinter.getAllPrinters(mContext);
            // remove master printer from selection
            POSPrinter masterPrinter = null;
            for (POSPrinter printer : mPrinters) {
                if (printer.getUid().contentEquals("master")) {
                    masterPrinter = printer;
                }
            }
            if (masterPrinter != null) {
                mPrinters.remove(masterPrinter);
            }
        }

        @Override
        public View getDropDownView(final int i, final View aView, final ViewGroup aViewGroup) {
            View view = aView;
            if (view == null) {
                view = View.inflate(mContext, R.layout.spinner_item, null);
            }

            POSPrinter printer = mPrinters.get(i);

            TextView textView = (TextView) view.findViewById(R.id.text);
            textView.setText(printer.getName());

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
            return mPrinters.size();
        }

        @Override
        public Object getItem(final int i) {
            return mPrinters.get(i);
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
                view = View.inflate(mContext, R.layout.spinner_item, null);
            }

            POSPrinter printer = mPrinters.get(i);
            TextView textView = (TextView) view.findViewById(R.id.text);
            textView.setText(printer.getName());
            return view;
        }

        @Override
        public int getItemViewType(final int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
