package com.example.mmplayer.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class VideoGestureDetector(
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onLongPressUp: () -> Unit,
    private val onHorizontalScroll: (distanceX: Float) -> Unit,
    private val onVerticalScrollLeft: (distanceY: Float) -> Unit,
    private val onVerticalScrollRight: (distanceY: Float) -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private var isLongPressing = false
    private var initialX = 0f
    private var initialY = 0f
    private var screenWidth = 0f
    private var isScrolling = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (!isScrolling) {
            onSingleTap()
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        onDoubleTap()
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        isLongPressing = true
        onLongPress()
    }

    override fun onDown(e: MotionEvent): Boolean {
        initialX = e.x
        initialY = e.y
        isScrolling = false
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (e1 == null) return false
        
        isScrolling = true
        
        val deltaX = abs(e2.x - e1.x)
        val deltaY = abs(e2.y - e1.y)
        
        // 确定是水平滑动还是垂直滑动
        if (deltaX > deltaY) {
            // 水平滑动 - 进度控制
            onHorizontalScroll(distanceX)
        } else {
            // 垂直滑动 - 亮度/音量控制
            if (screenWidth == 0f) {
                screenWidth = e2.device?.getMotionRange(MotionEvent.AXIS_X)?.max ?: 1080f
            }
            
            val isLeftSide = e1.x < screenWidth / 2
            if (isLeftSide) {
                // 左侧滑动控制亮度
                onVerticalScrollLeft(distanceY)
            } else {
                // 右侧滑动控制音量
                onVerticalScrollRight(distanceY)
            }
        }
        
        return true
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isLongPressing) {
                    isLongPressing = false
                    onLongPressUp()
                }
                isScrolling = false
            }
        }
        return false
    }
}