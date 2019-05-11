package ru.etu.parkinsonlibrary.missclick

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

const val TAG = "TrackingView"
const val DEFAULT_TIMEOUT = 5000L
const val DEFAULT_MAX_DISTANCE = 20.0

/**
 * Контейнер который отслеживает все касания,
 * для того чтобы определять непопадание по элементам View.
 * Должен быть родителем для отслеживаемых эдементов View( не обязательно прямым)
 * Т.е допускается как иерархия подобного рода:
 * <TrackingViewGroup...>
 *     <TrackedView.../>
 * </TrackingViewGroup>
 * ,так и такого:
 * <FrameLayout...>
 *     <TrackingViewGroup...>
 *         <ConstraintLayout...>
 *             ...
 *             <TrackedView...>
 *             ...
 *         </ConstraintLayout>
 *     </TrackingViewGroup>
 * </FrameLayout...>
 * Все касания в периметре [maxDistanceValue] (значение указывается в dpi),
 * вокруг отмслеживаемого элемента запомнинаются c таймаутом в [timeOut] миллимекунд
 * Для добавление View как отслеживаемого используйте метод [addTrackedView].
 * Событием непоподание считается если пользователь больше одного раза коснулся рядом с отслеживаемым элементов
 * и затем нажал на сам эедемен.
 */
class TrackingViewGroup : FrameLayout, MaxDistanceProvider {
    override fun getMaxDistance(): Double = maxDistanceValue


    private lateinit var internalContextLink: Context
    var timeOut = DEFAULT_TIMEOUT
    var maxDistanceValue = DEFAULT_MAX_DISTANCE
    var drawRect = false
    private val boundsPaint = Paint()

    init {
        boundsPaint.color = Color.rgb(0, 0, 0)
        boundsPaint.strokeWidth = 5f
        boundsPaint.style = Paint.Style.STROKE
    }


    constructor(context: Context) : super(context) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(context)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        initialize(context)
    }

    private fun initialize(context: Context) {
        this.internalContextLink = context
    }


    private fun onTouch(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        for (tracker in views) {
            tracker.onMotionEvent(event)
        }
        return false
    }

    private val views: ArrayList<ViewTracker> = ArrayList()

    var consumer: MissClickEventsConsumer? = null
        set(newValue) {
            for (tracker in views) {
                tracker.missClickConsumer = newValue
            }
        }

    fun addTrackedView(view: View) {
        this.views.add(ViewTracker(view, timeOut, this, consumer))
    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return this.onTouch(ev) || super.onInterceptTouchEvent(ev)
    }


    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        if (canvas != null && drawRect) {
            drawViewsBounds(canvas)
        }
    }

    private fun drawViewsBounds(canvas: Canvas) {
        val myRect = Rect()
        getGlobalVisibleRect(myRect)
        for (tracker in views) {
            val viewRect = Rect()
            tracker.view.getGlobalVisibleRect(viewRect)
            val width = viewRect.right - viewRect.left
            val height = viewRect.bottom - viewRect.top
            val distance = fromDpi(maxDistanceValue)
            viewRect.top = Math.max(0, viewRect.top - myRect.top - distance.toInt())
            viewRect.bottom = viewRect.top + height + distance.toInt() * 2
            viewRect.left = Math.max(0, viewRect.left - myRect.left - distance.toInt())
            viewRect.right = viewRect.left + width + distance.toInt() * 2
            canvas.drawRect(viewRect, boundsPaint)
        }
    }

    private fun fromDpi(value: Double): Double = densityDivider * value

    private val densityDivider: Float = context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT.toFloat()
}

/**
 * Обертка над View для отслеживания косаний.
 * Каждое view которое добавлено как отслеживаемое в [TrackingViewGroup] методом [TrackingViewGroup.addTrackedView],
 * оборачивается в ViewTracker.
 * Когда в TrackingViewGroup происходит косание, это косание передается
 * всем имеющимся ViewTracked-ам.
 * View трекер определяет на сколько близко было косание к элементу и было ли нажатие на сам отслеживаемый элемент
 */
