package com.example.fxexchange;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CurrencyService.CurrencyCallback {
    
    private TextView tvLastUpdated, tvError, tvResult;
    private EditText etSearch, etAmount;
    private Button btnSearch, btnConvert, btnSwap, btnClearSearch, btnViewAll;
    private ListView listViewCurrencies;
    private Spinner spinnerFrom, spinnerTo;
    
    private List<Currency> allCurrencies;
    private List<Currency> filteredCurrencies;
    private CurrencyAdapter currencyAdapter;
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> currencyCodes;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout based on orientation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main_land);
        } else {
            setContentView(R.layout.activity_main);
        }
        
        initializeViews();
        setupListeners();
        
        // Initialize data
        allCurrencies = new ArrayList<>();
        filteredCurrencies = new ArrayList<>();
        currencyCodes = new ArrayList<>();
        currencyCodes.add("GBP"); // Add GBP as default
        
        // Setup adapters
        currencyAdapter = new CurrencyAdapter(this, filteredCurrencies);
        listViewCurrencies.setAdapter(currencyAdapter);
        
        // Add GBP as base currency first
        currencyCodes.add("GBP");
        
        // Initialize spinner adapter
        spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, currencyCodes);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerFrom.setAdapter(spinnerAdapter);
        spinnerTo.setAdapter(spinnerAdapter);
        
        // Load sample data immediately for testing
        System.out.println("DEBUG: onCreate - About to load sample currencies");
        addSampleCurrencies();
        System.out.println("DEBUG: onCreate - Sample currencies loaded, spinner has: " + currencyCodes.size() + " items");
        
        // Show confirmation to user
        Toast.makeText(this, "App loaded with " + allCurrencies.size() + " currencies", Toast.LENGTH_LONG).show();
        
        // Also try to fetch currency data from RSS
        fetchCurrencyData();
    }
    
    private void initializeViews() {
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        tvError = findViewById(R.id.tvError);
        tvResult = findViewById(R.id.tvResult);
        etSearch = findViewById(R.id.etSearch);
        etAmount = findViewById(R.id.etAmount);
        btnSearch = findViewById(R.id.btnSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnViewAll = findViewById(R.id.btnViewAll);
        btnConvert = findViewById(R.id.btnConvert);
        btnSwap = findViewById(R.id.btnSwap);
        listViewCurrencies = findViewById(R.id.listViewCurrencies);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
    }
    
    private void setupListeners() {
        // Search button - click to search
        btnSearch.setOnClickListener(v -> {
            System.out.println("DEBUG: Search button clicked!");
            searchCurrencies();
        });
        
        // Clear search button - clears search box and shows all currencies
        btnClearSearch.setOnClickListener(v -> {
            System.out.println("DEBUG: Clear search button clicked!");
            etSearch.setText(""); // Clear the search box
            
            // Show first 5 currencies
            filteredCurrencies.clear();
            int limit = Math.min(5, allCurrencies.size());
            for (int i = 0; i < limit; i++) {
                filteredCurrencies.add(allCurrencies.get(i));
            }
            currencyAdapter.notifyDataSetChanged();
            
            // Hide error message
            tvError.setVisibility(View.GONE);
            
            // Hide keyboard
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
            
            Toast.makeText(this, "Search cleared - showing all " + allCurrencies.size() + " currencies", Toast.LENGTH_SHORT).show();
        });
        
        // View All button - opens new activity with all currencies
        btnViewAll.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, AllCurrenciesActivity.class);
            intent.putExtra("currencies", new ArrayList<>(allCurrencies));
            startActivity(intent);
        });
        
        btnConvert.setOnClickListener(v -> convertCurrency());
        btnSwap.setOnClickListener(v -> swapCurrencies());
        
        // Allow pressing Enter key in search box to trigger search
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            searchCurrencies();
            return true;
        });
        
        // Long press on search button to load sample data for testing
        btnSearch.setOnLongClickListener(v -> {
            loadSampleDataForTesting();
            Toast.makeText(this, "Sample data reloaded for testing", Toast.LENGTH_SHORT).show();
            return true;
        });
        
        listViewCurrencies.setOnItemClickListener((parent, view, position, id) -> {
            Currency selectedCurrency = filteredCurrencies.get(position);
            Toast.makeText(this, selectedCurrency.toString(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void fetchCurrencyData() {
        tvError.setVisibility(View.GONE);
        tvLastUpdated.setText("Loading currency data from RSS...");
        
        // Try to fetch from RSS feed (don't clear existing data)
        CurrencyService.fetchCurrencies(this, this);
        
        // Set a timeout to load sample data if RSS fails
        new android.os.Handler().postDelayed(() -> {
            if (allCurrencies.size() <= 5) { // If we only have sample data or less
                System.out.println("DEBUG: RSS timeout or failed, keeping sample data");
                Toast.makeText(this, "Using sample data (RSS feed unavailable)", Toast.LENGTH_SHORT).show();
            }
        }, 5000); // 5 second timeout - faster fallback
    }
    
    private void searchCurrencies() {
        String searchTerm = etSearch.getText().toString().trim().toLowerCase();
        
        System.out.println("DEBUG: searchCurrencies called with term: '" + searchTerm + "'");
        System.out.println("DEBUG: Total currencies available: " + allCurrencies.size());
        
        List<Currency> matchingCurrencies = new ArrayList<>();
        
        if (searchTerm.isEmpty()) {
            // Show first 5 currencies when search is empty
            matchingCurrencies.addAll(allCurrencies);
            tvError.setVisibility(View.GONE);
            System.out.println("DEBUG: Empty search - showing first 5 of " + allCurrencies.size() + " currencies");
        } else {
            // Filter currencies by code, name, or country
            for (Currency currency : allCurrencies) {
                String code = currency.getCode().toLowerCase();
                String name = currency.getName().toLowerCase();
                String country = currency.getCountry().toLowerCase();
                
                if (code.contains(searchTerm) || 
                    name.contains(searchTerm) || 
                    country.contains(searchTerm)) {
                    matchingCurrencies.add(currency);
                    System.out.println("DEBUG: Match found - " + currency.getCode() + " " + currency.getName());
                }
            }
            
            System.out.println("DEBUG: Search complete - found " + matchingCurrencies.size() + " matches");
            
            // Show feedback if no results found
            if (matchingCurrencies.isEmpty()) {
                tvError.setText("❌ No currencies found matching: \"" + searchTerm + "\"");
                tvError.setVisibility(View.VISIBLE);
                Toast.makeText(this, "No results for: " + searchTerm, Toast.LENGTH_SHORT).show();
            } else {
                tvError.setVisibility(View.GONE);
                Toast.makeText(this, "Found " + matchingCurrencies.size() + " currencies - showing first 5", Toast.LENGTH_SHORT).show();
            }
        }
        
        // Only show first 5 results in the main list
        filteredCurrencies.clear();
        int limit = Math.min(5, matchingCurrencies.size());
        for (int i = 0; i < limit; i++) {
            filteredCurrencies.add(matchingCurrencies.get(i));
        }
        
        currencyAdapter.notifyDataSetChanged();
    }
    
    private void convertCurrency() {
        String amountText = etAmount.getText().toString().trim();
        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double amount = Double.parseDouble(amountText);
            String fromCurrency = (String) spinnerFrom.getSelectedItem();
            String toCurrency = (String) spinnerTo.getSelectedItem();
            
            if (fromCurrency == null || toCurrency == null) {
                Toast.makeText(this, "Please select currencies", Toast.LENGTH_SHORT).show();
                return;
            }
            
            double result = convertAmount(amount, fromCurrency, toCurrency);
            tvResult.setText(String.format("%.2f %s = %.4f %s", 
                amount, fromCurrency, result, toCurrency));
                
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }
    
    private double convertAmount(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        if (fromCurrency.equals("GBP")) {
            // Convert from GBP to other currency
            Currency targetCurrency = findCurrencyByCode(toCurrency);
            if (targetCurrency != null) {
                return amount * targetCurrency.getRate();
            }
        } else if (toCurrency.equals("GBP")) {
            // Convert from other currency to GBP
            Currency sourceCurrency = findCurrencyByCode(fromCurrency);
            if (sourceCurrency != null) {
                return amount / sourceCurrency.getRate();
            }
        } else {
            // Convert between two non-GBP currencies via GBP
            Currency fromCurr = findCurrencyByCode(fromCurrency);
            Currency toCurr = findCurrencyByCode(toCurrency);
            if (fromCurr != null && toCurr != null) {
                // Convert from -> GBP -> to
                double gbpAmount = amount / fromCurr.getRate();
                return gbpAmount * toCurr.getRate();
            }
        }
        
        return 0.0;
    }
    
    private Currency findCurrencyByCode(String code) {
        for (Currency currency : allCurrencies) {
            if (currency.getCode().equals(code)) {
                return currency;
            }
        }
        return null;
    }
    
    private void swapCurrencies() {
        int fromPosition = spinnerFrom.getSelectedItemPosition();
        int toPosition = spinnerTo.getSelectedItemPosition();
        
        spinnerFrom.setSelection(toPosition);
        spinnerTo.setSelection(fromPosition);
    }
    
    @Override
    public void onSuccess(List<Currency> currencies, String lastUpdated) {
        runOnUiThread(() -> {
            // Only update if RSS feed has more data than sample data
            if (currencies == null || currencies.size() < 5) {
                System.out.println("DEBUG: RSS returned " + (currencies != null ? currencies.size() : 0) + " currencies, keeping existing data");
                Toast.makeText(this, "RSS feed incomplete, using sample data", Toast.LENGTH_SHORT).show();
                return; // Keep the sample data
            }
            
            System.out.println("DEBUG: RSS loaded " + currencies.size() + " currencies successfully");
            allCurrencies.clear();
            allCurrencies.addAll(currencies);
            
            // Update filtered list - show first 5
            filteredCurrencies.clear();
            int limit = Math.min(5, currencies.size());
            for (int i = 0; i < limit; i++) {
                filteredCurrencies.add(currencies.get(i));
            }
            currencyAdapter.notifyDataSetChanged();
            
            // Update spinner with all available currencies
            currencyCodes.clear();
            currencyCodes.add("GBP"); // Add GBP first as base currency
            
            System.out.println("DEBUG: Processing " + currencies.size() + " currencies for spinner...");
            int addedCount = 0;
            int emptyCodeCount = 0;
            for (int i = 0; i < currencies.size(); i++) {
                Currency currency = currencies.get(i);
                String code = currency.getCode();
                
                // Only show first 5 currencies in debug to avoid flooding logs
                if (i < 5) {
                    System.out.println("DEBUG: Currency " + i + " - Code: '" + code + "', Name: '" + currency.getName() + "', Rate: " + currency.getRate());
                }
                
                if (code == null || code.trim().isEmpty()) {
                    emptyCodeCount++;
                } else if (!currencyCodes.contains(code)) {
                    currencyCodes.add(code);
                    addedCount++;
                }
            }
            
            System.out.println("DEBUG: Added " + addedCount + " currencies to spinner, skipped " + emptyCodeCount + " with empty codes");
            System.out.println("DEBUG: First 10 spinner codes: " + currencyCodes.subList(0, Math.min(10, currencyCodes.size())));
            spinnerAdapter.notifyDataSetChanged();
            
            // Set default selections
            if (currencyCodes.size() > 0) {
                spinnerFrom.setSelection(0); // GBP
                if (currencyCodes.size() > 1) {
                    // Find USD, EUR, or JPY for default "to" selection
                    int defaultToIndex = 1; // Default to second item if no match found
                    for (int i = 0; i < currencyCodes.size(); i++) {
                        String code = currencyCodes.get(i);
                        if (code != null && (code.equals("USD") || code.equals("EUR") || code.equals("JPY"))) {
                            defaultToIndex = i;
                            break;
                        }
                    }
                    if (defaultToIndex < currencyCodes.size()) {
                        spinnerTo.setSelection(defaultToIndex);
                    }
                }
            }
            
            // Update last updated time
            tvLastUpdated.setText("Last updated: " + lastUpdated);
            
            // Hide error message
            tvError.setVisibility(View.GONE);
            
            Toast.makeText(this, "Currency data loaded: " + currencyCodes.size() + " currencies available", Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            tvError.setText("RSS Feed Error: " + error + " - Using sample data");
            tvError.setVisibility(View.VISIBLE);
            
            // Add some sample currencies for testing if RSS fails
            addSampleCurrencies();
            
            Toast.makeText(this, "RSS feed failed, using sample data: " + error, Toast.LENGTH_LONG).show();
        });
    }
    
    // Add a method to load sample data immediately for testing
    private void loadSampleDataForTesting() {
        addSampleCurrencies();
        Toast.makeText(this, "Loaded sample currencies for testing", Toast.LENGTH_SHORT).show();
    }
    
    private void addSampleCurrencies() {
        // Add sample currencies for testing
        allCurrencies.clear();
        allCurrencies.add(new Currency("USD", "US Dollar", 1.27, "United States", new Date()));
        allCurrencies.add(new Currency("EUR", "Euro", 1.18, "European Union", new Date()));
        allCurrencies.add(new Currency("JPY", "Japanese Yen", 150.0, "Japan", new Date()));
        allCurrencies.add(new Currency("CAD", "Canadian Dollar", 1.70, "Canada", new Date()));
        allCurrencies.add(new Currency("AUD", "Australian Dollar", 1.90, "Australia", new Date()));
        
        // Update UI with sample data - show first 5
        filteredCurrencies.clear();
        int limit = Math.min(5, allCurrencies.size());
        for (int i = 0; i < limit; i++) {
            filteredCurrencies.add(allCurrencies.get(i));
        }
        currencyAdapter.notifyDataSetChanged();
        
        // Update spinner
        currencyCodes.clear();
        currencyCodes.add("GBP");
        for (Currency currency : allCurrencies) {
            String code = currency.getCode();
            if (code != null && !code.trim().isEmpty()) {
                currencyCodes.add(code);
            }
        }
        
        System.out.println("DEBUG: Sample data loaded. Spinner has " + currencyCodes.size() + " currencies: " + currencyCodes);
        spinnerAdapter.notifyDataSetChanged();
        
        // Set default selections
        if (currencyCodes.size() > 1) {
            spinnerFrom.setSelection(0); // GBP
            spinnerTo.setSelection(1);   // USD (or first currency after GBP)
        }
        
        tvLastUpdated.setText("Last updated: Sample data (RSS failed)");
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Recreate activity when orientation changes to use correct layout
        recreate();
    }
}
