package com.posturelab.app.analysis

import com.posturelab.app.analysis.PostureMath.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PostureMathTest {

    private fun normalFront(): List<Point> {
        val cx = 500f
        val pts = MutableList(33) { Point(0f, 0f) }
        pts[7]  = Point(cx + 30, 100f)
        pts[8]  = Point(cx - 30, 100f)
        pts[11] = Point(cx + 80, 250f)
        pts[12] = Point(cx - 80, 250f)
        pts[23] = Point(cx + 60, 600f)
        pts[24] = Point(cx - 60, 600f)
        pts[25] = Point(cx + 60, 900f)
        pts[26] = Point(cx - 60, 900f)
        pts[27] = Point(cx + 60, 1200f)
        pts[28] = Point(cx - 60, 1200f)
        pts[0]  = Point(cx, 90f)
        return pts
    }

    @Test fun normalPostureFrontIsClose() {
        val f = PostureMath.analyzeFront(normalFront(), 68f)
        assertTrue(abs(f.headShiftIn) < 0.05f)
        assertTrue(abs(f.headTiltDeg) < 0.5f)
        assertTrue(abs(f.shouldersTiltDeg) < 0.5f)
        assertTrue(abs(f.hipsShiftIn) < 0.05f)
    }

    @Test fun headShiftedToPatientRightReadsPositive() {
        val pts = normalFront().toMutableList()
        pts[7] = Point(pts[7].x - 50, pts[7].y)
        pts[8] = Point(pts[8].x - 50, pts[8].y)
        val f = PostureMath.analyzeFront(pts, 68f)
        assertTrue("expected positive headShiftIn, got ${f.headShiftIn}", f.headShiftIn > 0.5f)
    }

    @Test fun hansrajCurveStartsAt13AndIsMonotonic() {
        assertEquals(13f, PostureMath.effectiveHeadWeightLb(0f), 1e-3f)
        var prev = 13f
        for (d in 5..60 step 5) {
            val v = PostureMath.effectiveHeadWeightLb(d.toFloat())
            assertTrue(v >= prev - 1e-3f)
            prev = v
        }
    }

    @Test fun forwardHeadIncreasesEffectiveWeight() {
        val pts = MutableList(33) { Point(500f, 0f, 0.9f) }
        // simulate side view
        pts[8]  = Point(580f, 100f, 0.9f)   // ear forward
        pts[12] = Point(500f, 250f, 0.9f)
        pts[24] = Point(500f, 600f, 0.9f)
        pts[26] = Point(500f, 900f, 0.9f)
        pts[28] = Point(500f, 1200f, 0.9f)
        pts[7]  = Point(500f, 100f, 0.1f)
        pts[11] = Point(500f, 250f, 0.1f)
        pts[23] = Point(500f, 600f, 0.1f)
        pts[25] = Point(500f, 900f, 0.1f)
        pts[27] = Point(500f, 1200f, 0.1f)
        pts[0]  = Point(640f, 100f)
        val s = PostureMath.analyzeSide(pts, 68f)
        assertTrue(s.headOffVerticalDeg > 5f)
        assertTrue(s.effectiveHeadWeightLb > s.baseHeadWeightLb)
    }
}
