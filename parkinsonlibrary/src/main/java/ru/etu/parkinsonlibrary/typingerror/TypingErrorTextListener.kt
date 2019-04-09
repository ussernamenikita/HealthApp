package ru.etu.parkinsonlibrary.typingerror

import android.text.Editable
import android.text.TextWatcher
import ru.etu.parkinsonlibrary.getSize

/**
 * TextWatcher который отслеживает изменение текста и передает эти изменения
 * в [consumer] передается время события, в формате unix timestamp,
 * символ который был напечатан или один из дополнительных символов:
 * [eraseSymbol] - передается в [consumer] когда пользователь стер символ
 * [eraseFewSymbols] - передается в [consumer] когда пользователь стер несколько символов( например выделал несколько символов и стер)
 * [typeFewSymbol] - передается в [consumer] когда пользователь вставил несколько символов
 * [emptySymbol] - когда система сообщила об изменении текста,
 *      но сам текст не изменился ( может произойти когда пользователь вставит текст идентичный уже напечатоному в поле)
 */
class TypingErrorTextListener(var consumer: TypingErrorConsumer? = null) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    private var lastSize: Int = 0

    private var lastTimeStamp: Long? = null

    override fun afterTextChanged(s: Editable) {
        val currentTimeStamp = System.currentTimeMillis()
        val changes = this.getSymbolChanges(s)
        consumer?.onEvent(currentTimeStamp, changes, currentTimeStamp - (lastTimeStamp
                ?: currentTimeStamp))
        this.lastTimeStamp = currentTimeStamp
        this.lastSize = getSize(s)
    }

    private fun getSymbolChanges(s: Editable): CharSequence =
            ru.etu.parkinsonlibrary.getSymbolChanges(s, lastSize, eraseSymbol, eraseFewSymbols, typeFewSymbol)
                    ?: emptySymbol


    val eraseSymbol: CharSequence = "<-"
    val eraseFewSymbols: CharSequence = "FE"
    val typeFewSymbol: CharSequence = "FT"
    val emptySymbol: CharSequence = "EM"

    interface TypingErrorConsumer {
        fun onEvent(currentTimestamp: Long, changes: CharSequence, l: Long)
    }
}
