package cn.intret.app.picgo.screens.main

import android.animation.Animator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.transition.Transition
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.ViewGroup

import com.davemorrissey.labs.subscaleview.ImageViewState
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

import cn.intret.app.picgo.R

/**
 * While changing size of [com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView], change scale and center for smooth transition.
 *
 * This transition must be postponed, because it will fail if image is not loaded.
 */
class SubsamplingScaleImageViewSharedTransition : Transition {

    private var subsamplingScaleType: Int = 0
    private var imageViewScaleType: Int = 0
    private var direction: Int = 0

    /**
     * For manual creation through code. Initializes transition with all default values and no specified targets.
     */
    constructor() {
        imageViewScaleType = 0
        direction = 0
        subsamplingScaleType = 0
    }

    /**
     * Created from XML, targets [SubsamplingScaleImageView] class.
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SubsamplingScaleImageViewSharedTransition)
        direction = a.getInt(R.styleable.SubsamplingScaleImageViewSharedTransition_transitionDirection, 0)
        subsamplingScaleType = a.getInt(R.styleable.SubsamplingScaleImageViewSharedTransition_subsamplingImageViewScaleType, FIT_CENTER)
        imageViewScaleType = a.getInt(R.styleable.SubsamplingScaleImageViewSharedTransition_imageViewScaleType, FIT_CENTER)
        a.recycle()
        addTarget(SubsamplingScaleImageView::class.java)
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        if (transitionValues.view is SubsamplingScaleImageView) {
            val size = Point(transitionValues.view.width, transitionValues.view.height)
            transitionValues.values[PROPNAME_SIZE] = size
            val ssiv = transitionValues.view as SubsamplingScaleImageView
            transitionValues.values[PROPNAME_STATE] = ssiv.state
        }
    }

    override fun createAnimator(sceneRoot: ViewGroup, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val subsamplingState = startValues.values[PROPNAME_STATE] as ImageViewState
        val startSize = startValues.values[PROPNAME_SIZE] as Point
        val endSize = endValues.values[PROPNAME_SIZE] as Point
        if (startSize == null || endSize == null || subsamplingState == null || startSize == endSize) {
            return null//missing some values, don't animate
        }
        val view = startValues.view as SubsamplingScaleImageView
        val imgSize = Point(view.sWidth, view.sHeight)
        if (imgSize.x == 0 || imgSize.y == 0) {
            return null //no image size, skip animation.
        }

        //resolve transition direction (enter or leaving), assumes enter if view gets larger
        val isEntering = if (direction == 0) startSize.x < endSize.x || startSize.y < endSize.y else direction == 1

        val centerFrom: PointF
        val scaleFrom: Float
        val scaleTo: Float
        val valueAnimator: ValueAnimator
        val centerTo = PointF((imgSize.x / 2).toFloat(), (imgSize.y / 2).toFloat())

        if (isEntering) {
            centerFrom = PointF((imgSize.x / 2).toFloat(), (imgSize.y / 2).toFloat())
            scaleFrom = getMinIfTrue(startSize.x / imgSize.x.toFloat(), startSize.y / imgSize.y.toFloat(),
                    imageViewScaleType == FIT_CENTER)
            scaleTo = getMinIfTrue(imgSize.x / endSize.x.toFloat(), imgSize.y / endSize.y.toFloat(),
                    subsamplingScaleType == FIT_CENTER)
        } else {
            centerFrom = subsamplingState.center
            scaleFrom = subsamplingState.scale
            scaleTo = getMinIfTrue(endSize.x / imgSize.x.toFloat(), endSize.y / imgSize.y.toFloat(),
                    imageViewScaleType == FIT_CENTER)

        }

        val prop_scale = PropertyValuesHolder.ofFloat(PROPNAME_SIZE, scaleFrom, scaleTo)
        val prop_center_x = PropertyValuesHolder.ofFloat(PROPNAME_CENTER_X, centerFrom.x, centerTo.x)
        val prop_center_y = PropertyValuesHolder.ofFloat(PROPNAME_CENTER_Y, centerFrom.y, centerTo.y)

        valueAnimator = ValueAnimator.ofPropertyValuesHolder(prop_scale, prop_center_x, prop_center_y)

        valueAnimator.addUpdateListener { animation ->
            val newCenter = PointF(animation.getAnimatedValue(PROPNAME_CENTER_X) as Float, animation.getAnimatedValue(PROPNAME_CENTER_Y) as Float)
            view.setScaleAndCenter(animation.getAnimatedValue(PROPNAME_SIZE) as Float, newCenter)
        }
        return valueAnimator
    }

    /**
     * Does [Math.min] or [Math.max] depending on boolean.
     *
     * @param val1 value to compare
     * @param val2 value to compare
     * @param con  condition to check
     * @return If `con` return minimum of 2 values, otherwise return max.
     */
    private fun getMinIfTrue(val1: Float, val2: Float, con: Boolean): Float {
        return if (con) Math.min(val1, val2) else Math.max(val1, val2)
    }

    fun setSubsamplingScaleType(subsamplingScaleType: Int): SubsamplingScaleImageViewSharedTransition {
        this.subsamplingScaleType = subsamplingScaleType
        return this
    }

    fun setImageViewScaleType(imageViewScaleType: Int): SubsamplingScaleImageViewSharedTransition {
        this.imageViewScaleType = imageViewScaleType
        return this
    }

    fun setDirection(direction: Int): SubsamplingScaleImageViewSharedTransition {
        this.direction = direction
        return this
    }

    companion object {
        private val PROPNAME_STATE = "com.example:transition:state"
        private val PROPNAME_SIZE = "com.example:transition:size"
        private val PROPNAME_CENTER_X = "com.example:transition:center_x"
        private val PROPNAME_CENTER_Y = "com.example:transition:center_y"

        private val FIT_CENTER = 0
        private val CENTER_CROP = 1
    }
}