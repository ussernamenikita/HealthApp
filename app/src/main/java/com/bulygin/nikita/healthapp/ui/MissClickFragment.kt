package com.bulygin.nikita.healthapp.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.bulygin.nikita.healthapp.R
import ru.etu.parkinsonlibrary.database.consumer.DatabaseMissClickConsumer
import ru.etu.parkinsonlibrary.di.DependencyProducer
import ru.etu.parkinsonlibrary.missclick.CloseTouchEvent
import ru.etu.parkinsonlibrary.missclick.MissClickEventsConsumer
import ru.etu.parkinsonlibrary.missclick.TrackingViewGroup

class MissClickFragment : Fragment(), MissClickEventsConsumer, SeekBar.OnSeekBarChangeListener {
    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    private lateinit var module: DependencyProducer

    private lateinit var missClickConsumer: DatabaseMissClickConsumer

    private lateinit var missClickResults: TextView

    private lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.inject()
    }

    private fun inject() {
        if (activity == null) {
            return
        }
        val a = activity as MainActivity
        module = DependencyProducer(a.application)
        missClickConsumer = module.createDatabaseMissclickConsumer()
    }

    private lateinit var trackingViewGroup: TrackingViewGroup

    private lateinit var maxDistanceTv: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.rootView = inflater.inflate(R.layout.miss_click_fragment_layout, container, false)
        val btn = rootView.findViewById<Button>(R.id.button)
        /**
         * Берем [TrackingViewGroup] из лэйаута
         */
        trackingViewGroup = rootView.findViewById(R.id.tracking_view)
        /**
         * Находим кнопку непопажание на которую нужно отслеживать
         */
        val sw = rootView.findViewById<Switch>(R.id.miss_click_draw_bound_rect_sw)
        val seekBar = rootView.findViewById<SeekBar>(R.id.miss_click_max_difference_seek_bar)
        this.missClickResults = rootView.findViewById(R.id.tracjing_view_miss_click_count_tv)
        this.maxDistanceTv = rootView.findViewById(R.id.missclick_max_diff_tv)
        /**
         * Добавляем кнопку как отслеживаемую
         */
        trackingViewGroup.addTrackedView(btn)
        /**
         * Задаем консьюмера,
         * Можно сразу задать консьюмера из базы данных,
         * тогда данные будут сразу писаться в базу
         * ```
         * trackingViewGroup.consumer = this.missClickConsumer
         * ```
         */
        trackingViewGroup.consumer = this
        sw.setOnCheckedChangeListener { _, isChecked ->
            trackingViewGroup.drawRect = isChecked
            trackingViewGroup.invalidate()
        }
        seekBar.setOnSeekBarChangeListener(this)
        updateMissClickCount(0)
        return rootView
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        /**
         * Задаем дистанцию на которой отслеживается непоподание
         */
        trackingViewGroup.maxDistanceValue = progress.toDouble()
        maxDistanceTv.text = activity?.getString(R.string.miss_click_max_diff_label_text, progress)
        trackingViewGroup.invalidate()
    }

    override fun onConsume(timestamp: Long, clickDistanceFromCenter: Double, closeEvents: ArrayList<CloseTouchEvent>) {
        /**
         * Передаем  дынные в базу
         */
        this.missClickConsumer.onConsume(timestamp, clickDistanceFromCenter, closeEvents)
    }


    private fun updateMissClickCount(missClickCount: Int) {
        this.missClickResults.text = getString(R.string.miss_click_count, missClickCount)
    }


}