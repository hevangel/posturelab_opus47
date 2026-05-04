package com.posturelab.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(vm: AppViewModel, onAnalyzed: () -> Unit) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var cameraTarget by remember { mutableStateOf<CameraTarget?>(null) }

    if (cameraTarget != null) {
        CameraCaptureSheet(
            onCapture = { bm ->
                if (cameraTarget == CameraTarget.FRONT) vm.setFront(bm) else vm.setSide(bm)
                cameraTarget = null
            },
            onDismiss = { cameraTarget = null },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogo(28)
                        Spacer(Modifier.width(8.dp))
                        Text("PostureLab", fontWeight = FontWeight.Bold, color = Brand.OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Brand.Primary),
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Patient Info", color = Brand.Primary, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(state.name, vm::setName, label = { Text("Name") }, modifier = Modifier.weight(1f))
                OutlinedTextField(state.date, vm::setDate, label = { Text("Date") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Height (in)", state.heightIn, vm::setHeight, Modifier.weight(1f))
                NumberField("Weight (lb)", state.weightLb, vm::setWeight, Modifier.weight(1f))
                NumberField("Head wt (lb)", state.baseHeadWeightLb, vm::setHeadWeight, Modifier.weight(1f))
            }

            PhotoCard(
                "Front view photo",
                bitmap = state.frontBitmap,
                onCamera = { cameraTarget = CameraTarget.FRONT },
                onPick = { uri -> vm.loadFromUri(uri, isFront = true) },
            )
            PhotoCard(
                "Side view photo",
                bitmap = state.sideBitmap,
                onCamera = { cameraTarget = CameraTarget.SIDE },
                onPick = { uri -> vm.loadFromUri(uri, isFront = false) },
            )

            error?.let {
                Text(it, color = androidx.compose.ui.graphics.Color.Red)
            }

            Button(
                onClick = {
                    error = null
                    vm.analyze(onDone = onAnalyzed, onError = { error = it })
                },
                enabled = !state.busy && state.frontBitmap != null && state.sideBitmap != null,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.busy) "Analyzing…" else "Analyze posture")
            }
        }
    }
}

private enum class CameraTarget { FRONT, SIDE }

@Composable
private fun NumberField(label: String, value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toFloatOrNull()?.let(onChange) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
    )
}

@Composable
private fun PhotoCard(
    label: String,
    bitmap: Bitmap?,
    onCamera: () -> Unit,
    onPick: (Uri) -> Unit,
) {
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPick(uri)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Brand.Primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFFEEF2F7)),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = label, modifier = Modifier.fillMaxSize())
                } else {
                    Text("No image", color = androidx.compose.ui.graphics.Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCamera) { Text("Camera") }
                OutlinedButton(onClick = {
                    pickLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) { Text("Gallery") }
            }
        }
    }
}

@Composable
private fun CameraCaptureSheet(onCapture: (Bitmap) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted) permLauncher.launch(Manifest.permission.CAMERA)
    }
    val imageCapture = remember { ImageCapture.Builder().build() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Capture photo", color = Brand.Primary) },
        text = {
            if (!permissionGranted) {
                Text("Camera permission is required to capture posture photos.")
            } else {
                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context)
                        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                            ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val selector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                            } catch (_: Throwable) {}
                        }, ContextCompat.getMainExecutor(context))
                        previewView
                    },
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    capture(ctx, imageCapture) { bm ->
                        if (bm != null) onCapture(bm) else onDismiss()
                    }
                },
                enabled = permissionGranted,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Primary),
            ) { Text("Capture") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun capture(ctx: Context, imageCapture: ImageCapture, cb: (Bitmap?) -> Unit) {
    val file = File.createTempFile("posturelab_", ".jpg", ctx.cacheDir)
    val opts = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(opts, ContextCompat.getMainExecutor(ctx),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bm = BitmapFactory.decodeFile(file.absolutePath)
                cb(bm)
            }
            override fun onError(exc: ImageCaptureException) { cb(null) }
        })
}
