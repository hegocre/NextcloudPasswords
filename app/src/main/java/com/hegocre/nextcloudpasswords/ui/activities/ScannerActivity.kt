package com.hegocre.nextcloudpasswords.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hegocre.nextcloudpasswords.R
import com.hegocre.nextcloudpasswords.ui.theme.NextcloudPasswordsTheme
import zxingcpp.BarcodeReader

class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextcloudPasswordsTheme {
                ScannerScreen(
                    onScanned = { scannedValue ->
                        val resultIntent = Intent().putExtra("SCAN_RESULT", scannedValue)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancelled = { exception ->
                        val exceptionIntent = Intent().putExtra("EXCEPTION", exception)
                        setResult(RESULT_CANCELED, exceptionIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ScannerScreen(
    onScanned: (String) -> Unit,
    onCancelled: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val noCameraPermissionMessage = stringResource(R.string.error_no_camera_permission)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onCancelled(noCameraPermissionMessage)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var lastScannedValue by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            val cameraController = remember {
                LifecycleCameraController(context).apply {
                    setEnabledUseCases(androidx.camera.view.CameraController.IMAGE_ANALYSIS)
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                }
            }

            val barcodeReader = remember { BarcodeReader(
                options = BarcodeReader.Options(
                    formats = setOf(BarcodeReader.Format.QR_CODE)
                )
            ) }

            DisposableEffect(Unit) {
                val executor = ContextCompat.getMainExecutor(context)
                cameraController.setImageAnalysisAnalyzer(executor) { imageProxy: ImageProxy ->
                    imageProxy.use {
                        val cropSize = minOf(it.width, it.height) * 8 / 10 // matches on-screen box proportionally
                        val cropRect = android.graphics.Rect(
                            (it.width - cropSize) / 2,
                            (it.height - cropSize) / 2,
                            (it.width + cropSize) / 2,
                            (it.height + cropSize) / 2
                        )
                        it.setCropRect(cropRect)

                        val results = barcodeReader.read(it)
                        val value = results.firstOrNull()?.text
                        if (value != null && value != lastScannedValue) {
                            lastScannedValue = value
                            onScanned(value)
                        }
                    }
                }
                onDispose { cameraController.clearImageAnalysisAnalyzer() }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            controller = cameraController
                            cameraController.bindToLifecycle(lifecycleOwner)
                        }
                    }
                )

                val windowSize = LocalWindowInfo.current.containerDpSize

                ScannerOverlay(boxSize = (min(windowSize.width, windowSize.height).value * 0.8f).dp)
            }
        }
    }
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier, boxSize: Dp = 250.dp) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val boxSizePx = boxSize.toPx()
        val left = (size.width - boxSizePx) / 2f
        val top = (size.height - boxSizePx) / 2f

        Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
        }
        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(left, top, left + boxSizePx, top + boxSizePx),
                    CornerRadius(16.dp.toPx())
                )
            )
        }
        clipPath(cutoutPath, clipOp = ClipOp.Difference) {
            drawRect(color = Color.Black.copy(alpha = 0.5f))
        }

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(boxSizePx, boxSizePx),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}