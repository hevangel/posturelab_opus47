package com.posturelab.app.analysis

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Wraps ML Kit Pose Detection. Maps the 33 ML-Kit landmark IDs to MediaPipe-compatible indices. */
class PoseAnalyzer {
    private val detector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
    )

    /** ML Kit landmark types correspond 1:1 to MediaPipe pose indices 0-32 (same enum order). */
    private val mlkitToMpIndex = mapOf(
        PoseLandmark.NOSE to 0,
        PoseLandmark.LEFT_EYE_INNER to 1,
        PoseLandmark.LEFT_EYE to 2,
        PoseLandmark.LEFT_EYE_OUTER to 3,
        PoseLandmark.RIGHT_EYE_INNER to 4,
        PoseLandmark.RIGHT_EYE to 5,
        PoseLandmark.RIGHT_EYE_OUTER to 6,
        PoseLandmark.LEFT_EAR to 7,
        PoseLandmark.RIGHT_EAR to 8,
        PoseLandmark.LEFT_MOUTH to 9,
        PoseLandmark.RIGHT_MOUTH to 10,
        PoseLandmark.LEFT_SHOULDER to 11,
        PoseLandmark.RIGHT_SHOULDER to 12,
        PoseLandmark.LEFT_ELBOW to 13,
        PoseLandmark.RIGHT_ELBOW to 14,
        PoseLandmark.LEFT_WRIST to 15,
        PoseLandmark.RIGHT_WRIST to 16,
        PoseLandmark.LEFT_PINKY to 17,
        PoseLandmark.RIGHT_PINKY to 18,
        PoseLandmark.LEFT_INDEX to 19,
        PoseLandmark.RIGHT_INDEX to 20,
        PoseLandmark.LEFT_THUMB to 21,
        PoseLandmark.RIGHT_THUMB to 22,
        PoseLandmark.LEFT_HIP to 23,
        PoseLandmark.RIGHT_HIP to 24,
        PoseLandmark.LEFT_KNEE to 25,
        PoseLandmark.RIGHT_KNEE to 26,
        PoseLandmark.LEFT_ANKLE to 27,
        PoseLandmark.RIGHT_ANKLE to 28,
        PoseLandmark.LEFT_HEEL to 29,
        PoseLandmark.RIGHT_HEEL to 30,
        PoseLandmark.LEFT_FOOT_INDEX to 31,
        PoseLandmark.RIGHT_FOOT_INDEX to 32,
    )

    suspend fun detect(bitmap: Bitmap): List<PostureMath.Point> {
        val img = InputImage.fromBitmap(bitmap, 0)
        val pose = suspendCancellableCoroutine<com.google.mlkit.vision.pose.Pose> { cont ->
            detector.process(img)
                .addOnSuccessListener { p -> cont.resume(p) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) error("No person detected in image")
        val arr = MutableList(33) { PostureMath.Point(0f, 0f, 0f) }
        for (lm in landmarks) {
            val idx = mlkitToMpIndex[lm.landmarkType] ?: continue
            arr[idx] = PostureMath.Point(lm.position.x, lm.position.y, lm.inFrameLikelihood)
        }
        return arr
    }

    fun annotate(bitmap: Bitmap, lms: List<PostureMath.Point>): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val w = out.width.toFloat(); val h = out.height.toFloat()

        val ear = PostureMath.midpoint(lms[PostureMath.LEFT_EAR], lms[PostureMath.RIGHT_EAR])
        val sh = PostureMath.midpoint(lms[PostureMath.LEFT_SHOULDER], lms[PostureMath.RIGHT_SHOULDER])
        val hip = PostureMath.midpoint(lms[PostureMath.LEFT_HIP], lms[PostureMath.RIGHT_HIP])
        val ank = PostureMath.midpoint(lms[PostureMath.LEFT_ANKLE], lms[PostureMath.RIGHT_ANKLE])

        // vertical reference (green)
        val vp = Paint().apply { color = AColor.rgb(40, 200, 80); strokeWidth = 4f; isAntiAlias = true }
        c.drawLine(ank.x, 0f, ank.x, h, vp)

        // body chain (red)
        val rp = Paint().apply { color = AColor.rgb(220, 60, 60); strokeWidth = 6f; isAntiAlias = true }
        val chain = listOf(ear, sh, hip, ank)
        for (i in 0 until chain.size - 1) {
            c.drawLine(chain[i].x, chain[i].y, chain[i + 1].x, chain[i + 1].y, rp)
        }

        // horizontal rails (blue)
        val bp = Paint().apply { color = AColor.rgb(60, 120, 220); strokeWidth = 4f; isAntiAlias = true }
        val pairs = listOf(
            PostureMath.LEFT_SHOULDER to PostureMath.RIGHT_SHOULDER,
            PostureMath.LEFT_HIP to PostureMath.RIGHT_HIP,
            PostureMath.LEFT_EAR to PostureMath.RIGHT_EAR,
            PostureMath.LEFT_KNEE to PostureMath.RIGHT_KNEE,
        )
        for ((l, r) in pairs) {
            c.drawLine(lms[l].x, lms[l].y, lms[r].x, lms[r].y, bp)
        }

        // landmark dots (yellow)
        val dp = Paint().apply { color = AColor.rgb(255, 220, 40); style = Paint.Style.FILL; isAntiAlias = true }
        for (p in listOf(ear, sh, hip, ank, lms[PostureMath.LEFT_KNEE], lms[PostureMath.RIGHT_KNEE])) {
            c.drawCircle(p.x, p.y, 8f, dp)
        }
        return out
    }
}
