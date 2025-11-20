package com.group1.dartbud.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {

    /**
     * Sjekker om enheten har internett-tilkobling
     * @param context Application context
     * @return true hvis internett er tilgjengelig, false ellers
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
