package com.example.fxexchange;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class CurrencyService {
    private static final String TAG = "CurrencyService";
    private static final String RSS_URL = "https://www.fx-exchange.com/gbp/rss.xml";
    
    public interface CurrencyCallback {
        void onSuccess(List<Currency> currencies, String lastUpdated);
        void onError(String error);
    }
    
    public static void fetchCurrencies(Context context, CurrencyCallback callback) {
        if (!isNetworkAvailable(context)) {
            callback.onError("No internet connection available");
            return;
        }
        
        new FetchCurrencyTask(callback).execute();
    }
    
    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    private static class FetchCurrencyTask extends AsyncTask<Void, Void, List<Currency>> {
        private CurrencyCallback callback;
        private String error;
        private String lastUpdated;
        
        public FetchCurrencyTask(CurrencyCallback callback) {
            this.callback = callback;
        }
        
        @Override
        protected List<Currency> doInBackground(Void... voids) {
            try {
                URL url = new URL(RSS_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // Reduced timeout
                connection.setReadTimeout(8000); // Reduced timeout
                connection.setRequestProperty("User-Agent", "FxExchange App");
                connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    CurrencyRssParser parser = new CurrencyRssParser();
                    List<Currency> currencies = parser.parse(inputStream);
                    lastUpdated = parser.getFeedDate() != null ? 
                        parser.getFeedDate().toString() : "Unknown";
                    inputStream.close();
                    
                    // Check if parsing was successful
                    if (currencies == null || currencies.isEmpty()) {
                        error = "RSS feed parsing failed - no currencies found";
                        return null;
                    }
                    
                    return currencies;
                } else {
                    error = "HTTP Error: " + responseCode;
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error", e);
                error = "Network error: " + e.getMessage();
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Parsing error", e);
                error = "Parsing error: " + e.getMessage();
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(List<Currency> currencies) {
            if (currencies != null && !currencies.isEmpty()) {
                callback.onSuccess(currencies, lastUpdated);
            } else {
                callback.onError(error != null ? error : "Failed to fetch currency data");
            }
        }
    }
}
