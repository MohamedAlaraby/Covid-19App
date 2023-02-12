package com.example.covid_19tracking

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {

    @GET(value = "us/daily.json")
    fun getNationalData():Call<List<CovidData>>
    @GET(value = "states/daily.json")
    fun getStatesData():Call<List<CovidData>>

}