package com.dongholab.rainoti.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.util.Log

class NetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val conn = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()
        conn.registerNetworkCallback(builder.build(), object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                // 네트워크를 사용할 준비가 되었을 때
                Log.d("onAvailable", network.toString())
            }

            override fun onLost(network: Network) {
                // 네트워크가 끊겼을 때
                Log.d("onLost", network.toString())
            }

            override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
                Log.e("onCapabilitiesChanged", "The default network changed capabilities: " + networkCapabilities)
            }

            override fun onLinkPropertiesChanged(network : Network, linkProperties : LinkProperties) {
                Log.e("onLinkPropertiesChanged", "The default network changed link properties: " + linkProperties)
            }
        })

    }
}