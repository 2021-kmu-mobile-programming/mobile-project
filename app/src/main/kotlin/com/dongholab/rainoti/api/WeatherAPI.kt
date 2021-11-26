package com.dongholab.rainoti.api

import com.dongholab.rainoti.data.Weather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAPI {
    @GET("/data/2.5/weather")
    fun getWeather(
        @Query("appid") appid: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("lang") lang: String? = null,
        @Query("units") units: Int? = null
    ): Call<Weather>
}