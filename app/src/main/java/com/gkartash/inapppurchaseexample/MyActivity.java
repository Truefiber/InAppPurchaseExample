package com.gkartash.inapppurchaseexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gkartash.inapppurchaseexample.util.IabHelper;
import com.gkartash.inapppurchaseexample.util.IabResult;
import com.gkartash.inapppurchaseexample.util.Inventory;
import com.gkartash.inapppurchaseexample.util.Purchase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MyActivity extends Activity {

    private static final String TAG = "MyActivity";
    private static final int PURCHASE_REQUEST_CODE = 123;

    IabHelper mHelper;
    Spinner purchasesSpinner;
    TextView priceTextView, purchaseTextView, inventoryItemTextView;
    Button itemsButton, buyButton, showPriceButton, consumeItemButton;
    Map<String, String> purchasesMap;
    String selectedItem;
    Activity mActivity;
    Inventory mInventory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mActivity = this;
        purchasesMap = new HashMap<String, String>();
        purchasesMap.put("Gain Experience", "android.test.purchased");
        purchasesMap.put("Happiness", "android.test.item_unavailable");
        purchasesSpinner = (Spinner) findViewById(R.id.purchasesSpinner);
        purchasesSpinner.setVisibility(View.INVISIBLE);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, new ArrayList<String>(purchasesMap.keySet()));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        purchasesSpinner.setAdapter(spinnerAdapter);
        purchasesSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);
        itemsButton = (Button) findViewById(R.id.itemsButton);
        buyButton = (Button) findViewById(R.id.buyButton);
        buyButton.setVisibility(View.INVISIBLE);
        itemsButton.setOnClickListener(itemsButtonListener);
        buyButton.setOnClickListener(buyButtonListener);
        showPriceButton = (Button) findViewById(R.id.showPriceButton);
        showPriceButton.setOnClickListener(showPriceOnClickListener);
        priceTextView = (TextView) findViewById(R.id.priceTextView);
        purchaseTextView = (TextView) findViewById(R.id.purchaseTextView);
        inventoryItemTextView = (TextView) findViewById(R.id.inventoryItemTextView);
        consumeItemButton = (Button) findViewById(R.id.consumeItemButton);
        consumeItemButton.setOnClickListener(consumeButtonListener);



        String publicKey = getString(R.string.googleapikey); //Use own API key

        mHelper = new IabHelper(this, publicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                String resultString;
                if (!result.isSuccess()) {
                    resultString = "Setup Failed";
                } else {
                    resultString = "Successful setup";
                    mHelper.queryInventoryAsync(queryListener);

                }

                Toast.makeText(getApplicationContext(), resultString, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    View.OnClickListener itemsButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            purchasesSpinner.setVisibility(View.VISIBLE);
            buyButton.setVisibility(View.VISIBLE);
            showPriceButton.setVisibility(View.VISIBLE);
        }
    };

    View.OnClickListener buyButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            mHelper.launchPurchaseFlow(mActivity, purchasesMap.get(selectedItem),
                    PURCHASE_REQUEST_CODE,
                    purchaseFinishedListener);

        }
    };

    View.OnClickListener showPriceOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Log.d(TAG, "in showPriceOnClickListener");

            List<String> itemsList = new ArrayList<String>(purchasesMap.values());
            mHelper.queryInventoryAsync(true, itemsList, queryListener);

        }
    };

    IabHelper.QueryInventoryFinishedListener queryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            if (result.isSuccess()) {



                if (selectedItem != null) {
                    String itemPrice = inv.getSkuDetails(purchasesMap.get(selectedItem)).getPrice();
                    priceTextView.setText(itemPrice);
                }

                if (inv.hasPurchase("android.test.purchased")) {
                    consumeItemButton.setEnabled(true);
                    inventoryItemTextView.setText("Experience");
                } else {
                    consumeItemButton.setEnabled(false);
                    inventoryItemTextView.setText("");
                }

                mInventory = inv;


            }

        }
    };

    AdapterView.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            priceTextView.setText("");
            selectedItem = (String) purchasesSpinner.getSelectedItem();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };

    IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {


            if (result.isFailure()) {
                Log.d(TAG, "Purchasing failed: " + result);
                return;
            }

            Toast.makeText(getApplicationContext(), R.string.successful_purchase,
                    Toast.LENGTH_SHORT).show();

            mHelper.queryInventoryAsync(queryListener);

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode,
                data);

        if (requestCode == PURCHASE_REQUEST_CODE) {
            mHelper.handleActivityResult(requestCode, resultCode, data);
        }

    }

    View.OnClickListener consumeButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            mHelper.consumeAsync(mInventory.getPurchase("android.test.purchased"),
                    consumeFinishedListener);

        }
    };

    IabHelper.OnConsumeFinishedListener consumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        @Override
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isSuccess()) {
                mHelper.queryInventoryAsync(queryListener);

            }
        }
    };
}