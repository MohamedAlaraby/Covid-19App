package com.example.covid_19tracking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.ColorInt
import android.util.Log
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.covid_19tracking.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG: String = "Main Activity"
private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val ALL_STATES: String="All(Nationwide)"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentlyShownData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        supportActionBar?.title=getString(R.string.app_desc)
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)


        //fetch national data
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.d(TAG, "on Respose ${response}")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.d(TAG, "did not received a valid response body")
                    return
                }
                //the reason i put this function here is i want to make ui interactive only when we get a valid data
                setupEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.d(TAG, "update graph with national data")
                //update graph with national data
                updateDisplayWithData(nationalDailyData)

            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.d(TAG, "on Failure ${t.message.toString()}")
            }


        })


        //fetch states data in the spinner
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.d(TAG, "on Response $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.d(TAG, "did not received a valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.d(TAG, "update spinner with state names")
                //update spinner with states
            updateSpinnerWithStateData(perStateDailyData.keys)




            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.d(TAG, "on Failure ${t.message.toString()}")
            }
        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
         val stateAbbreviationList=stateNames.toMutableList()
         stateAbbreviationList.sort()
         stateAbbreviationList.add(0,ALL_STATES)
        //add state list as data source for the spinner

        val adapter=ArrayAdapter(this,R.layout.drop_down_item,stateAbbreviationList)
        binding.autoComleteTv.setAdapter(adapter)


        binding.autoComleteTv.setOnItemClickListener { parent, view, position, id ->
             val selectedState= parent.getItemAtPosition(position) as String
             val selectedData=perStateDailyData[selectedState] ?:nationalDailyData
             updateDisplayWithData(selectedData)
        }
    }

    private fun setupEventListeners() {
        //ticker view
        binding.tickerView.setCharacterLists(TickerUtils.provideNumberList())

        //Add a listener for the user scrubbing on the chart
        binding.sparkView.isScrubEnabled = true
        binding.sparkView.setScrubListener { itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }
        }


        //Respond to the radio button  selected events.

        binding.radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.rbWeek -> TimeScale.WEEK
                R.id.rbMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        binding.radioGroupMetricSelection.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_positive -> updateDisplayWithMetric(Metric.POSITIVE)
                R.id.rb_negative -> updateDisplayWithMetric(Metric.NEGATIVE)
                else -> updateDisplayWithMetric(Metric.DEATH)
            }

        }


    }

    private fun updateDisplayWithMetric(metric: Metric) {
        //update the color of the chart.
        val colorRes = when (metric) {
            Metric.POSITIVE -> R.color.colorPositive
            Metric.NEGATIVE -> R.color.colorNegative
            else -> R.color.colorDeath
        }
        @ColorInt val colorInt = ContextCompat.getColor(this, colorRes)

        binding.sparkView.lineColor = colorInt
        binding.tickerView.setTextColor(colorInt)


        //update the metric on the adapter.
        adapter.metric = metric
        adapter.notifyDataSetChanged()
        //Reset the number and the date shown in the bottom.
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShownData = dailyData

        //create new spark adapter with data
        adapter = CovidSparkAdapter(dailyData)
        binding.sparkView.adapter = adapter

        //update radio buttons to select the positive cases and max time by default.
        binding.rbPositive.isChecked = true
        binding.rbMax.isChecked = true
        //Display metric for the most recent date
        updateDisplayWithMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> covidData.negativeIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.DEATH -> covidData.deathIncrease
        }
        binding.tickerView.text = NumberFormat.getInstance().format(numCases)
        val outputDateFormat = SimpleDateFormat("MMM,dd yyyy", Locale.US)
        binding.tvDateLabel.text = outputDateFormat.format(covidData.dateChecked)
    }
}