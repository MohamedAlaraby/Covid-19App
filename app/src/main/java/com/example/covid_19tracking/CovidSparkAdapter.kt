package com.example.covid_19tracking

import android.graphics.RectF
import android.text.BoringLayout.Metrics
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>): SparkAdapter() {
   //default values
    var metric=Metric.POSITIVE
    var daysAgo=TimeScale.MAX

    override fun getItem(index: Int) = dailyData[index]
    override fun getY(index: Int): Float {
        val chosenDayDate = dailyData[index]
        return when(metric){
            Metric.POSITIVE->{chosenDayDate.positiveIncrease.toFloat()}
            Metric.NEGATIVE->{chosenDayDate.negativeIncrease.toFloat()}
            Metric.DEATH->{chosenDayDate.deathIncrease.toFloat()}
        }
    }
    override fun getCount(): Int = dailyData.size
    override fun getDataBounds(): RectF {
       val bounds= super.getDataBounds()
        if (daysAgo != TimeScale.MAX){
            bounds.left=count-daysAgo.numDays.toFloat()
        }

        return bounds
    }
}
