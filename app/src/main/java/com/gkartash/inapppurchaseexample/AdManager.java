package com.gkartash.inapppurchaseexample;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.view.View;


import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
    private static final String MILLENIAL = "MM";

    private static final String EVENT_PUBLISHER_ID = "publisherid";
    private static final String EVENT_AD_NETWORK = "adnetwork";
    private static final String EVENT_AD_SIZE = "adsize";

    private String currentNetwork;

    LocationValet locationValet;


    private Context adContext;
    private OnInitCompletedListener initListener;
    private View advertisement;
    private AdSetup adSetup;

    public AdManager(Context context, String settingsUrl) {
        adContext = context;
        XML_SETTINGS_URL = settingsUrl;

    }

    public void initAsync(OnInitCompletedListener listener) {
        initListener = listener;
        adSetup = new AdSetup();
        adSetup.execute();

    }

    public interface OnInitCompletedListener {

        public void onInitCompleted(View advertisement);

    }

    public void destroy() {

        if (adSetup != null) {
            adSetup.cancel(true);
        }

        if (currentNetwork == null)
            return;
        if (currentNetwork.equals(ADMOB)) {
            ((AdView)advertisement).destroy();
        }

    }

    public void pause() {
        if (currentNetwork == null)
            return;
        if (currentNetwork.equals(ADMOB)) {
            ((AdView)advertisement).pause();
        } else if (currentNetwork.equals(MILLENIAL)) {
            locationValet.stopAquire();

        }

    }

    public void resume() {

        if (currentNetwork == null)
            return;

        if (currentNetwork.equals(ADMOB)) {
            ((AdView)advertisement).resume();
        } else if (currentNetwork.equals(MILLENIAL)) {

            locationValet.startAquire(true);

        }


    }

    private class AdSetup extends AsyncTask<Void, Void, View> {

        @Override
        protected View doInBackground(Void... voids) {
            Looper.prepare();
            requestXML();

            return parseXML();
        }

        @Override
        protected void onPostExecute(View ad) {

            initListener.onInitCompleted(ad);
            load();
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
                        setID(parser.getText());
                    } else if (parser.getName().equals(EVENT_AD_SIZE)) {
                        parser.next();
                        setSize(parser.getText());
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


        } else if (network.equals(MILLENIAL)) {

            currentNetwork = MILLENIAL;

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

    private void setID(String publisherID) {
        if (currentNetwork.equals(ADMOB)) {
            ((AdView)advertisement).setAdUnitId(publisherID);
        } else if (currentNetwork.equals(MILLENIAL)) {
            ((MMAdView)advertisement).setApid(publisherID);
            ((MMAdView)advertisement).setMMRequest(new MMRequest());
        }
    }

    private void setSize(String adSize) {
        if (currentNetwork.equals(ADMOB)) {

            if (adSize.equals("BANNER")) {

                ((AdView)advertisement).setAdSize(AdSize.BANNER);

            }
        }

    }

    private void finalizeAdvertisement() {



    }

    public void load() {
        if (currentNetwork.equals(ADMOB)) {
            AdRequest request = new AdRequest.Builder().build();
            ((AdView)advertisement).loadAd(request);

        } else if (currentNetwork.equals(MILLENIAL)) {
            (((MMAdView)advertisement)).setId(MMSDK.getDefaultAdId());
            ((MMAdView)advertisement).getAd();

        }
    }
}
