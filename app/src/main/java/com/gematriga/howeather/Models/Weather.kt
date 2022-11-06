package com.gematriga.howeather.Models

data class Weather(
    val description: String,
    val icon: String,
    val id: Int,
    val main: String
)