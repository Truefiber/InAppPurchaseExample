package com.gkartash.inapppurchaseexample;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Gennadiy on 26.06.2014.
 */
public class AdManager {

    private static final String TAG = "AdManager";

    private final String XML_SETTINGS_URL;
    private static final String SETTINGS_FILENAME = "AdManagerSettings";
    private static final String ADMOB = "AdMob";
    private static final String MILLENIUM = "MM";

    private static final String EVENT_PUBLISHER_ID = "publisherid";
    private static final String EVENT_AD_NETWORK = "adnetwork";

    private String currentNetwork;

    LocationValet locationValet;


    private Context adContext;
    private OnInitCompletedListener initListener;
    private View advertisement;

    public AdManager(Context context, String settingsUrl) {
        adContext = context;
        XML_SETTINGS_URL = settingsUrl;

    }

    public void initAsync(OnInitCompletedListener listener) {
        initListener = listener;
        new AdSetup().execute();

    }

    public interface OnInitCompletedListener {

        public void onInitCompleted(View advertisement);

    }

    public void destroy() {

    }

    public void pause() {
        locationValet.stopAquire();

    }

    public void resume() {

        locationValet.startAquire(true);


    }

    private class AdSetup extends AsyncTask<Void, Void, View> {

        @Override
        protected View doInBackground(Void... voids) {
            requestXML();
            parseXML();
            return parseXML();
        }

        @Override
        protected void onPostExecute(View ad) {
            advertisement = ad;
            initListener.onInitCompleted(ad);
        }
    }

    private void requestXML() {
        try {
            URL xmlUrl = new URL(XML_SETTINGS_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) xmlUrl.openConnection();

            urlConnection.connect();


            FileOutputStream fileOutputStream = adContext.openFileOutput(SETTINGS_FILENAME,
                    adContext.MODE_PRIVATE);

            InputStream stream = urlConnection.getInputStream();
            byte[] buffer = new byte[1024];

            while (stream.read(buffer) > -1) {
                fileOutputStream.write(buffer);
            }

            fileOutputStream.close();
            urlConnection.disconnect();



        } catch (MalformedURLException e) {
            Log.d(TAG, "Wrong url format" + e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private View parseXML() {

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

            XmlPullParser parser = factory.newPullParser();
            File file = adContext.getFileStreamPath(SETTINGS_FILENAME);
            FileReader fileReader = new FileReader(file);
            parser.setInput(fileReader);

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals(EVENT_AD_NETWORK)) {
                        parser.next();
                        createAdvertisement(parser.getText());

                    } else if (parser.getName().equals(EVENT_PUBLISHER_ID)) {
                        parser.next();
                        setupID(parser.getText());
                    }
                }


                eventType = parser.next();
            }

            finalizeAdvertisement();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return advertisement;


    }

    private void createAdvertisement(String network) {
        if (network.equals(ADMOB)) {

            currentNetwork = ADMOB;

            advertisement = new AdView(adContext);

        } else if (network.equals(MILLENIUM)) {

            currentNetwork = MILLENIUM;

            MMSDK.initialize(adContext);
            locationValet = new LocationValet(adContext, new LocationValet.ILocationValetListener() {
                @Override
                public void onBetterLocationFound(Location l) {
                    MMRequest.setUserLocation(l);
                }
            });

            advertisement = new MMAdView(adContext);
        }
    }

    private void setupID(String publisherID) {
        if (currentNetwork.equals(ADMOB)) {
            ((AdView)advertisement).setAdUnitId(publisherID);
        } else if (currentNetwork.equals(MILLENIUM)) {
            ((MMAdView)advertisement).setApid(publisherID);
        }
    }

    private void finalizeAdvertisement() {



    }

    public void load() {
        if (currentNetwork.equals(ADMOB)) {
            AdRequest request = new AdRequest.Builder().build();
            ((AdView)advertisement).loadAd(request);

        } else if (currentNetwork.equals(MILLENIUM)) {
            ((MMAdView)advertisement).getAd();

        }
    }
}
