package com.android.jdejonge.jdejongescanner;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.jdejonge.jdejongescanner.api.Insphire;
import com.android.jdejonge.jdejongescanner.model.ContItem;
import com.android.jdejonge.jdejongescanner.model.ContItemsInRent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ContItemsActivity extends Activity {
    private Button backToMainButton;
    private TextView customerContactName;
    private TableLayout contItemsInRent;

    private Gson gson;
    private Retrofit retrofit;
    private Insphire insphire;

    private static final String TAG = ContItemsActivity.class.getSimpleName();
    private String authHeader;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contitems);

        backToMainButton = (Button) findViewById(R.id.backToMainButton);
        customerContactName = (TextView) findViewById(R.id.customerContactName);
        contItemsInRent = (TableLayout) findViewById(R.id.contItemsInRent);

        View.OnClickListener backToMainButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };

        backToMainButton.setOnClickListener(backToMainButtonListener);

        String apiUrl   = Helper.getConfigValue(this, "api_url");
        String apiUser  = Helper.getConfigValue(this, "api_user");
        String apiPass  = Helper.getConfigValue(this, "api_pass");
        String authBase = apiUser + ":" + apiPass;
        authHeader = "Basic " + Base64.encodeToString(authBase.getBytes(), Base64.NO_WRAP);

        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(apiUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        insphire = retrofit.create(Insphire.class);

        Bundle extras = getIntent().getExtras();

        if(extras !=null) {
            String reference = extras.getString("Reference");
            String customerName = extras.getString("CustomerName");

            customerContactName.setText(customerName);
            getContItemsInRent(reference, customerName);
        }
    }

    private void getContItemsInRent(final String reference, final String customerName) {
        Call<ContItemsInRent> call = insphire.getContItemsInRent(authHeader, reference);
        call.enqueue(new Callback<ContItemsInRent>() {
            @Override
            public void onResponse(Call<ContItemsInRent> call, Response<ContItemsInRent> response) {
                int statusCode = response.code();
                ContItemsInRent result = response.body();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    Context self = getBaseContext();

                    TableRow.LayoutParams tvlparams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
                    TableRow.LayoutParams trlparams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

                    trlparams.setMargins(5, 5, 5, 5);

                    for (int i = 0; i < result.CONTITEMS.length; i++) {
                        ContItem item = result.CONTITEMS[i];

                        TableRow tr = new TableRow(self);
                        tr.setId(100 + i);

                        tr.setLayoutParams(trlparams);

                        Log.d(TAG, "I: " + i + ", itemnumber: " + item.ITEMNO);

                        TextView itemno = new TextView(self);
                        itemno.setId(200 + i);
                        itemno.setText(item.ITEMNO);
                        itemno.setPadding(0, 20, 0, 0);
                        itemno.setTextColor(Color.WHITE);
                        itemno.setLayoutParams(tvlparams);
                        tr.addView(itemno);

                        TextView qty = new TextView(self);
                        qty.setId(200 + i);
                        qty.setText(Integer.toString(item.QTY));
                        qty.setPadding(0, 20, 0, 0);
                        qty.setTextColor(Color.WHITE);
                        qty.setLayoutParams(tvlparams);
                        tr.addView(qty);

                        TextView itemdesc = new TextView(self);
                        itemdesc.setId(200 + i);
                        itemdesc.setText(item.ITEMDESC);
                        itemdesc.setPadding(0, 20, 0, 0);
                        itemdesc.setTextColor(Color.WHITE);
                        itemdesc.setLayoutParams(tvlparams);
                        tr.addView(itemdesc);

                        TextView theirref = new TextView(self);
                        theirref.setId(200 + i);
                        theirref.setText(item.THEIRREF);
                        theirref.setPadding(0, 20, 0, 0);
                        theirref.setTextColor(Color.WHITE);
                        theirref.setLayoutParams(tvlparams);
                        tr.addView(theirref);

                        // finally add this to the table row
                        contItemsInRent.addView(tr);
                    }

                    customerContactName.setVisibility(View.VISIBLE);
                    contItemsInRent.setVisibility(View.VISIBLE);

                } else {
                    showAPIErrorMessage(response);
                }
            }

            @Override
            public void onFailure(Call<ContItemsInRent> call, Throwable t) {
                String msg = t.getMessage();

                if (t.getMessage() == null || msg.length() < 1) {
                    msg = "Geen artikelen in huur gevonden vor klant met referentie: " + reference;
                }

                showAPIFailureMessage(msg);
                Log.d(TAG, msg);
            }
        });
    }

    private void showAPIErrorMessage(Response response) {
        try {
            JSONObject jObjError = new JSONObject(response.errorBody().string());
            Toast.makeText(getApplicationContext(), jObjError.getString("message"), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showAPIFailureMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
