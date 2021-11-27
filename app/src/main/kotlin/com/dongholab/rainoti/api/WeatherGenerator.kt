package com.dongholab.rainoti.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherGenerator {
    companion object {
        fun generate() = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}