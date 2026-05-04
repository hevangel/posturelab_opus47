package com.posturelab.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.posturelab.app.analysis.PoseAnalyzer
import com.posturelab.app.analysis.PostureMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val analyzer = PoseAnalyzer()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setFront(bitmap: Bitmap?) = _state.update { it.copy(frontBitmap = bitmap) }
    fun setSide(bitmap: Bitmap?) = _state.update { it.copy(sideBitmap = bitmap) }
    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setDate(v: String) = _state.update { it.copy(date = v) }
    fun setHeight(v: Float) = _state.update { it.copy(heightIn = v) }
    fun setWeight(v: Float) = _state.update { it.copy(weightLb = v) }
    fun setHeadWeight(v: Float) = _state.update { it.copy(baseHeadWeightLb = v) }

    fun loadFromUri(uri: Uri, isFront: Boolean) {
        viewModelScope.launch {
            val bm = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            } ?: return@launch
            if (isFront) setFront(bm) else setSide(bm)
        }
    }

    fun analyze(onDone: () -> Unit, onError: (String) -> Unit) {
        val s = _state.value
        val front = s.frontBitmap ?: return onError("Front photo missing")
        val side = s.sideBitmap ?: return onError("Side photo missing")
        viewModelScope.launch {
            try {
                _state.update { it.copy(busy = true) }
                val frontLms = analyzer.detect(front)
                val sideLms = analyzer.detect(side)
                val frontAnno = analyzer.annotate(front, frontLms)
                val sideAnno = analyzer.annotate(side, sideLms)
                val frontMetrics = PostureMath.analyzeFront(frontLms, s.heightIn)
                val sideMetrics = PostureMath.analyzeSide(sideLms, s.heightIn, s.baseHeadWeightLb)
                _state.update {
                    it.copy(
                        busy = false,
                        frontAnnotated = frontAnno,
                        sideAnnotated = sideAnno,
                        front = frontMetrics,
                        side = sideMetrics,
                    )
                }
                onDone()
            } catch (e: Throwable) {
                _state.update { it.copy(busy = false) }
                onError(e.message ?: "Analysis failed")
            }
        }
    }
}

private inline fun MutableStateFlow<UiState>.update(transform: (UiState) -> UiState) {
    value = transform(value)
}

data class UiState(
    val name: String = "Patient",
    val date: String = java.time.LocalDate.now().toString(),
    val heightIn: Float = 68f,
    val weightLb: Float = 160f,
    val baseHeadWeightLb: Float = 11f,
    val frontBitmap: Bitmap? = null,
    val sideBitmap: Bitmap? = null,
    val frontAnnotated: Bitmap? = null,
    val sideAnnotated: Bitmap? = null,
    val front: PostureMath.FrontMetrics? = null,
    val side: PostureMath.SideMetrics? = null,
    val busy: Boolean = false,
)
