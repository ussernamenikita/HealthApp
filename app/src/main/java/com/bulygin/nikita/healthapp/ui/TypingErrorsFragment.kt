package com.bulygin.nikita.healthapp.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.bulygin.nikita.healthapp.R
import ru.etu.parkinsonlibrary.database.consumer.DatabaseTypingErrorConsumer
import ru.etu.parkinsonlibrary.di.DependencyProducer
import ru.etu.parkinsonlibrary.typingerror.TypingErrorTextListener

class TypingErrorsFragment : androidx.fragment.app.Fragment(), TypingErrorTextListener.TypingErrorConsumer {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.inject()
    }

    private lateinit var module: DependencyProducer

    private fun inject() {
        if (activity == null) {
            return
        }
        if (activity is MainActivity) {
            this.module = DependencyProducer(activity!!.application)
        }
        textListener = TypingErrorTextListener(this)
        this.typingErrorsConsumer = module.createDatabaseTypingErrorConsumer()
    }

    lateinit var textListener: TypingErrorTextListener

    lateinit var typingErrorsConsumer: DatabaseTypingErrorConsumer
    private lateinit var typingErrorsCount: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.typing_errors_fragment, container, false)
        val editText = rootView.findViewById<EditText>(R.id.typing_errors_edit_text)
        this.typingErrorsCount = rootView.findViewById(R.id.typing_erorrs_count_tv)
        editText.addTextChangedListener(textListener)
        textListener.consumer = this
        return rootView
    }

    override fun onEvent(currentTimestamp: Long, changes: CharSequence, l: Long) {
        if (activity != null) {
            typingErrorsCount.text = "$currentTimestamp, $changes,$l"
        }
        typingErrorsConsumer.onEvent(currentTimestamp, changes, l)
    }


}