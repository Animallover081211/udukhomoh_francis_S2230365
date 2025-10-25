package com.example.fxexchange;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CurrencyRssParser {
    private static final String TAG_ITEM = "item";
    private static final String TAG_TITLE = "title";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_PUB_DATE = "pubDate";
    
    private List<Currency> currencies;
    private String currentTag;
    private Currency currentCurrency;
    private StringBuilder currentText;
    private Date feedDate;
    
    public CurrencyRssParser() {
        currencies = new ArrayList<>();
    }
    
    public List<Currency> parse(InputStream inputStream) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false); // Disable validation to handle malformed XML
        XmlPullParser parser = factory.newPullParser();
        
        try {
            parser.setInput(inputStream, null);
            
            int eventType = parser.getEventType();
            currentText = new StringBuilder();
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                try {
                    String tagName = parser.getName();
                    
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if (TAG_ITEM.equals(tagName)) {
                                currentCurrency = new Currency();
                            }
                            currentTag = tagName;
                            currentText.setLength(0);
                            break;
                            
                        case XmlPullParser.TEXT:
                            if (currentText != null) {
                                String text = parser.getText();
                                if (text != null) {
                                    // Clean up malformed entities
                                    text = cleanXmlText(text);
                                    currentText.append(text);
                                }
                            }
                            break;
                            
                        case XmlPullParser.END_TAG:
                            if (TAG_ITEM.equals(tagName)) {
                                if (currentCurrency != null) {
                                    currencies.add(currentCurrency);
                                }
                            } else if (TAG_TITLE.equals(tagName) && currentCurrency != null) {
                                parseTitle(currentText.toString());
                            } else if (TAG_DESCRIPTION.equals(tagName) && currentCurrency != null) {
                                parseDescription(currentText.toString());
                            } else if (TAG_PUB_DATE.equals(tagName)) {
                                parsePubDate(currentText.toString());
                            }
                            currentTag = null;
                            break;
                    }
                    
                    eventType = parser.next();
                } catch (XmlPullParserException e) {
                    System.out.println("DEBUG: XML parsing error, skipping malformed content: " + e.getMessage());
                    // Skip malformed content and continue
                    try {
                        eventType = parser.next();
                    } catch (Exception skipException) {
                        break; // Exit if we can't continue
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Fatal XML parsing error: " + e.getMessage());
            // Return empty list if parsing completely fails
            return new ArrayList<>();
        }
        
        System.out.println("DEBUG: Total currencies parsed: " + currencies.size());
        return currencies;
    }
    
    private String cleanXmlText(String text) {
        if (text == null) return "";
        
        // Replace common malformed entities
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&apos;", "'");
        
        // Remove any remaining malformed entities
        text = text.replaceAll("&[^;]*;", "");
        
        return text.trim();
    }
    
    private void parseTitle(String title) {
        // Parse title like "British Pound Sterling(GBP)/Australian Dollar(AUD)"
        System.out.println("DEBUG PARSER: Parsing title: '" + title + "'");
        
        if (title.contains("/") && title.contains("(") && title.contains(")")) {
            // Format: "British Pound Sterling(GBP)/Australian Dollar(AUD)"
            // Split by "/" to get the second part
            String[] parts = title.split("/");
            if (parts.length == 2) {
                String secondPart = parts[1]; // "Australian Dollar(AUD)"
                
                // Extract currency code from parentheses
                int startParen = secondPart.indexOf("(");
                int endParen = secondPart.indexOf(")");
                
                if (startParen != -1 && endParen != -1 && endParen > startParen) {
                    String currencyCode = secondPart.substring(startParen + 1, endParen).trim();
                    currentCurrency.setCode(currencyCode);
                    System.out.println("DEBUG PARSER: Set currency code from title: '" + currencyCode + "'");
                } else {
                    System.out.println("DEBUG PARSER: Could not find parentheses in: '" + secondPart + "'");
                }
            }
        } else if (title.contains("GBP/")) {
            // Fallback format: "GBP/USD"
            String[] parts = title.split("/");
            if (parts.length == 2) {
                String currencyCode = parts[1].trim();
                currentCurrency.setCode(currencyCode);
                System.out.println("DEBUG PARSER: Set currency code from simple format: '" + currencyCode + "'");
            }
        } else {
            System.out.println("DEBUG PARSER: Title doesn't match expected format");
        }
    }
    
    private void parseDescription(String description) {
        // Parse description: "1 British Pound = 1.27 US Dollar"
        if (description.contains("=")) {
            String[] parts = description.split("=");
            if (parts.length == 2) {
                String ratePart = parts[1].trim();
                
                // Extract rate (first number)
                String[] tokens = ratePart.split("\\s+");
                if (tokens.length >= 2) {
                    try {
                        double rate = Double.parseDouble(tokens[0]);
                        currentCurrency.setRate(rate);
                        
                        // Extract currency name (everything after the rate)
                        StringBuilder nameBuilder = new StringBuilder();
                        for (int i = 1; i < tokens.length; i++) {
                            if (i > 1) nameBuilder.append(" ");
                            nameBuilder.append(tokens[i]);
                        }
                        String currencyName = nameBuilder.toString();
                        currentCurrency.setName(currencyName);
                        
                        // Set country name (first part of currency name)
                        if (tokens.length >= 2) {
                            currentCurrency.setCountry(tokens[1]);
                        }
                    } catch (NumberFormatException e) {
                        // Handle parsing error
                    }
                }
            }
        }
    }
    
    private void parsePubDate(String pubDate) {
        // Parse date format like "Mon, 01 Jan 2024 12:00:00 GMT"
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        try {
            feedDate = format.parse(pubDate);
            if (currentCurrency != null) {
                currentCurrency.setLastUpdated(feedDate);
            }
        } catch (ParseException e) {
            // Handle parsing error
        }
    }
    
    public Date getFeedDate() {
        return feedDate;
    }
}
