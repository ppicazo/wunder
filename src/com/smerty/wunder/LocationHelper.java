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

import java.util.List;

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

                final String url = "http://api.smerty.org/wunder/wx_near_all.php?output=json&lat="
                        + location.getLatitude() + "&lon=" + location.getLongitude();

                Ion.with(wunderActivity, url)
                        .progressDialog(wunderActivity.mProgressDialog)
                        .group(priceGroup)
                        .as(new TypeToken<List<Station>>() { })
                        .setCallback(new FutureCallback<List<Station>> () {
                            @Override
                            public void onCompleted(Exception e, List<Station> result) {
                                Log.i(TAG, "Callback firing.");
                                if (e == null) {
                                    wunderActivity.processLocalStations(result);
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

}
