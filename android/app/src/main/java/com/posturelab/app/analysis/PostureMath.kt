package com.posturelab.app.analysis

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Posture analysis math. Numeric parity with `shared/posture_math.py`.
 * Indices use the MediaPipe / ML Kit Pose 33-keypoint convention.
 */
object PostureMath {
    const val NOSE = 0
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28

    data class Point(val x: Float, val y: Float, val visibility: Float = 1f)

    fun midpoint(a: Point, b: Point) =
        Point((a.x + b.x) / 2f, (a.y + b.y) / 2f, minOf(a.visibility, b.visibility))

    /** Tilt from horizontal in (-90, 90]; positive = b higher than a (smaller image-y). */
    fun tiltDeg(a: Point, b: Point): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        if (abs(dx) < 1e-6f) return 0f
        return Math.toDegrees(atan2((-dy).toDouble(), abs(dx).toDouble())).toFloat()
    }

    /** Absolute angle between vector a->b and the vertical axis (deg). */
    fun angleFromVertical(a: Point, b: Point): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return abs(Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat())
    }

    fun pixelsPerInch(lms: List<Point>, heightInches: Float): Float {
        val head = midpoint(lms[LEFT_EAR], lms[RIGHT_EAR])
        val ank = midpoint(lms[LEFT_ANKLE], lms[RIGHT_ANKLE])
        val px = hypot((head.x - ank.x).toDouble(), (head.y - ank.y).toDouble()).toFloat()
        val inches = (heightInches * 0.92f).coerceAtLeast(1e-3f)
        return px / inches
    }

    private val hansraj = listOf(0f to 13f, 15f to 27f, 30f to 40f, 45f to 49f, 60f to 60f)

    fun effectiveHeadWeightLb(forwardTiltDeg: Float): Float {
        val a = abs(forwardTiltDeg)
        if (a <= hansraj.first().first) return hansraj.first().second
        if (a >= hansraj.last().first) return hansraj.last().second
        for (i in 0 until hansraj.size - 1) {
            val (x0, y0) = hansraj[i]
            val (x1, y1) = hansraj[i + 1]
            if (a in x0..x1) {
                val t = (a - x0) / (x1 - x0)
                return y0 + t * (y1 - y0)
            }
        }
        return 13f
    }

    data class FrontMetrics(
        val headShiftIn: Float, val headTiltDeg: Float,
        val shouldersShiftIn: Float, val shouldersTiltDeg: Float,
        val ribcageShiftIn: Float,
        val hipsShiftIn: Float, val hipsTiltDeg: Float,
        val qAngleLeftDeg: Float, val qAngleRightDeg: Float,
        val totalShiftIn: Float, val totalTiltDeg: Float,
    )

    data class SideMetrics(
        val headForwardIn: Float, val headOffVerticalDeg: Float,
        val baseHeadWeightLb: Float, val effectiveHeadWeightLb: Float,
        val shoulderShiftIn: Float, val shoulderOffVerticalDeg: Float,
        val hipShiftIn: Float, val hipOffVerticalDeg: Float,
        val kneeShiftIn: Float,
        val headVsAnkleIn: Float, val headVsAnkleFlexDeg: Float,
        val totalShiftIn: Float, val totalTiltDeg: Float,
    )

    fun analyzeFront(lms: List<Point>, heightInches: Float): FrontMetrics {
        val ppi = pixelsPerInch(lms, heightInches)
        val ear = midpoint(lms[LEFT_EAR], lms[RIGHT_EAR])
        val sh = midpoint(lms[LEFT_SHOULDER], lms[RIGHT_SHOULDER])
        val hip = midpoint(lms[LEFT_HIP], lms[RIGHT_HIP])
        val ank = midpoint(lms[LEFT_ANKLE], lms[RIGHT_ANKLE])
        val torso = midpoint(sh, hip)

        // Patient-right is image-LEFT; report-right = positive.
        fun shiftIn(top: Point, base: Point) = -(top.x - base.x) / ppi

        val headTilt = -tiltDeg(lms[LEFT_EAR], lms[RIGHT_EAR])
        val shTilt = -tiltDeg(lms[LEFT_SHOULDER], lms[RIGHT_SHOULDER])
        val hipsTilt = -tiltDeg(lms[LEFT_HIP], lms[RIGHT_HIP])

        fun qAngle(hipP: Point, kneeP: Point, ankleP: Point): Float {
            val vx = ankleP.x - hipP.x; val vy = ankleP.y - hipP.y
            val L = hypot(vx.toDouble(), vy.toDouble()).toFloat().coerceAtLeast(1f)
            val d = abs((kneeP.x - hipP.x) * vy - (kneeP.y - hipP.y) * vx) / L
            return Math.toDegrees(atan2(d.toDouble(), (L / 2.0).coerceAtLeast(1.0))).toFloat()
        }

        val qL = qAngle(lms[LEFT_HIP], lms[LEFT_KNEE], lms[LEFT_ANKLE])
        val qR = qAngle(lms[RIGHT_HIP], lms[RIGHT_KNEE], lms[RIGHT_ANKLE])

        val headShift = shiftIn(ear, ank)
        val shShift = shiftIn(sh, ank)
        val ribShift = shiftIn(torso, ank)
        val hipsShift = shiftIn(hip, ank)

        return FrontMetrics(
            headShiftIn = headShift, headTiltDeg = headTilt,
            shouldersShiftIn = shShift, shouldersTiltDeg = shTilt,
            ribcageShiftIn = ribShift,
            hipsShiftIn = hipsShift, hipsTiltDeg = hipsTilt,
            qAngleLeftDeg = qL, qAngleRightDeg = qR,
            totalShiftIn = abs(headShift) + abs(shShift) + abs(ribShift) + abs(hipsShift),
            totalTiltDeg = abs(headTilt) + abs(shTilt) + abs(hipsTilt),
        )
    }

    fun analyzeSide(lms: List<Point>, heightInches: Float, baseHeadWeightLb: Float = 11f): SideMetrics {
        val ppi = pixelsPerInch(lms, heightInches)

        val leftScore = lms[LEFT_EAR].visibility + lms[LEFT_SHOULDER].visibility +
                        lms[LEFT_HIP].visibility + lms[LEFT_ANKLE].visibility
        val rightScore = lms[RIGHT_EAR].visibility + lms[RIGHT_SHOULDER].visibility +
                         lms[RIGHT_HIP].visibility + lms[RIGHT_ANKLE].visibility
        val (e, s, h, k, a) = if (rightScore >= leftScore)
            Quintet(RIGHT_EAR, RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE)
        else Quintet(LEFT_EAR, LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE, LEFT_ANKLE)
        val ear = lms[e]; val sh = lms[s]; val hip = lms[h]; val knee = lms[k]; val ank = lms[a]

        val facingRight = lms[NOSE].x >= sh.x
        val sgn = if (facingRight) 1f else -1f

        fun fwdIn(p: Point, base: Point) = sgn * (p.x - base.x) / ppi

        val headForward = fwdIn(ear, ank)
        val headOff = angleFromVertical(sh, ear)
        val shShift = fwdIn(sh, ank)
        val shOff = angleFromVertical(hip, sh)
        val hipShift = fwdIn(hip, ank)
        val hipOff = angleFromVertical(ank, hip)
        val kneeShift = fwdIn(knee, ank)
        val headVsAnkle = fwdIn(ear, ank)
        val headVsAnkleFlex = angleFromVertical(ank, ear)

        val effW = baseHeadWeightLb / 13f * effectiveHeadWeightLb(headOff)

        return SideMetrics(
            headForwardIn = headForward,
            headOffVerticalDeg = headOff,
            baseHeadWeightLb = baseHeadWeightLb,
            effectiveHeadWeightLb = effW,
            shoulderShiftIn = -shShift,
            shoulderOffVerticalDeg = shOff,
            hipShiftIn = hipShift,
            hipOffVerticalDeg = hipOff,
            kneeShiftIn = kneeShift,
            headVsAnkleIn = headVsAnkle,
            headVsAnkleFlexDeg = headVsAnkleFlex,
            totalShiftIn = abs(headForward) + abs(shShift) + abs(hipShift) + abs(kneeShift),
            totalTiltDeg = headOff + shOff + hipOff,
        )
    }

    private data class Quintet(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int)
}
