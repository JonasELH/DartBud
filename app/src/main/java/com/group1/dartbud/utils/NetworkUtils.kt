package com.group1.dartbud.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Brukes til å sjekke tilkobling før vi gjør kall mot Firestore,
// slik at appen kan vise en fornuftig feilmelding i stedet for at
// nettverkskallet bare timer ut eller feiler kryptisk.
object NetworkUtils {

    /**
     * Sjekker om enheten har internett-tilkobling
     * @param context Application context
     * @return true hvis internett er tilgjengelig, false ellers
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Ingen aktivt nettverk i det hele tatt (f.eks. flymodus)
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // NB: Dette sjekker kun at nettverket er *merket* som internett-kapabelt,
        // ikke at internett faktisk fungerer (f.eks. captive portal / dødt WiFi
        // kan fortsatt gi true her).
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
