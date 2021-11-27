package com.dongholab.rainoti.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.net.*
import android.util.Log

class MyJobService : JobService() {
    companion object {
        private val TAG = "MyJobService"
    }

    override
    fun onStartJob(params: JobParameters): Boolean {
        Log.d(TAG, "onStartJob: ${params.jobId}")
        val conn = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()
        val callback: ConnectivityManager.NetworkCallback = object: ConnectivityManager.NetworkCallback() {
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
        }
        conn.registerNetworkCallback(builder.build(), callback)

        Thread {
            Thread.sleep(1000)
            Log.d(TAG, "doing Job in other thread")
            conn.unregisterNetworkCallback(callback)
            jobFinished(params, false)
        }
        return false
    }

    override
    fun onStopJob(params: JobParameters): Boolean {
        Log.d(TAG, "onStopJob: ${params.jobId}")
        return false
    }
}