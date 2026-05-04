package com.posturelab.app.report

import com.posturelab.app.analysis.PostureMath.FrontMetrics
import com.posturelab.app.analysis.PostureMath.SideMetrics
import kotlin.math.abs

object PostureReportText {
    const val INTRO =
        "Good posture is simple and elegant by design in form and function. The body is designed to have the head, " +
        "rib cage, and pelvis perfectly balanced upon one another in both the front and side views. If the posture " +
        "is deviated from normal, then the spine is also deviated from the normal healthy position. Abnormal posture " +
        "has been associated with the development and progression of many spinal conditions and injuries including: " +
        "increased muscle activity and disc injury, scoliosis, work lifting injuries, sports injuries, back pain, " +
        "neck pain, headaches, carpal tunnel symptoms, shoulder and ankle injuries, and many other conditions."

    private fun shift(v: Float, posLabel: String, negLabel: String, threshold: Float = 0.05f): String {
        return if (abs(v) < threshold) "not shifted significantly"
        else "shifted ${"%.2f".format(abs(v))} in ${if (v > 0) posLabel else negLabel}"
    }

    private fun tilt(v: Float): String {
        return if (abs(v) < 0.5f) "not tilted"
        else "tilted ${"%.1f".format(abs(v))}° ${if (v > 0) "right" else "left"}"
    }

    fun headLine(f: FrontMetrics) =
        "Head is ${shift(f.headShiftIn, "right", "left")}. Head is ${tilt(f.headTiltDeg)}."
    fun shouldersLine(f: FrontMetrics) =
        "Shoulders are ${shift(f.shouldersShiftIn, "right", "left")}. Shoulders are ${tilt(f.shouldersTiltDeg)}."
    fun ribLine(f: FrontMetrics) = "Ribcage is ${shift(f.ribcageShiftIn, "right", "left")}."
    fun hipsLine(f: FrontMetrics) =
        "Hips are ${shift(f.hipsShiftIn, "right", "left")}. Hips are ${tilt(f.hipsTiltDeg)}."
    fun qLine(f: FrontMetrics) =
        "The Right Q-Angle is ${"%.1f".format(f.qAngleRightDeg)}°. The Left Q-Angle is ${"%.1f".format(f.qAngleLeftDeg)}°."

    fun headSide(s: SideMetrics) =
        "Head is ${shift(s.headForwardIn, "forward", "backward")}, ${"%.1f".format(s.headOffVerticalDeg)}° off vertical."
    fun headWeightSide(s: SideMetrics) =
        "Based on physics, your head now weighs ${"%.0f".format(s.effectiveHeadWeightLb)} lbs instead of ${"%.0f".format(s.baseHeadWeightLb)} lbs."
    fun shouldersSide(s: SideMetrics) =
        "Shoulders are ${shift(s.shoulderShiftIn, "backward", "forward")}, ${"%.1f".format(s.shoulderOffVerticalDeg)}° off vertical."
    fun hipsSide(s: SideMetrics) =
        "Hips are ${shift(s.hipShiftIn, "forward", "backward")}, ${"%.1f".format(s.hipOffVerticalDeg)}° off vertical."
    fun kneesSide(s: SideMetrics) =
        "Knees are ${shift(s.kneeShiftIn, "forward", "backward")}."
    fun headVsAnkle(s: SideMetrics) =
        "Head vs. Ankle alignment is ${"%.2f".format(abs(s.headVsAnkleIn))} in ${if (s.headVsAnkleIn >= 0) "forward" else "backward"}, ${"%.1f".format(s.headVsAnkleFlexDeg)}° flexed."
}
