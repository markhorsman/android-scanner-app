package com.android.jdejonge.jdejongescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ContItemsActivity extends Activity {
    private Button backToMainButton;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contitems);

        backToMainButton = (Button) findViewById(R.id.backToMainButton);

        View.OnClickListener backToMainButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };

        backToMainButton.setOnClickListener(backToMainButtonListener);

        Bundle extras = getIntent().getExtras();

        if(extras !=null) {
            String reference = extras.getString("Reference");
        }
    }

}
