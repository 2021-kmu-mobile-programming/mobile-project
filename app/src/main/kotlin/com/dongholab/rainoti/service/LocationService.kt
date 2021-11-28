package com.dongholab.rainoti.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.net.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dongholab.rainoti.MainActivity
import com.dongholab.rainoti.R
import com.dongholab.rainoti.SharedPreferenceUtil
import com.dongholab.rainoti.api.WeatherAPI
import com.dongholab.rainoti.api.WeatherGenerator
import com.dongholab.rainoti.data.Weather
import com.dongholab.rainoti.toText
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import android.media.AudioAttributes





/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */
class LocationService : Service() {
    companion object {
        val API_KEY: String = "7d2aae1e6ca6f98074073f32bb0f7ef4"

        private const val TAG = "LocationService"

        private const val PACKAGE_NAME = "com.dongholab.rainoti.service"

        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST = "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"
        internal const val ACTION_FOREGROUND_ONLY_WEATHER_BROADCAST = "$PACKAGE_NAME.action.FOREGROUND_ONLY_WEATHER_BROADCAST"

        internal const val EXTRA_LOCATION = "$PACKAGE_NAME.extra.LOCATION"
        internal const val EXTRA_WEATHER_ID = "$PACKAGE_NAME.extra.WEATHER_ID"
        internal const val EXTRA_WEATHER_DESC = "$PACKAGE_NAME.extra.WEATHER_DESC"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678
        private const val NOTIFICATION_PUSH_ID = 12345679

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
        private const val NOTIFICATION_PUSH_CHANNEL_ID = "push_channel_01"
    }
    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager
    private lateinit var connectvityManager: ConnectivityManager
    private lateinit var vibrator: Vibrator

    // TODO: Step 1.1, Review variables (no changes).
    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    private lateinit var connectvityCallback: ConnectivityManager.NetworkCallback

    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    lateinit var weatherAPI: WeatherAPI;

    var currentWeatherId: Int = 0

    override fun onCreate() {
        Log.d(TAG, "onCreate()")

        weatherAPI = WeatherGenerator.generate().create(WeatherAPI::class.java)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        connectvityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val builder: NetworkRequest.Builder = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(60)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                currentLocation = locationResult.lastLocation
                currentLocation?.run {
                    weatherAPI.getWeather(
                        API_KEY,
                        latitude,
                        longitude
                    ).enqueue(object: Callback<Weather> {
                        override fun onResponse(
                            call: Call<Weather>,
                            response: Response<Weather>
                        ) {
                            if (response.isSuccessful) {
                                val weather: Weather? = response.body()
                                weather?.let {
                                    val intent = Intent(ACTION_FOREGROUND_ONLY_WEATHER_BROADCAST)
                                    it.weather.first().let {
                                        // 날씨 정보 업데이트
                                        currentWeatherId = it.id
                                        intent.putExtra(EXTRA_WEATHER_ID, it.id)
                                        intent.putExtra(EXTRA_WEATHER_DESC, it.description)
                                    }
                                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                                }
                            }
                        }

                        override fun onFailure(call: Call<Weather>, t: Throwable) {}
                    })
                }


                val intent = Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
                intent.putExtra(EXTRA_LOCATION, currentLocation)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

                if (serviceRunningInForeground) {
                    notificationManager.notify(NOTIFICATION_ID, generateNotification(currentLocation))
                }
            }
        }

        connectvityCallback = object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // 네트워크를 사용할 준비가 되었을 때
                Log.d("onAvailable", network.toString())
                val caps: NetworkCapabilities? = connectvityManager.getNetworkCapabilities(network)
                Log.e("hasWifi", caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI).toString())
            }

            override fun onLost(network: Network) {
                // 와이파이가 끊겼을 때
                Log.d("onLost", network.toString())
                val caps: NetworkCapabilities? = connectvityManager.getNetworkCapabilities(network)
                Log.e("hasWifi", caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI).toString())
                Log.e("currentWeather", currentWeatherId.toString())

                // 우산이 필요한지 체크
                if (Weather.getNeedUmbrellaById(currentWeatherId)) {
                    notificationManager.notify(NOTIFICATION_PUSH_ID, generatePushNotification(currentWeatherId))
                }
            }

            override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
                Log.e("onCapabilitiesChanged", "The default network changed capabilities: " + networkCapabilities)
            }

            override fun onLinkPropertiesChanged(network : Network, linkProperties : LinkProperties) {
                Log.e("onLinkPropertiesChanged", "The default network changed link properties: " + linkProperties)
            }
        }
        connectvityManager.registerNetworkCallback(builder.build(), connectvityCallback)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        val cancelLocationTrackingFromNotification = intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind()")

        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        connectvityManager.unregisterNetworkCallback(connectvityCallback)
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification(currentLocation)
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        connectvityManager.unregisterNetworkCallback(connectvityCallback)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        SharedPreferenceUtil.saveLocationTrackingPref(this, true)
        startService(Intent(applicationContext, LocationService::class.java))

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                    stopSelf()
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    private fun generateNotification(location: Location?): Notification {
        Log.d(TAG, "generateNotification()")

        val mainNotificationText = location?.toText() ?: getString(R.string.no_location_text)
        val titleText = getString(R.string.app_name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setSummaryText("현재 날씨: " + currentWeatherId.toString())
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, LocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val activityPendingIntent = PendingIntent.getActivity(this, 0, launchActivityIntent, 0)

        val notificationCompatBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_home_black_24dp,  getString(R.string.launch_activity), activityPendingIntent)
            .addAction(R.drawable.ic_action_cancel, getString(R.string.stop_location_updates_button_text), servicePendingIntent)
            .build()
    }

    private fun generatePushNotification(weatherId: Int): Notification {
        Log.d(TAG, "generatePushNotification()")

        val titleText = getString(R.string.app_name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val mainText = "현재 날씨는 (${Weather.getWeatherDescById(weatherId)})입니다. 우산을 챙겨주세요"

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(titleText)

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(500)
        }

        val notificationCompatBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: LocationService
            get() = this@LocationService
    }
}
