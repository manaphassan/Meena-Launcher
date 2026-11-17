package my.hana.meena.ui.hubs

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import my.hana.meena.R
import my.hana.meena.data.system.ScanEntry
import my.hana.meena.data.system.ScannerRepository
import my.hana.meena.theme.MeenaScrimDim
import my.hana.meena.theme.MeenaScrimHeavy
import my.hana.meena.theme.MeenaScrimSolid
import my.hana.meena.theme.MeenaSpacing
import my.hana.meena.theme.MeenaSurface
import my.hana.meena.theme.MeenaTextMuted
import my.hana.meena.theme.MeenaTextWhite
import my.hana.meena.ui.components.metroClickable
import java.util.concurrent.Executors

@Composable
fun ScannerSubPage(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeenaSurface)
            .statusBarsPadding()
    ) {
        // App bar
        ScannerAppBar(
            title = stringResource(R.string.scanner_title),
            onNavigateBack = onNavigateBack
        )

        if (hasPermission) {
            ScannerCamera(onNavigateBack = onNavigateBack)
        } else {
            // Permission request
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MeenaSpacing.l),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.scanner_needs_camera),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
                Spacer(modifier = Modifier.height(MeenaSpacing.l))
                Text(
                    text = stringResource(R.string.scanner_grant_camera),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        .padding(MeenaSpacing.m)
                )
            }
        }
    }
}

