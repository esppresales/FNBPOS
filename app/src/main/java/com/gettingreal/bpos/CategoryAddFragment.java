package com.gettingreal.bpos;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gettingreal.bpos.model.POSCategory;

/**
 * Created by ivanfoong on 18/6/14.
 */
public class CategoryAddFragment extends Fragment {

    private Button mBackButton, mAddButton;
    private EditText mCategoryEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_add, container, false);
        mBackButton = (Button) view.findViewById(R.id.button_back);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View aView) {
                Fragment fragment = new CategoryManagementFragment();
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
                getActivity().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

                final String name = mCategoryEditText.getText().toString();

                if (name == null || name.equals("")) {
                    Toast.makeText(aView.getContext(), "Category name cannot be empty!", Toast.LENGTH_LONG).show();
                }
                else {
                    final String uid = name.toLowerCase();
                    POSCategory newCategory = POSCategory.createCategory(aView.getContext(), uid, name, 0, false);

                    Fragment fragment = new CategoryManagementFragment();
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction transaction = fm.beginTransaction();
                    transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                    transaction.replace(R.id.layout_content, fragment);
                    transaction.commit();

                    if (newCategory != null) {
                        Toast.makeText(aView.getContext(), "Category " + newCategory.getName() + " added!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(aView.getContext(), "Failed to create Category " + newCategory.getName(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        mCategoryEditText = (EditText) view.findViewById(R.id.edit_text_category);

        return view;
    }
}
