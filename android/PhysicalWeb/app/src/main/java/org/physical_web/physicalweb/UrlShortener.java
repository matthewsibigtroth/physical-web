/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.physical_web.physicalweb;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.UrlshortenerRequestInitializer;
import com.google.api.services.urlshortener.model.Url;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class shortens urls and also expands those short urls
 * to their original url.
 * Currently this class only supports google url shortener
 * TODO: support other url shorteners
 */
class UrlShortener {

  private static final String TAG = "UrlShortener";

  public interface UrlShortenerCallback {
    public void onUrlShortened(String shortUrl);

    public void onUrlLengthened(String longUrl);
  }

  /**
   * Create the shortened form
   * of the given url.
   *
   * @param longUrl The url that will be shortened
   * @return The short url for the given longUrl
   */
  // TODO: make sure this network operation is off the ui thread
  public static void shortenUrl(UrlShortenerCallback urlShortenerCallback, String longUrl) {
    Log.d(TAG, "longUrl:  " + longUrl);
    new ShortenUrlTask(urlShortenerCallback, longUrl).execute();
  }

  /**
   * Create a google url shortener interface object
   * and make a request to shorten the given url
   */
  private static class ShortenUrlTask extends AsyncTask<Void, Void, Void> {

    private String mLongUrl;
    private UrlShortenerCallback mUrlShortenerCallback;

    public ShortenUrlTask(UrlShortenerCallback urlShortenerCallback, String longUrl) {
      mUrlShortenerCallback = urlShortenerCallback;
      mLongUrl = longUrl;
    }

    @Override
    protected Void doInBackground(Void... params) {
      String shortUrl = null;
      Urlshortener urlshortener = createGoogleUrlShortener();
      Url url = new Url();
      url.setLongUrl(mLongUrl);
      try {
        Url response = urlshortener.url().insert(url).execute();
        //avoid possible NPE
        if (response != null) {
          shortUrl = response.getId();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      Log.d(TAG, "shortUrl:  " + shortUrl);
      mUrlShortenerCallback.onUrlShortened(shortUrl);
      return null;
    }
  }

  /**
   * Create an instance of the google url shortener object
   * and return it.
   *
   * @return The created shortener object
   */
  private static Urlshortener createGoogleUrlShortener() {
    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    JsonFactory jsonFactory = AndroidJsonFactory.getDefaultInstance();
    UrlshortenerRequestInitializer urlshortenerRequestInitializer = new UrlshortenerRequestInitializer();
    return new Urlshortener.Builder(httpTransport, jsonFactory, null)
        .setApplicationName("PhysicalWeb")
        .setUrlshortenerRequestInitializer(urlshortenerRequestInitializer)
        .build();
  }

  /**
   * Check if the given url is a short url.
   *
   * @param url The url that will be tested to see if it is short
   * @return The value that indicates if the given url is short
   */
  public static boolean isShortUrl(String url) {
    return url.startsWith("http://goo.gl/") || url.startsWith("https://goo.gl/");
  }

  /**
   * Takes any short url and converts it to the long url that is being pointed to.
   * Note: this method will work for all types of shortened urls as it inspect the
   * returned headers for the location.
   *
   * @param shortUrl The short url that will be lengthened
   * @return The lengthened url for the given short url
   */
  // TODO: make sure this network operation is off the ui thread
  public static void lengthenShortUrl(UrlShortenerCallback urlShortenerCallback, String shortUrl) {
    Log.d(TAG, "shortUrl: " + shortUrl);
    new LengthenShortUrlTask(urlShortenerCallback, shortUrl).execute();
  }

  private static class LengthenShortUrlTask extends AsyncTask<Void, Void, Void> {

    private UrlShortenerCallback mUrlShortenerCallback;
    private String mShortUrl;

    public LengthenShortUrlTask(UrlShortenerCallback urlShortenerCallback, String shortUrl) {
      mUrlShortenerCallback = urlShortenerCallback;
      mShortUrl = shortUrl;
    }

    @Override
    protected Void doInBackground(Void... params) {
      String longUrl = null;
      try {
        URL url = new URL(mShortUrl);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setInstanceFollowRedirects(false);
        longUrl = httpURLConnection.getHeaderField("location");
        longUrl = (longUrl != null) ? longUrl : mShortUrl;
      } catch (MalformedURLException e) {
        Log.w(TAG, "Malformed URL: " + mShortUrl);
      } catch (IOException e) {
        e.printStackTrace();
      }
      Log.d(TAG, "longUrl: " + longUrl);
      mUrlShortenerCallback.onUrlLengthened(longUrl);
      return null;
    }
  }
}