@Composable
private fun ScannerCamera(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var scanResult by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    // A stable ref the analyzer callback can read from any thread.
    val paused = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    LaunchedEffect(scanResult) { paused.set(scanResult != null) }

    var torchOn by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val savedMsg = stringResource(R.string.scanner_saved)

    // Haptic on detection
    LaunchedEffect(scanResult) {
        if (scanResult != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Torch control
    LaunchedEffect(torchOn, cameraRef) {
        cameraRef?.cameraControl?.enableTorch(torchOn)
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        val executor = remember { Executors.newSingleThreadExecutor() }
        val scanner = remember {
            val opts = com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE,
                    com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
                )
                .enableAllPotentialBarcodes()
                .build()
            BarcodeScanning.getClient(opts)
        }

        DisposableEffect(Unit) {
            onDispose {
                scanner.close()
                executor.shutdown()
            }
        }

        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    previewViewRef = previewView
                    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(android.util.Size(1920, 1080))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            if (paused.get()) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            @Suppress("UnsafeOptInUsageError")
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty() && !paused.get()) {
                                            val barcode = barcodes.first()
                                            val content = barcode.rawValue ?: barcode.displayValue ?: ""
                                            if (content.isNotBlank()) {
                                                scanResult = Triple(
                                                    content,
                                                    ScannerRepository.formatName(barcode.format),
                                                    barcode.format
                                                )
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            cameraRef = camera

                            // Start at 1.5x zoom so small codes fill more of the frame
                            camera.cameraControl.setLinearZoom(0.15f) // 0=1x, 1=max
                        } catch (_: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder overlay with corner marks + touch gestures (tap to focus, pinch to zoom)
        val dimColor = MeenaScrimDim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cameraRef, previewViewRef) {
                    detectTapGestures { offset ->
                        val camera = cameraRef ?: return@detectTapGestures
                        val pv = previewViewRef ?: return@detectTapGestures
                        val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                        camera.cameraControl.startFocusAndMetering(
                            androidx.camera.core.FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                        )
                    }
                }
                .pointerInput(cameraRef) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val camera = cameraRef ?: return@detectTransformGestures
                        val current = camera.cameraInfo.zoomState.value?.linearZoom ?: 0f
                        camera.cameraControl.setLinearZoom((current + (zoom - 1f) * 0.5f).coerceIn(0f, 1f))
                    }
                }
                .drawBehind {
                    val side = size.minDimension * 0.65f
                    val left = (size.width - side) / 2f
                    val top = (size.height - side) / 2.5f

                    // Dim outside the viewfinder
                    drawRect(dimColor, Offset.Zero, Size(size.width, top))
                    drawRect(dimColor, Offset(0f, top + side), Size(size.width, size.height - top - side))
                    drawRect(dimColor, Offset(0f, top), Size(left, side))
                    drawRect(dimColor, Offset(left + side, top), Size(size.width - left - side, side))

                    // Corner marks
                    val cornerLen = 24.dp.toPx()
                    val stroke = 2.dp.toPx()

                    // Top-left
                    drawLine(accentColor, Offset(left, top), Offset(left + cornerLen, top), stroke)
                    drawLine(accentColor, Offset(left, top), Offset(left, top + cornerLen), stroke)
                    // Top-right
                    drawLine(accentColor, Offset(left + side - cornerLen, top), Offset(left + side, top), stroke)
                    drawLine(accentColor, Offset(left + side, top), Offset(left + side, top + cornerLen), stroke)
                    // Bottom-left
                    drawLine(accentColor, Offset(left, top + side - cornerLen), Offset(left, top + side), stroke)
                    drawLine(accentColor, Offset(left, top + side), Offset(left + cornerLen, top + side), stroke)
                    // Bottom-right
                    drawLine(accentColor, Offset(left + side - cornerLen, top + side), Offset(left + side, top + side), stroke)
                    drawLine(accentColor, Offset(left + side, top + side - cornerLen), Offset(left + side, top + side), stroke)
                }
        )

        // Hint or result panel
        val result = scanResult
        if (result == null) {
            // Scanning hint + torch toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MeenaScrimHeavy)
                    .padding(MeenaSpacing.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MeenaSpacing.m)
            ) {
                Text(
                    text = stringResource(R.string.scanner_scanning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextMuted
                )
                Text(
                    text = stringResource(if (torchOn) R.string.scanner_torch_off else R.string.scanner_torch_on),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (torchOn) accentColor else MeenaTextMuted,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable { torchOn = !torchOn }
                        .padding(MeenaSpacing.s)
                )
            }
        } else {
            // Result panel
            val (content, format, _) = result
            val isUrl = content.startsWith("http://", true) || content.startsWith("https://", true)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MeenaScrimSolid)
                    .padding(MeenaSpacing.l),
                verticalArrangement = Arrangement.spacedBy(MeenaSpacing.m)
            ) {
                Text(
                    text = format,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeenaTextWhite,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState())
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MeenaSpacing.l)
                ) {
                    // Copy
                    Text(
                        text = stringResource(R.string.scanner_copy),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MeenaTextWhite,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .metroClickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("scan", content))
                            }
                            .padding(vertical = MeenaSpacing.m)
                    )
                    // Open link
                    if (isUrl) {
                        Text(
                            text = stringResource(R.string.scanner_open_link),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MeenaTextWhite,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .metroClickable {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(content)))
                                    }
                                }
                                .padding(vertical = MeenaSpacing.m)
                        )
                    }
                    // Save to history
                    Text(
                        text = stringResource(R.string.scanner_save),
                        style = MaterialTheme.typography.bodyLarge,
                        color = accentColor,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .metroClickable {
                                ScannerRepository.add(
                                    context,
                                    ScanEntry(content, format, System.currentTimeMillis())
                                )
                                my.hana.meena.ui.components.MetroMessage.show(savedMsg)
                            }
                            .padding(vertical = MeenaSpacing.m)
                    )
                }
                // Scan again
                Text(
                    text = stringResource(R.string.scanner_scan_again),
                    style = MaterialTheme.typography.labelSmall,
                    color = MeenaTextMuted,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .metroClickable { scanResult = null }
                        .padding(vertical = MeenaSpacing.s)
                )
            }
        }
    }
}

@Composable
private fun ScannerAppBar(title: String, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = MeenaSpacing.l,
                start = MeenaSpacing.l,
                end = MeenaSpacing.l,
                bottom = MeenaSpacing.m
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_thin_back),
            contentDescription = stringResource(R.string.settings_back),
            tint = MeenaTextWhite,
            modifier = Modifier
                .size(48.dp)
                .metroClickable(onClick = onNavigateBack)
                .padding(MeenaSpacing.m)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = MeenaTextWhite
        )
    }
}
