package com.example.fxexchange;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CurrencyAdapter extends BaseAdapter {
    private Context context;
    private List<Currency> currencies;
    private LayoutInflater inflater;
    private SimpleDateFormat dateFormat;

    public CurrencyAdapter(Context context, List<Currency> currencies) {
        this.context = context;
        this.currencies = currencies;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getCount() {
        return currencies.size();
    }

    @Override
    public Object getItem(int position) {
        return currencies.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            holder = new ViewHolder();
            holder.text1 = convertView.findViewById(android.R.id.text1);
            holder.text2 = convertView.findViewById(android.R.id.text2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Currency currency = currencies.get(position);
        
        // Set currency info with flag emoji and color coding
        String currencyInfo = String.format("%s %s - %s", 
            currency.getFlagEmoji(), currency.getCode(), currency.getName());
        holder.text1.setText(currencyInfo);
        holder.text1.setTextColor(currency.getStrengthColor());
        holder.text1.setTextSize(16);
        
        // Set rate and country info with strength indicator
        String rateInfo = String.format("1 GBP = %.4f %s (%s) • %s", 
            currency.getRate(), currency.getCode(), currency.getCountry(), 
            currency.getStrengthDescription());
        holder.text2.setText(rateInfo);
        holder.text2.setTextSize(12);
        holder.text2.setTextColor(0xFF757575); // Secondary text color

        return convertView;
    }

    public void updateCurrencies(List<Currency> newCurrencies) {
        this.currencies = newCurrencies;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView text1;
        TextView text2;
    }
}
