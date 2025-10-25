package com.example.fxexchange;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class AllCurrenciesActivity extends AppCompatActivity {
    
    private ListView listViewAllCurrencies;
    private TextView tvCount;
    private List<Currency> currencies;
    private CurrencyAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_currencies);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("All Exchange Rates");
        }
        
        // Initialize views
        listViewAllCurrencies = findViewById(R.id.listViewAllCurrencies);
        tvCount = findViewById(R.id.tvCurrencyCount);
        
        // Get currencies from intent
        currencies = (ArrayList<Currency>) getIntent().getSerializableExtra("currencies");
        
        if (currencies != null && !currencies.isEmpty()) {
            // Setup adapter
            adapter = new CurrencyAdapter(this, currencies);
            listViewAllCurrencies.setAdapter(adapter);
            
            // Update count
            tvCount.setText("Showing all " + currencies.size() + " currencies");
            
            // Set click listener
            listViewAllCurrencies.setOnItemClickListener((parent, view, position, id) -> {
                Currency selectedCurrency = currencies.get(position);
                android.widget.Toast.makeText(this, selectedCurrency.toString(), android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

