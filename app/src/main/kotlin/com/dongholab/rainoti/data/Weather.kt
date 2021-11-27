package com.dongholab.rainoti.data

import com.dongholab.rainoti.R

data class Coord(
    val lon: Double,
    val lat: Double
)

data class WeatherStatus(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int,
    val sea_level: Int,
    val grnd_level: Int
)

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double
)

data class Clouds(
    val all: Int
)

data class Sys(
    val type: Int,
    val id: Int,
    val country: String,
    val sunrise: Int,
    val sunset: Int
)

data class Weather(
    val coord: Coord,
    val weather: List<WeatherStatus>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val timeZone: Int,
    val id: Int,
    val name: String,
    val cod: Int
) {
    companion object {
        fun getWeatherIconById(id: Int) = when (id / 100) {
            8 -> {
                when (id) {
                    800 -> R.string.wi_day_sunny
                    else -> R.string.wi_day_cloudy
                }
            }
            7 -> R.string.wi_day_cloudy_gusts
            6 -> R.string.wi_day_snow
            5 -> R.string.wi_day_rain_wind
            3 -> R.string.wi_day_rain_mix
            2 -> R.string.wi_day_thunderstorm
            else -> R.string.wi_day_sunny
        }

        fun getWeatherDescById(id: Int) = when (id / 100) {
            8 -> {
                when (id) {
                    800 -> "맑은"
                    else -> "흐린"
                }
            }
            7 -> "짖게 흐린"
            6 -> "눈"
            5 -> "비"
            3 -> "이슬비"
            2 -> "천둥"
            else -> "맑은"
        }

        fun getNeedUmbrellaById(id: Int) = when (id / 100) {
            8 -> {
                when (id) {
                    800 -> false
                    else -> true
                }
            }
            7 -> false
            6 -> true
            5 -> true
            3 -> true
            2 -> true
            else -> false
        }
    }
}
