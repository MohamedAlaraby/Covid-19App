package com.example.covid_19tracking

import java.util.Date

data class CovidData(
    val dateChecked:Date,
    val positiveIncrease:Int,
    val negativeIncrease:Int,
    val deathIncrease:Int,
    val state:String
)
