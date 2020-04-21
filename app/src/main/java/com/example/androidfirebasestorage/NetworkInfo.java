package com.example.androidfirebasestorage;

import android.content.Context;
import android.net.ConnectivityManager;

public class NetworkInfo { // This Class Use For Check Network Connectivity Info

    public static int getNetworkStatus(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null) {

            switch (networkInfo.getType()) {

                case ConnectivityManager.TYPE_WIFI : // If Uer Using WIFI
                    return 1;
                case ConnectivityManager.TYPE_MOBILE : // If User Using Mobile
                    return 2;
                default : // Don't Have Network Connectivity
                    return 3;
            }
        } else {
            return 0;
        }
    }

}
