package com.example.fxexchange;

import java.io.Serializable;
import java.util.Date;

public class Currency implements Serializable {
    private String code;
    private String name;
    private double rate;
    private String country;
    private Date lastUpdated;
    
    public Currency() {}
    
    public Currency(String code, String name, double rate, String country, Date lastUpdated) {
        this.code = code;
        this.name = name;
        this.rate = rate;
        this.country = country;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and Setters
    public String getCode() {
        return code != null ? code : "";
    }
    
    public void setCode(String code) {
        this.code = code != null ? code.trim() : "";
    }
    
    public String getName() {
        return name != null ? name : "";
    }
    
    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
    }
    
    public double getRate() {
        return rate;
    }
    
    public void setRate(double rate) {
        this.rate = rate;
    }
    
    public String getCountry() {
        return country != null ? country : "";
    }
    
    public void setCountry(String country) {
        this.country = country != null ? country.trim() : "";
    }
    
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    // Helper method to get currency strength color
    public int getStrengthColor() {
        if (rate < 1.0) {
            return 0xFF4CAF50; // Green for strong currency (< 1.0)
        } else if (rate < 5.0) {
            return 0xFF8BC34A; // Light green (1.0-5.0)
        } else if (rate < 10.0) {
            return 0xFFFFC107; // Amber (5.0-10.0)
        } else {
            return 0xFFFF5722; // Red for weak currency (> 10.0)
        }
    }
    
    // Helper method to get currency strength description
    public String getStrengthDescription() {
        if (rate < 1.0) {
            return "Strong";
        } else if (rate < 5.0) {
            return "Moderate";
        } else if (rate < 10.0) {
            return "Weak";
        } else {
            return "Very Weak";
        }
    }
    
    // Helper method to get flag emoji for country
    public String getFlagEmoji() {
        if (country == null || country.trim().isEmpty()) {
            return "💱"; // Default currency symbol
        }
        
        String countryLower = country.toLowerCase();
        if (countryLower.contains("united states")) {
            return "🇺🇸";
        } else if (countryLower.contains("european")) {
            return "🇪🇺";
        } else if (countryLower.contains("japan")) {
            return "🇯🇵";
        } else if (countryLower.contains("canada")) {
            return "🇨🇦";
        } else if (countryLower.contains("australia")) {
            return "🇦🇺";
        } else if (countryLower.contains("switzerland")) {
            return "🇨🇭";
        } else if (countryLower.contains("united kingdom")) {
            return "🇬🇧";
        } else {
            return "💱"; // Default currency symbol
        }
    }
    
    @Override
    public String toString() {
        return code + " - " + name + " (" + country + "): " + rate;
    }
}
