package com.smerty.wunder;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.smerty.android.DocumentHelper;

import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationHelper implements LocationListener {

	static private final String TAG = LocationHelper.class.getSimpleName();

	transient final private LocationManager locationManager;
	transient final private String bestProvider;
	transient final private Wunder wunderActivity;

	private static final String[] LOCATION_STATUS = { "Out of Service",
			"Temporarily Unavailable", "Available" };

	public LocationHelper(final LocationManager locationManager, final Wunder wunderActivity) {

		this.locationManager = locationManager;
		this.wunderActivity = wunderActivity;

		final List<String> providers = locationManager.getAllProviders();
		for (String provider : providers) {
			printProvider(provider);
		}

		final Criteria criteria = new Criteria();
		bestProvider = locationManager.getBestProvider(criteria, false);
		Log.d(TAG, "BEST Provider:");
		printProvider(bestProvider);

		locationManager.requestLocationUpdates(bestProvider, 20000, 1, this);
	}

	public void onLocationChanged(final Location location) {
		printLocation(location);

		if (location == null) {
			// Failed
			Toast.makeText(wunderActivity.getBaseContext(),
					R.string.toast_location_failure, Toast.LENGTH_SHORT).show();
			return;
		} else {

            Object priceGroup = new Object();

            int pendingRequestCount = Ion.getDefault(wunderActivity).getPendingRequestCount(priceGroup);

            Log.i(TAG, "Pending requests: " + pendingRequestCount);

            if (pendingRequestCount == 0) {

                final String url = "http://api.smerty.org/wunder/wx_near_all.php?lat="
                        + location.getLatitude() + "&lon=" + location.getLongitude();

                Ion.with(wunderActivity, url)
                        .progressDialog(wunderActivity.mProgressDialog)
                        .group(priceGroup)
                        .asString()
                        .setCallback(new FutureCallback<String> () {
                            @Override
                            public void onCompleted(Exception e, String result) {
                                Log.i(TAG, "Callback firing.");
                                if (e == null) {
                                    Map<String, String> stations = downloadLocalStations(result);
                                    wunderActivity.processLocalStations(stations);
                                    Log.i(TAG, "Network Success.");
                                    Toast.makeText(wunderActivity.getBaseContext(), "Network Success...", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e(TAG, "Network Failure?", e);
                                    Toast.makeText(wunderActivity.getBaseContext(), "Network Failure...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                Toast.makeText(wunderActivity.getBaseContext(), "Loading...", Toast.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "Pending requests exist.");
            }

			locationManager.removeUpdates(this);

		}

	}

	public void onProviderDisabled(final String provider) {
		Log.d(TAG, "Provider Disabled: " + provider);
	}

	public void onProviderEnabled(final String provider) {
		Log.d(TAG, "Provider Enabled: " + provider);
	}

	public void onStatusChanged(final String provider, final int status,
			final Bundle extras) {
		Log.d(TAG, "Provider Status Changed: " + provider + ", Status="
				+ LOCATION_STATUS[status] + ", Extras=" + extras);
	}

	private void printProvider(final String provider) {
		final LocationProvider info = locationManager.getProvider(provider);
		Log.d(TAG, info.toString());
	}

	private void printLocation(final Location location) {
		if (location == null) {
			Log.d(TAG, "Location[unknown]");
		} else {
			Log.d(TAG, location.toString());
		}
	}

	public Map<String, String> downloadLocalStations(final String xml) {

		InputStream data;
		
		Map<String, String> stationMap = new HashMap<String, String>();
		
		try {

            data = new ByteArrayInputStream(xml.getBytes("UTF-8"));


            final Document doc = DocumentHelper.getDocument(data);

			try {

				final int pwsStationCount = doc.getElementsByTagName("neighborhood")
						.getLength();

				for (int i = 0; i < pwsStationCount; i++) {

					String tmpPWSName;
					String tmpPWSID;

					try {
						tmpPWSName = doc.getElementsByTagName("neighborhood")
								.item(i).getChildNodes().item(0).getNodeValue()
								.replaceAll("\\s+", " ");
					} catch (NullPointerException e) {
						tmpPWSName = doc.getElementsByTagName("city").item(i)
								.getChildNodes().item(0).getNodeValue()
								.replaceAll("\\s+", " ");
					}
					tmpPWSID = doc.getElementsByTagName("id").item(i)
							.getChildNodes().item(0).getNodeValue();

					if (tmpPWSID != null && tmpPWSName != null
							& tmpPWSID.length() > 0 && tmpPWSName.length() > 0) {
						stationMap.put(tmpPWSID, tmpPWSName);
					} else {
						Log.d(TAG, "didn't put station in map");
					}
				}

			} catch (Exception e) {
				Log.d(TAG, e.getMessage(), e);
				stationMap = null;
				// do nothing
			}
			//return stationMap;

		} catch (IOException e1) {
			//e1.printStackTrace();
			Log.d(TAG, e1.getMessage(), e1);
			stationMap = null;
		}
		return stationMap;
	}

}
