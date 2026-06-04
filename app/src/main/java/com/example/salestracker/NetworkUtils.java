package com.example.salestracker.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.widget.Toast;

import com.example.salestracker.R;

public class NetworkUtils {

    /**
     * Проверка наличия интернет-соединения
     * @param context Контекст приложения
     * @return true - интернет есть, false - нет интернета
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    /**
     * Показать сообщение об отсутствии интернета
     * @param context Контекст
     */
    public static void showNoInternetMessage(Context context) {
        if (context != null) {
            Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Проверка интернета с показом сообщения
     * @param context Контекст
     * @return true - интернет есть, false - нет интернета (и показано сообщение)
     */
    public static boolean checkAndShowNoInternet(Context context) {
        boolean hasInternet = isNetworkAvailable(context);
        if (!hasInternet) {
            showNoInternetMessage(context);
        }
        return hasInternet;
    }
}