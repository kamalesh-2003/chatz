package com.example.auctioneer;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GeocodingTask extends AsyncTask<Location, Void, String> {

    private Context context;

    public GeocodingTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Location... locations) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(locations[0].getLatitude(), locations[0].getLongitude(), 1);
            if (addresses.size() > 0) {
                return addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String cityName) {
        if (cityName != null) {
            TextView cityTextView = ((aucmap) context).findViewById(R.id.cityTextView);
            aucmap.updateCityTextView(cityTextView, cityName);
        }
    }
}
