package com.android.jdejonge.jdejongescanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
//import android.support.design.widget.Snackbar;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.jdejonge.jdejongescanner.api.Insphire;
import com.android.jdejonge.jdejongescanner.model.ContItem;
import com.android.jdejonge.jdejongescanner.model.CustomerContact;
import com.android.jdejonge.jdejongescanner.model.Status;
import com.android.jdejonge.jdejongescanner.model.Stock;
import com.android.jdejonge.jdejongescanner.model.Contract;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.R.id.message;

public class MainActivity extends Activity implements
        EMDKListener,
        com.symbol.emdk.barcode.Scanner.DataListener,
        com.symbol.emdk.barcode.Scanner.StatusListener,
        ScannerConnectionListener,
        CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener
{

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String BASE_URL = "http://192.168.192.23:8080/";
    public final int DEFAULT_QUANTITY = 1;

    private static final int SCAN_ACTION_FIND_CUSTOMER = 1;
    private static final int SCAN_ACTION_FIND_STOCK = 2;

//    private final String username = "jdejong";
//    private final String password = "insphire";
//    private String base = username + ":" + password;
//    private String authHeader = "Basic " + Base64.encodeToString(base.getBytes(), Base64.NO_WRAP);

//    private String apiUsername;
//    private String apiPassword;
    private String authHeader;

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private boolean bContinuousMode = false;
    private List<ScannerInfo> deviceList = null;

    private int defaultIndex = 0;
    private int scannerIndex = 0; // Keep the selected scanner
    private int triggerIndex = 0;
    private int dataLength = 0;
    private String statusString = "";

    private TextView textViewData = null;
    private TextView textViewStatus = null;

    private TextView informationTextView;
    private TextView customerContactTextView;
    private TextView quantityTextView;
    private Button findStockButton;
    private Button fetchCustomerButton;
    private Button putInRentButton;
    private Button pullFromRentButton;
    private Button stopScanButton;
    private Button manualInputButton;
    private Button manualStockSearchButton;
    private EditText contItemQuantity;
    private EditText stockItemSearchInput;
    private RelativeLayout contItemQuantityParent;
    private RelativeLayout contactParent;
    private Spinner currentContract;
    private TextView currentContractTextView;
    private Button overviewButton;

    private Gson gson;
    private Retrofit retrofit;
    private Insphire insphire;

    private CustomerContact currentCustomerContact;
    private Stock currentStock;
    private HashMap<String, Contract> contractMap = new HashMap<String, Contract>();

    private int scanAction;
    private Boolean pauseScanProcessing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);

        // set a custom tint color for all system bars
        tintManager.setTintColor(Color.RED);

        textViewData = (TextView)findViewById(R.id.textViewData);
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);

        deviceList = new ArrayList<ScannerInfo>();

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            textViewStatus.setText("Status: " + "EMDKManager object request failed!");
        }

        pauseScanProcessing = false;

        informationTextView = (TextView) findViewById(R.id.informationTextView);
        customerContactTextView = (TextView) findViewById(R.id.customerContactTextView);
        quantityTextView = (TextView) findViewById(R.id.quantityTextView);
        findStockButton = (Button) findViewById(R.id.findStockButton);
        fetchCustomerButton = (Button) findViewById(R.id.fetchCustomerButton);
        putInRentButton = (Button) findViewById(R.id.putInRentButton);
        pullFromRentButton = (Button) findViewById(R.id.pullFromRentButton);
        contItemQuantityParent = (RelativeLayout) findViewById(R.id.contItemQuantityParent);
        contItemQuantity = (EditText) findViewById(R.id.contItemQuantity);
        stockItemSearchInput = (EditText) findViewById(R.id.stockItemSearchInput);
        stopScanButton = (Button) findViewById(R.id.stopScanButton);
        manualInputButton = (Button) findViewById(R.id.manualInputButton);
        manualStockSearchButton = (Button) findViewById(R.id.manualStockSearchButton);

        contactParent = (RelativeLayout) findViewById(R.id.contactParent);
        currentContract = (Spinner)findViewById(R.id.currentContract);
        currentContract.setOnItemSelectedListener(this);
        currentContractTextView = (TextView) findViewById(R.id.currentContractTextView);
        overviewButton = (Button) findViewById(R.id.overviewButton);

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

        View.OnClickListener stopScanButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchCustomerButton.setVisibility(View.VISIBLE);
                manualInputButton.setVisibility(View.GONE);
                manualStockSearchButton.setVisibility(View.GONE);
                stockItemSearchInput.setVisibility(View.GONE);
                stockItemSearchInput.setText("");

                switch (scanAction) {
                    case SCAN_ACTION_FIND_CUSTOMER:

                        break;
                    case SCAN_ACTION_FIND_STOCK:
                        findStockButton.setVisibility(View.VISIBLE);
                        break;
                }

                if (currentCustomerContact != null) {
                    findStockButton.setVisibility(View.VISIBLE);
                }

                if (currentStock != null) {
                    informationTextView.setText(currentStock.DESC1 + " (" + currentStock.ITEMNO + ")");

                    if (currentStock.STATUS == Stock.STATUS_AVAILABLE) {
                        // show put in rental button
                        contItemQuantity.setText(String.valueOf(DEFAULT_QUANTITY));
                        contItemQuantityParent.setVisibility(View.VISIBLE);
                        putInRentButton.setVisibility(View.VISIBLE);

                        if (currentStock.UNIQUE == 0) {
                            contItemQuantity.setVisibility(View.VISIBLE);
                            quantityTextView.setVisibility(View.VISIBLE);
                        }
                    } else if (currentStock.STATUS == Stock.STATUS_IN_RENT) {
                        // show pull from rental button if we have a ContItem
                        if (currentStock.CONTITEM != null) {
                            pullFromRentButton.setVisibility(View.VISIBLE);
                        } else if (currentStock.UNIQUE == 0) {
                            // show put in rental button
                            contItemQuantity.setText(String.valueOf(DEFAULT_QUANTITY));
                            contItemQuantityParent.setVisibility(View.VISIBLE);
                            putInRentButton.setVisibility(View.VISIBLE);

                            contItemQuantity.setVisibility(View.VISIBLE);
                            quantityTextView.setVisibility(View.VISIBLE);
                        }
                    }
                }

                stopScan(true);
            }
        };

        View.OnClickListener fetchCustomerButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // reset fields and textview
                informationTextView.setText("Scan een QR code");

                fetchCustomerButton.setVisibility(View.GONE);
                findStockButton.setVisibility(View.GONE);
                contItemQuantityParent.setVisibility(View.GONE);
                putInRentButton.setVisibility(View.GONE);
                contItemQuantity.setVisibility(View.GONE);
                pullFromRentButton.setVisibility(View.GONE);
                quantityTextView.setVisibility(View.GONE);
                contactParent.setVisibility(View.GONE);

                scanAction = SCAN_ACTION_FIND_CUSTOMER;

                // active scanner
                startScan();
            }
        };

        View.OnClickListener stockButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update informationTextView with the Stock item Desc field value

                manualInputButton.setVisibility(View.VISIBLE);

                // hide buttons
                informationTextView.setText("Scan een artikel");
                findStockButton.setVisibility(View.GONE);
                contItemQuantityParent.setVisibility(View.GONE);
                putInRentButton.setVisibility(View.GONE);
                contItemQuantity.setVisibility(View.GONE);
                pullFromRentButton.setVisibility(View.GONE);
                quantityTextView.setVisibility(View.GONE);
                fetchCustomerButton.setVisibility(View.GONE);

                scanAction = SCAN_ACTION_FIND_STOCK;

                startScan();
            }
        };

        View.OnClickListener putInRentButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide button
                contItemQuantityParent.setVisibility(View.GONE);
                findStockButton.setVisibility(View.GONE);
                contItemQuantityParent.setVisibility(View.GONE);
                putInRentButton.setVisibility(View.GONE);
                contItemQuantity.setVisibility(View.GONE);
                quantityTextView.setVisibility(View.GONE);

                insertContItem(currentStock.ITEMNO, ContItem.STATUS_CONTRACT_CREATED, Stock.STATUS_IN_RENT, Integer.parseInt(contItemQuantity.getText().toString()), currentCustomerContact);
            }
        };

        final View.OnClickListener pullFromRentButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide button
                pullFromRentButton.setVisibility(View.GONE);
                findStockButton.setVisibility(View.GONE);
                contItemQuantityParent.setVisibility(View.GONE);
                contItemQuantity.setVisibility(View.GONE);
                quantityTextView.setVisibility(View.GONE);

                int qty = 0;

                if (currentStock.UNIQUE == 0)
                    qty = Integer.parseInt(contItemQuantity.getText().toString());

                updateStockStatus(currentStock.ITEMNO, Stock.STATUS_AVAILABLE, qty);
            }
        };

        final View.OnClickListener manualInputButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide button
                manualInputButton.setVisibility(View.GONE);

                // shows buttons
                stockItemSearchInput.setVisibility(View.VISIBLE);
                manualStockSearchButton.setVisibility(View.VISIBLE);

                stopScan(false);
            }
        };

        final View.OnClickListener manualStockSearchButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide buttons
                stockItemSearchInput.setVisibility(View.GONE);
                manualStockSearchButton.setVisibility(View.GONE);
                stopScanButton.setVisibility(View.GONE);

                String searchString = stockItemSearchInput.getText().toString();
                stockItemSearchInput.setText("");

                informationTextView.setText("Ophalen artikel met code " + searchString + "...");
                getStockItem(searchString);
            }
        };

        View.OnClickListener overviewButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getBaseContext(), ContItemsActivity.class);
                i.putExtra("Reference", currentCustomerContact.REFERENCE);
                i.putExtra("CustomerName", currentCustomerContact.NAME);
                startActivity(i);
            }
        };

        fetchCustomerButton.setOnClickListener(fetchCustomerButtonListener);
        findStockButton.setOnClickListener(stockButtonListener);
        putInRentButton.setOnClickListener(putInRentButtonListener);
        pullFromRentButton.setOnClickListener(pullFromRentButtonListener);
        stopScanButton.setOnClickListener(stopScanButtonListener);
        manualInputButton.setOnClickListener(manualInputButtonListener);
        manualStockSearchButton.setOnClickListener(manualStockSearchButtonListener);
        overviewButton.setOnClickListener(overviewButtonListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // De-initialize scanner
        deInitScanner();

        // Remove connection listener
        if (barcodeManager != null) {
            barcodeManager.removeConnectionListener(this);
            barcodeManager = null;
        }

        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background

        // De-initialize scanner
        deInitScanner();

        // Remove connection listener
        if (barcodeManager != null) {
            barcodeManager.removeConnectionListener(this);
            barcodeManager = null;
            deviceList = null;
        }

        // Release the barcode manager resources
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.BARCODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground

        // Acquire the barcode manager resources
        if (emdkManager != null) {
            barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);

            // Add connection listener
            if (barcodeManager != null) {
                barcodeManager.addConnectionListener(this);
            }

            // Enumerate scanner devices
            enumerateScannerDevices();

            // Initialize scanner
            initScanner();
            setTrigger();
            setDecoders();
        }
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        textViewStatus.setText("Status: " + "EMDK open success!");

        this.emdkManager = emdkManager;

        // Acquire the barcode manager resources
        barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);

        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }

        // Enumerate scanner devices
        enumerateScannerDevices();
    }

    @Override
    public void onClosed() {

        if (emdkManager != null) {

            // Remove connection listener
            if (barcodeManager != null){
                barcodeManager.removeConnectionListener(this);
                barcodeManager = null;
            }

            // Release all the resources
            emdkManager.release();
            emdkManager = null;
        }
        textViewStatus.setText("Status: " + "EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {

        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanData> scanData = scanDataCollection.getScanData();
            for(ScanData data : scanData) {

                String dataString =  data.getData();

                new AsyncDataUpdate().execute(dataString);
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {

        ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle...";
                new AsyncStatusUpdate().execute(statusString);

                if (bContinuousMode) {
                    try {
                        // An attempt to use the scanner continuously and rapidly (with a delay < 100 ms between scans)
                        // may cause the scanner to pause momentarily before resuming the scanning.
                        // Hence add some delay (>= 100ms) before submitting the next read.
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        scanner.read();
                    } catch (ScannerException e) {
                        statusString = e.getMessage();
                        new AsyncStatusUpdate().execute(statusString);
                    }
                }
                new AsyncUiControlUpdate().execute(true);
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(false);
                break;
            case SCANNING:
                statusString = "Scanning...";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(false);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" is disabled.";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(true);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(true);
                break;
            default:
                break;
        }

        textViewData.setText(statusString);
    }

    private void enumerateScannerDevices() {

        if (barcodeManager != null) {

            int spinnerIndex = 0;
            deviceList = barcodeManager.getSupportedDevicesInfo();

            if ((deviceList != null) && (deviceList.size() != 0)) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    if(scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            }
            else {
                textViewStatus.setText("Status: " + "Failed to get the list of supported scanner devices! Please close and restart the application.");
            }
        }
    }

    private void setTrigger() {

        if (scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            switch (triggerIndex) {
                case 0: // Selected "HARD"
                    scanner.triggerType = TriggerType.HARD;
                    break;
                case 1: // Selected "SOFT"
                    scanner.triggerType = TriggerType.SOFT_ALWAYS;
                    break;
            }
        }
    }

    private void setDecoders() {

        if (scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            try {

                ScannerConfig config = scanner.getConfig();

                config.decoderParams.ean8.enabled = true;
                config.decoderParams.ean13.enabled = true;
                config.decoderParams.code39.enabled = true;
                config.decoderParams.code128.enabled = true;

                scanner.setConfig(config);

            } catch (ScannerException e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }


    private void startScan() {

        if(scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            try {
                stopScanButton.setVisibility(View.VISIBLE);
                // Submit a new read.
                scanner.read();

                pauseScanProcessing = false;
                bContinuousMode = true;

                new AsyncUiControlUpdate().execute(false);

            } catch (ScannerException e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }
        }

    }

    private void stopScan(Boolean hideStopScanButton) {
        if (hideStopScanButton || hideStopScanButton == null) {
            stopScanButton.setVisibility(View.GONE);
            manualInputButton.setVisibility(View.GONE);
        }

        if (scanner != null) {

            try {
                pauseScanProcessing = false;

                // Reset continuous flag
                bContinuousMode = false;

                // Cancel the pending read.
                scanner.cancelRead();

                new AsyncUiControlUpdate().execute(true);

            } catch (ScannerException e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }

    private void initScanner() {

        if (scanner == null) {

            if ((deviceList != null) && (deviceList.size() != 0)) {
                scanner = barcodeManager.getDevice(deviceList.get(defaultIndex));
            }
            else {
                textViewStatus.setText("Status: " + "Failed to get the specified scanner device! Please close and restart the application.");
                return;
            }

            if (scanner != null) {

                scanner.addDataListener(this);
                scanner.addStatusListener(this);

                try {
                    scanner.enable();
                } catch (ScannerException e) {

                    textViewStatus.setText("Status: " + e.getMessage());
                }
            }else{
                textViewStatus.setText("Status: " + "Failed to initialize the scanner device.");
            }
        }
    }

    private void deInitScanner() {

        if (scanner != null) {

            try {

                scanner.cancelRead();
                scanner.disable();

            } catch (ScannerException e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }
            scanner.removeDataListener(this);
            scanner.removeStatusListener(this);
            try{
                scanner.release();
            } catch (ScannerException e) {

                textViewStatus.setText("Status: " + e.getMessage());
            }

            scanner = null;
        }
    }

    private class AsyncDataUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        protected void onPostExecute(String result) {

            if (result != null) {
                if(dataLength ++ > 100) { //Clear the cache after 100 scans
                    dataLength = 0;
                }

                if (pauseScanProcessing) {
                    return;
                }

                Log.d(TAG, "Result from scanner: " + result);

                stopScanButton.setVisibility(View.GONE);

                // what is the current action?
                switch (scanAction) {
                    case SCAN_ACTION_FIND_CUSTOMER:
                        informationTextView.setText("Ophalen klant met referentie " + result + "...");
                        pauseScanProcessing = true;
                        getCustomerContact(result);
                        break;
                    case SCAN_ACTION_FIND_STOCK:
                        informationTextView.setText("Ophalen artikel met code " + result + "...");
                        pauseScanProcessing = true;
                        getStockItem(result);
                        break;
                }
            } else {
                Log.d(TAG, "no barcode/qrcode scanned.");
            }
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {

            textViewStatus.setText("Status: " + result);
        }
    }

    private class AsyncUiControlUpdate extends AsyncTask<Boolean, Void, Boolean> {


        @Override
        protected void onPostExecute(Boolean bEnable) {

        }

        @Override
        protected Boolean doInBackground(Boolean... arg0) {

            return arg0[0];
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {

        setDecoders();
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {

        String status;
        String scannerName = "";

        String statusBT = connectionState.toString();
        String scannerNameBT = scannerInfo.getFriendlyName();

        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }

        if (scannerName.equalsIgnoreCase(scannerNameBT)) {

            status = scannerNameBT + ":" + statusBT;
            new AsyncStatusUpdate().execute(status);

            switch(connectionState) {
                case CONNECTED:
                    initScanner();
                    setTrigger();
                    setDecoders();
                    break;
                case DISCONNECTED:
                    deInitScanner();
                    new AsyncUiControlUpdate().execute(true);
                    break;
            }
        }
        else {
            status =  statusString + " " + scannerNameBT + ":" + statusBT;
            new AsyncStatusUpdate().execute(status);
        }
    }

    private void getStockItem(final String barcode) {
        Log.d(TAG, "About to fetch Stock item!");

        Call<Stock> call = insphire.getStockItem(authHeader, barcode, currentCustomerContact.CONTNO, currentCustomerContact.ACCT, currentCustomerContact.REFERENCE);
        call.enqueue(new Callback<Stock>() {
            @Override
            public void onResponse(Call<Stock> call, Response<Stock> response) {
                findStockButton.setVisibility(View.VISIBLE);
                fetchCustomerButton.setVisibility(View.VISIBLE);
                stopScan(true);

                int statusCode = response.code();
                Stock stock = response.body();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    informationTextView.setText(stock.DESC1 + " (" + stock.ITEMNO + ")");
                    currentStock = stock;

                    if (stock.UNIQUE == 0) {
                        contItemQuantityParent.setVisibility(View.VISIBLE);
                        contItemQuantity.setVisibility(View.VISIBLE);
                        quantityTextView.setVisibility(View.VISIBLE);
                    }

                    if (stock.STATUS == Stock.STATUS_AVAILABLE) {
                        // show put in rental button
                        contItemQuantity.setText(String.valueOf(DEFAULT_QUANTITY));
                        contItemQuantityParent.setVisibility(View.VISIBLE);
                        putInRentButton.setVisibility(View.VISIBLE);
                    } else if (stock.STATUS == Stock.STATUS_IN_RENT) {
                        // show pull from rental button if we have a ContItem
                        if (stock.CONTITEM != null) {
                            contItemQuantityParent.setVisibility(View.VISIBLE);
                            pullFromRentButton.setVisibility(View.VISIBLE);

                            if (stock.UNIQUE == 0) {
                                contItemQuantity.setText(String.valueOf(stock.CONTITEM.QTY));
                                informationTextView.setText(stock.DESC1 + " (" + stock.ITEMNO + ") - " + stock.CONTITEM.QTY + " stuk(s)");
                            }
                        } else {
                            pullFromRentButton.setVisibility(View.GONE);

                            // bulk item, can be rented multiple times
                            if (stock.UNIQUE == 0) {
                                // show put in rental button
                                contItemQuantity.setText(String.valueOf(DEFAULT_QUANTITY));
                                contItemQuantityParent.setVisibility(View.VISIBLE);
                                putInRentButton.setVisibility(View.VISIBLE);

                                contItemQuantity.setVisibility(View.VISIBLE);
                                quantityTextView.setVisibility(View.VISIBLE);
                            } else {

                                Toast.makeText(getApplicationContext(), "Product verhuurd onder ander contract", Toast.LENGTH_LONG).show();
                            }
                        }
                    } else {
                        // stock item not available
                        Toast.makeText(getApplicationContext(), "Product niet beschikbaar", Toast.LENGTH_LONG).show();
                    }
                } else {
                    informationTextView.setText("");
                    showAPIErrorMessage(response);
                }
            }

            @Override
            public void onFailure(Call<Stock> call, Throwable t) {
                stopScan(true);
                findStockButton.setVisibility(View.VISIBLE);
                fetchCustomerButton.setVisibility(View.VISIBLE);

                String msg = t.getMessage();

                if (t.getMessage() == null || msg.length() < 1) {
                    msg = "Artikel met code " + barcode + " niet gevonden";
                }

                if (currentStock != null) {
                    informationTextView.setText(currentStock.DESC1 + " (" + currentStock.ITEMNO + ")");
                } else {
                    informationTextView.setText("");
                }

                showAPIFailureMessage(msg);

                Log.d(TAG, msg);
            }
        });
    }

    private void getCustomerContact(final String reference) {
        Log.d(TAG, "About to fetch Customer!");

        currentStock = null;

        Call<CustomerContact> call = insphire.getCustomerContact(authHeader, reference);
        call.enqueue(new Callback<CustomerContact>() {
            @Override
            public void onResponse(Call<CustomerContact> call, Response<CustomerContact> response) {
                stopScan(true);
                fetchCustomerButton.setVisibility(View.VISIBLE);

                int statusCode = response.code();
                CustomerContact customer = response.body();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    if (customer.CONTRACTS.length > 0) {
                        currentCustomerContact = customer;
                        customerContactTextView.setText("Huidige klant: " + customer.NAME);
                        updateContractsSpinner(customer);
                        contactParent.setVisibility(View.VISIBLE);

                        informationTextView.setText("Scan een artikel");
                        findStockButton.setVisibility(View.VISIBLE);
                    } else {
                        currentCustomerContact = null;
                        customerContactTextView.setText("Kies een andere klant");
                        Toast.makeText(getApplicationContext(), "Klant heeft geen actieve contract(en)", Toast.LENGTH_LONG).show();
                    }
                } else {
                    informationTextView.setText("");
                    showAPIErrorMessage(response);
                }
            }

            @Override
            public void onFailure(Call<CustomerContact> call, Throwable t) {
                fetchCustomerButton.setVisibility(View.VISIBLE);
                stopScan(true);

                String msg = t.getMessage();

                if (t.getMessage() == null || msg.length() < 1) {
                    msg = "Klant met referentie " + reference + " niet gevonden";
                }

                showAPIFailureMessage(msg);

                informationTextView.setText("");
                Log.d(TAG, msg);
            }
        });
    }

    private void updateContractsSpinner(CustomerContact customer)
    {
        // Spinner Drop down elements
        List<String> contracts = new ArrayList<>();

        for (int i = 0; i < customer.CONTRACTS.length; i++) {
            Contract contract = customer.CONTRACTS[i];
            contracts.add(contract.THEIRREF);

            contractMap.put(contract.THEIRREF, contract);
        }

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_spinner_item, contracts);

        // Drop down layout style - list view with radio button
        dataAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        currentContract.setAdapter(dataAdapter);
    }

    private void updateStockStatus(String itemno, final int status, final int qty) {
        final int originalStockStatus = currentStock.STATUS;
        currentStock.STATUS = status;

        Call<Status> call = insphire.updateStockStatus(
                authHeader,
                itemno,
                currentCustomerContact.CONTNO,
                currentCustomerContact.REFERENCE,
                qty,
                currentStock
        );
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                int statusCode = response.code();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    Boolean resultStatus = response.body().status;
                    if (resultStatus == true) {
                        // not all items are returned
                        if (currentStock.UNIQUE == 0 && qty > 0 && qty < currentStock.CONTITEM.QTY) {
                            currentStock.CONTITEM.QTY -= qty;
                            contItemQuantity.setText(String.valueOf(currentStock.CONTITEM.QTY));
                            contItemQuantityParent.setVisibility(View.VISIBLE);
                            contItemQuantity.setVisibility(View.VISIBLE);
                            quantityTextView.setVisibility(View.VISIBLE);
                            pullFromRentButton.setVisibility(View.VISIBLE);

                            informationTextView.setText(currentStock.DESC1 + " (" + currentStock.ITEMNO + ") - " + currentStock.CONTITEM.QTY + " stuk(s)");
                        } else {
                            informationTextView.setText(currentStock.DESC1 + " (" + currentStock.ITEMNO + ")");
                        }

                        Toast.makeText(getApplicationContext(), "Artikel opgeslagen", Toast.LENGTH_LONG).show();
                    } else {
                        currentStock.STATUS = originalStockStatus;

                        contItemQuantity.setText(String.valueOf(currentStock.CONTITEM.QTY));
                        contItemQuantityParent.setVisibility(View.VISIBLE);
                        contItemQuantity.setVisibility(View.VISIBLE);
                        quantityTextView.setVisibility(View.VISIBLE);
                        pullFromRentButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    currentStock.STATUS = originalStockStatus;

                    contItemQuantity.setText(String.valueOf(currentStock.CONTITEM.QTY));
                    contItemQuantityParent.setVisibility(View.VISIBLE);
                    contItemQuantity.setVisibility(View.VISIBLE);
                    quantityTextView.setVisibility(View.VISIBLE);
                    pullFromRentButton.setVisibility(View.VISIBLE);

                    showAPIErrorMessage(response);
                }

                findStockButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                String msg = t.getMessage();

                if (t.getMessage() == null || msg.length() < 1) {
                    msg = "Updated artikel mislukt";
                }

                showAPIFailureMessage(msg);
                Log.d(TAG, msg);

                findStockButton.setVisibility(View.VISIBLE);

                if (status == Stock.STATUS_IN_RENT) {
                    pullFromRentButton.setVisibility(View.VISIBLE);
                } else {
                    contItemQuantityParent.setVisibility(View.VISIBLE);
                }

                currentStock.STATUS = originalStockStatus;
            }
        });
    }

    private void insertContItem(String itemno, int constStatus, int stockStatus, int qty, CustomerContact customerContact) {
        if (qty < DEFAULT_QUANTITY) {
            qty = DEFAULT_QUANTITY;
        }

        Call<Status> call = insphire.insertContItem(authHeader, itemno, constStatus, stockStatus, qty, customerContact);
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                int statusCode = response.code();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    Boolean resultStatus = response.body().status;
                    if (resultStatus == true) {
                        currentStock.STATUS = Stock.STATUS_IN_RENT;

                        if (currentStock.CONTITEM != null) {
                            currentStock.CONTITEM.STATUS = ContItem.STATUS_IN_RENT;
                        }
                        Toast.makeText(getApplicationContext(), "Artikel opgeslagen en contract item aangemaakt.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    showAPIErrorMessage(response);
                    contItemQuantityParent.setVisibility(View.VISIBLE);
                }

                findStockButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                String msg = t.getMessage();

                if (t.getMessage() == null || msg.length() < 1) {
                    msg = "Aanmaken contract mislukt";
                }

                showAPIFailureMessage(msg);
                Log.d(TAG, msg);

                findStockButton.setVisibility(View.VISIBLE);
                contItemQuantityParent.setVisibility(View.VISIBLE);
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String contractRef = currentContract.getSelectedItem().toString();
        Contract contract= contractMap.get(contractRef);

        currentCustomerContact.CONTNO   = contract.CONTNO;
        currentCustomerContact.ESTRETD  = contract.ESTRETD;

        // refetch article
        if (currentStock != null) {
            informationTextView.setText("Opnieuw ophalen van artikel met code " + currentStock.ITEMNO + "...");
            pauseScanProcessing = true;
            getStockItem(currentStock.ITEMNO);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }
}
