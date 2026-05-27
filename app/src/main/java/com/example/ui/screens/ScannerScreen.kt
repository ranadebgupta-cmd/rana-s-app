package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.GeminiClient
import com.example.ui.ScanUiState
import com.example.ui.ScannerViewModel
import com.example.ui.theme.CyberBlackAlpha
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberDarkCard
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberGlassSurface
import com.example.ui.theme.CyberGreenAlpha
import com.example.ui.theme.CyberMuted
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.CyberWarningRed
import com.example.ui.theme.CyberWhite
import kotlinx.coroutines.launch
import com.example.utils.QRDecoder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToGenerator: () -> Unit,
    onNavigateToDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.activeState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Permission tracking
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Camera settings state
    var isFlashOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }

    // local pick photo utility
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = QRDecoder.uriToBitmap(context, uri)
            if (bitmap != null) {
                // process bitmap
                coroutineScope.launch {
                    val rawValue = QRDecoder.decodeBitmap(bitmap)
                    if (rawValue != null) {
                        viewModel.processScanResult(rawValue)
                    } else {
                        viewModel.activeState.value = ScanUiState.Error("No valid QR Code discovered in selected image.")
                    }
                }
            } else {
                viewModel.activeState.value = ScanUiState.Error("Could not render image file.")
            }
        }
    }

    // React to scanning state transitions
    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Success) {
            onNavigateToDetail()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
    ) {
        // Render view finder if permission is approved
        if (cameraPermissionState.status.isGranted) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().apply {
                                surfaceProvider = previewView.surfaceProvider
                            }

                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                            val scanner = BarcodeScanning.getClient(options)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && viewModel.activeState.value == ScanUiState.Idle) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val firstCode = barcodes.firstOrNull()
                                            val textVal = firstCode?.rawValue
                                            if (textVal != null) {
                                                viewModel.processScanResult(textVal)
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = cam
                                cam.cameraControl.enableTorch(isFlashOn)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = {
                        cameraControl?.cameraControl?.enableTorch(isFlashOn)
                    }
                )

                // Laser Overlay overlay
                ScannerOverlay(
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Unapproved view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "QR Scanner",
                    tint = CyberNeonGreen,
                    modifier = Modifier
                        .size(96.dp)
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyberWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "QR Scanner AI queries your camera natively to recognize codes in less than 1 second offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonGreen, contentColor = CyberDarkBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("grant_camera_permission")
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // HUD Dashboard Overlay (Always shown)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Action HUD Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                    .background(CyberDarkSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI LENS ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberWhite,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "PRO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberNeonGreen,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (GeminiClient.isApiKeyAvailable()) CyberNeonGreen else CyberNeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (GeminiClient.isApiKeyAvailable()) "AI COGNITIVE: ON" else "AI STANDBY MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = CyberWhite
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Flash toggle
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isFlashOn) CyberNeonGreen else Color.Transparent,
                            contentColor = if (isFlashOn) CyberDarkBg else CyberWhite
                        )
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight Toggle"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Front/Rear Camera switch
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = CyberNeonGreen
                        )
                    ) {
                        Icon(
                            imageVector = if (useFrontCamera) Icons.Default.CameraFront else Icons.Default.CameraRear,
                            contentDescription = "Rotate Lens"
                        )
                    }
                }
            }

            // Scanning state updates
            if (uiState is ScanUiState.Processing) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberGlassSurface),
                    modifier = Modifier.border(1.dp, CyberNeonGreen, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = CyberNeonGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Processing QR Matrix...",
                            color = CyberWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (uiState is ScanUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                    modifier = Modifier.border(1.dp, CyberWarningRed, RoundedCornerShape(12.dp)).clickable { viewModel.resetScannerState() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (uiState as ScanUiState.Error).message,
                            color = CyberWhite,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to dismiss",
                            color = CyberMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            // Bottom Dashboard HUD Panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(26.dp))
                        .background(CyberDarkSurface)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upload Image File button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("upload_image_option")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CyberDarkCard)
                                .border(1.dp, CyberBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Upload QR",
                                tint = CyberWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Upload QR", fontSize = 11.sp, color = CyberWhite, fontWeight = FontWeight.Medium)
                    }

                    // Scan QR CTA main button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("scan_qr_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberNeonGreen)
                                .clickable {
                                    if (uiState !is ScanUiState.Idle) {
                                        viewModel.resetScannerState()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Live",
                                tint = CyberDarkBg,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Scanning Live", fontSize = 12.sp, color = CyberNeonGreen, fontWeight = FontWeight.Bold)
                    }

                    // History navigate button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigateToHistory() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CyberDarkCard)
                                .border(1.dp, CyberBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History Log",
                                tint = CyberWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Scan Logs", fontSize = 11.sp, color = CyberWhite, fontWeight = FontWeight.Medium)
                    }

                    // Generator navigate button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigateToGenerator() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CyberDarkCard)
                                .border(1.dp, CyberBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Generate QR",
                                tint = CyberWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Generator", fontSize = 11.sp, color = CyberWhite, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Compute box frame constraints (square in index center)
        val boxWidth = 260.dp.toPx()
        val boxHeight = 260.dp.toPx()
        val xOffset = (width - boxWidth) / 2
        val yOffset = (height - boxHeight) / 2

        val rect = Rect(xOffset, yOffset, xOffset + boxWidth, yOffset + boxHeight)
        val roundRect = RoundRect(rect, CornerRadius(24.dp.toPx(), 24.dp.toPx()))

        val path = Path().apply {
            addRoundRect(roundRect)
        }

        // Clip hollow center region to draw darker shroud
        clipPath(path, clipOp = ClipOp.Difference) {
            drawRect(
                color = Color(0xB2060809),
                topLeft = Offset.Zero,
                size = size
            )
        }

        // Draw animated laser line
        val laserY = yOffset + (boxHeight * animatedProgress)
        drawLine(
            color = CyberNeonGreen,
            strokeWidth = 4.dp.toPx(),
            start = Offset(xOffset + 12.dp.toPx(), laserY),
            end = Offset(xOffset + boxWidth - 12.dp.toPx(), laserY),
            alpha = 0.85f
        )

        // Draw laser halo glow
        drawRect(
            color = CyberGreenAlpha,
            topLeft = Offset(xOffset + 6.dp.toPx(), yOffset + 6.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(boxWidth - 12.dp.toPx(), boxHeight - 12.dp.toPx())
        )

        // Draw Corner Highlights around Cutout
        val lineLen = 32.dp.toPx()
        val strokeW = 4.dp.toPx()

        // Top Left corner
        drawLine(CyberNeonGreen, Offset(xOffset, yOffset), Offset(xOffset + lineLen, yOffset), strokeW)
        drawLine(CyberNeonGreen, Offset(xOffset, yOffset), Offset(xOffset, yOffset + lineLen), strokeW)

        // Top Right corner
        drawLine(CyberNeonGreen, Offset(xOffset + boxWidth, yOffset), Offset(xOffset + boxWidth - lineLen, yOffset), strokeW)
        drawLine(CyberNeonGreen, Offset(xOffset + boxWidth, yOffset), Offset(xOffset + boxWidth, yOffset + lineLen), strokeW)

        // Bottom Left corner
        drawLine(CyberNeonGreen, Offset(xOffset, yOffset + boxHeight), Offset(xOffset + lineLen, yOffset + boxHeight), strokeW)
        drawLine(CyberNeonGreen, Offset(xOffset, yOffset + boxHeight), Offset(xOffset, yOffset + boxHeight - lineLen), strokeW)

        // Bottom Right corner
        drawLine(CyberNeonGreen, Offset(xOffset + boxWidth, yOffset + boxHeight), Offset(xOffset + boxWidth - lineLen, yOffset + boxHeight), strokeW)
        drawLine(CyberNeonGreen, Offset(xOffset + boxWidth, yOffset + boxHeight), Offset(xOffset + boxWidth, yOffset + boxHeight - lineLen), strokeW)
    }
}