class ViewTracker(val view: View,
                  private val trackingTimeout: Long,
                  private val maxDistanceProvider: MaxDistanceProvider,
                  var missClickConsumer: MissClickEventsConsumer?) : Runnable {

    override fun run() {
        closeEvents = clearOldEvents(closeEvents, trackingTimeout, System.currentTimeMillis())
    }

    fun clearOldEvents(list: ArrayList<CloseTouchEvent>, timeout: Long, currentTime: Long): ArrayList<CloseTouchEvent> = ArrayList(list.filter { it.timestamp < currentTime - timeout })

    private val densityDivider: Float = view.context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT.toFloat()

    private var lastAction = MotionEvent.ACTION_CANCEL

    private var closeEvents = ArrayList<CloseTouchEvent>()

    private var lastAddedActionTimestamp: Long = 0L

    private var handler: Handler? = null

    fun onMotionEvent(event: MotionEvent) {
        val isEventOnView = isOnView(view, event)
        val timeStamp = getCurrentTime()
        val currentDistance = getDistanceFromViewInDpi(event)
        if (isEventOnView) {
            if (isClickOnView(event)) {
                if (isHasCloseEvents(timeStamp)) {
                    missClickConsumer?.onConsume(getCurrentTime(), getDistanceFromCenter(view, event), getCloseEvents(timeStamp))
                }
                closeEvents.clear()
            }
            lastAction = event.action
        } else {

            val currentTime = getCurrentTime()
            if (currentDistance <= maxDistanceProvider.getMaxDistance() && currentTime - lastAddedActionTimestamp >= 100L) {
                if (handler == null) {
                    handler = Handler(view.handler.looper)
                }
                handler!!.removeCallbacks(this)
                handler!!.postDelayed(this, trackingTimeout)
                this.lastAddedActionTimestamp = currentTime
                closeEvents.add(CloseTouchEvent(currentTime, getDistanceFromCenter(view, event)))
            }
        }
    }

    private fun getDistanceFromCenter(view: View, event: MotionEvent): Double {
        val viewRect = Rect()
        this.getViewRect(viewRect, view)
        val viewCenter = Point(viewRect.centerX(), viewRect.centerY())
        val eventPoint = Point(getXFromEvent(event), getYFromEvent(event))
        return inDpi(dist(PointF(viewCenter), PointF(eventPoint)))
    }

    private fun getCloseEvents(timeStamp: Long): ArrayList<CloseTouchEvent> =
            ArrayList(closeEvents.filter { timeStamp - it.timestamp < trackingTimeout })

    private fun getDistanceFromViewInDpi(event: MotionEvent): Double {
        val rect = Rect()
        this.getViewRect(rect, view)
        val rectF = RectF(rect)
        val eventPoint = PointF(getXFromEvent(event).toFloat(), getYFromEvent(event).toFloat())
        return inDpi(min(
                dist(eventPoint, PointF(rectF.left, rectF.top), PointF(rectF.left, rectF.bottom)),
                dist(eventPoint, PointF(rectF.right, rectF.top), PointF(rectF.right, rectF.bottom)),
                dist(eventPoint, PointF(rectF.left, rectF.top), PointF(rectF.right, rectF.top)),
                dist(eventPoint, PointF(rectF.left, rectF.bottom), PointF(rectF.right, rectF.bottom))))
    }

    private fun inDpi(value: Double): Double = value / densityDivider.toDouble()

    private fun min(vararg values: Double): Double {
        var minimum: Double? = null
        for (value in values) {
            minimum = if (minimum == null) {
                value
            } else {
                Math.min(minimum, value)
            }
        }
        return minimum ?: 0.0
    }


    private fun dist(event: PointF, lineP1: PointF, lineP2: PointF): Double {
        val xmin = Math.min(lineP1.x, lineP2.x)
        val ymin = Math.min(lineP1.y, lineP2.y)
        val xmax = Math.max(lineP1.x, lineP2.x)
        val ymax = Math.max(lineP1.y, lineP2.y)
        val closest = PointF()
        if (event.x in xmin..xmax) {
            closest.x = event.x
        } else {
            closest.x = if (event.x <= xmin) xmin else xmax
        }
        if (event.y in ymin..ymax) {
            closest.y = event.y
        } else {
            closest.y = if (event.y <= ymin) ymin else ymax
        }
        return dist(event, closest)
    }

    private fun dist(a: PointF, b: PointF): Double {
        return Math.sqrt(Math.pow((a.x - b.x).toDouble(), 2.0) + Math.pow((a.y - b.y).toDouble(), 2.0))
    }


    private fun getCurrentTime(): Long = System.currentTimeMillis()


    private fun isHasCloseEvents(timeStamp: Long): Boolean = closeEvents.isNotEmpty() &&
            closeEvents.find { timeStamp - it.timestamp < trackingTimeout } != null

    private fun isClickOnView(event: MotionEvent): Boolean {
        return (lastAction == MotionEvent.ACTION_DOWN || lastAction == MotionEvent.ACTION_MOVE) && event.action == MotionEvent.ACTION_UP
    }

    private fun isOnView(view: View, event: MotionEvent): Boolean {
        val x = this.getXFromEvent(event)
        val y = this.getYFromEvent(event)
        val rect = Rect()
        this.getViewRect(rect, view)
        return rect.contains(x, y)
    }

    private fun getYFromEvent(event: MotionEvent): Int = event.rawY.toInt()

    private fun getXFromEvent(event: MotionEvent): Int = event.rawX.toInt()

    private fun getViewRect(rect: Rect, viewForRect: View) {
        viewForRect.getGlobalVisibleRect(rect)
    }
}

interface MaxDistanceProvider {
    fun getMaxDistance(): Double
}

interface MissClickEventsConsumer {
    fun onConsume(timestamp: Long, clickDistanceFromCenter: Double, closeEvents: ArrayList<CloseTouchEvent>)
}

data class CloseTouchEvent(val timestamp: Long,
                           val distanceFromCenter: Double)


