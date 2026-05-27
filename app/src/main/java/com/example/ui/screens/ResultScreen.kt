package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.data.ScanRecord
import com.example.ui.ScannerViewModel
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberDarkCard
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberMuted
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.CyberWarningRed
import com.example.ui.theme.CyberWhite
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val record by viewModel.activeRecord.collectAsState()

    if (record == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberDarkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = CyberNeonGreen)
        }
        return
    }

    val activeRec = record!!

    // Copy Utility
    fun copyToClipboard(text: String, label: String = "Scanned QR Text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    // Share Utility
    fun shareContent(text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Scanned QR Content")
        context.startActivity(shareIntent)
    }

    // URL threat security scanner model
    val securityCheck = remember(activeRec.rawContent) { checkUrlThreat(activeRec.rawContent) }
    val isSuspicious = securityCheck.first
    val securityNotice = securityCheck.second
    val securityLightColor = securityCheck.third

    // Suspicious Link Warning Popups Override Switch
    var showThreatConfirmationAlert by remember { mutableStateOf(isSuspicious) }

    // Auto-open 2 second countdown progress control states
    var isAutoOpenEnabled by remember { mutableStateOf(activeRec.contentType in listOf("URL", "IMAGE", "VIDEO", "PDF")) }
    var autoOpenCountdown by remember { mutableStateOf(200) } // 200 units = 2.0 seconds

    LaunchedEffect(isAutoOpenEnabled, activeRec.rawContent) {
        if (isAutoOpenEnabled && !isSuspicious) {
            autoOpenCountdown = 200
            while (autoOpenCountdown > 0) {
                delay(10)
                autoOpenCountdown--
            }
            // Auto open the url
            try {
                var urlToOpen = activeRec.rawContent
                if (!urlToOpen.startsWith("http://", ignoreCase = true) && !urlToOpen.startsWith("https://", ignoreCase = true)) {
                    urlToOpen = "https://$urlToOpen"
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SCAN DECODED",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = CyberWhite,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Return", tint = CyberNeonGreen)
                    }
                },
                actions = {
                    // Favorite Toggle button
                    IconButton(
                        onClick = { viewModel.toggleFavorite(activeRec) }
                    ) {
                        Icon(
                            imageVector = if (activeRec.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Pin Favorite",
                            tint = if (activeRec.isFavorite) CyberNeonGreen else CyberWhite
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = { shareContent(activeRec.rawContent) }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Publish", tint = CyberWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDarkSurface,
                    navigationIconContentColor = CyberNeonGreen
                )
            )
        },
        containerColor = CyberDarkBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Visual Category Hub Emblem Header card
                CategoryBadgeHeader(type = activeRec.contentType)

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Content Viewer Card (M3 styled glass card)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "RAW QR PAYLOAD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberMuted,
                                letterSpacing = 1.sp
                            )

                            // Quick copy button
                            IconButton(
                                onClick = {
                                    copyToClipboard(activeRec.rawContent)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Clipboard copy",
                                    tint = CyberNeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Safe display with sanitization and scrollable view
                        Text(
                            text = activeRec.rawContent,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberWhite,
                            lineHeight = 22.sp,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scanned_text_container")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Visual Preview Panel based on Content Classification Type
                when (activeRec.contentType) {
                    "URL", "IMAGE", "VIDEO", "PDF" -> {
                        // URL actions panel
                        UrlPreviewPanel(
                            rawUrl = activeRec.rawContent,
                            type = activeRec.contentType,
                            securityNotice = securityNotice,
                            securityColor = securityLightColor,
                            isSuspicious = isSuspicious,
                            isAutoOpenEnabled = isAutoOpenEnabled,
                            onToggleAutoOpen = { isAutoOpenEnabled = it },
                            countdownFraction = autoOpenCountdown / 200f,
                            onTriggerRedirect = {
                                if (isSuspicious) {
                                    showThreatConfirmationAlert = true
                                } else {
                                    val safeUrl = if (!activeRec.rawContent.startsWith("http://", ignoreCase = true) && !activeRec.rawContent.startsWith("https://", ignoreCase = true)) {
                                        "https://${activeRec.rawContent}"
                                    } else {
                                        activeRec.rawContent
                                    }
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
                                    context.startActivity(browserIntent)
                                }
                            }
                        )
                    }
                    "WIFI" -> {
                        WifiPreviewPanel(
                            rawWifi = activeRec.rawContent,
                            onCopyPassword = { pass -> copyToClipboard(pass, "WiFi Hotspot Password") }
                        )
                    }
                    "VCARD" -> {
                        VCardPreviewPanel(
                            rawVCard = activeRec.rawContent,
                            onTriggerSave = {
                                val intent = createSaveContactIntent(activeRec.rawContent)
                                context.startActivity(intent)
                            }
                        )
                    }
                    "LOCATION" -> {
                        LocationPreviewPanel(
                            rawGeo = activeRec.rawContent,
                            onOpenNavigation = {
                                val coordinates = activeRec.rawContent.replace("geo:", "", ignoreCase = true)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$coordinates"))
                                intent.setPackage("com.google.android.apps.maps")
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$coordinates"))
                                    context.startActivity(fallback)
                                }
                            }
                        )
                    }
                    "PAYMENT" -> {
                        PaymentPreviewPanel(rawPayment = activeRec.rawContent)
                    }
                    else -> {
                        // Plain formatted Text block view
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Advanced AI Lens Cognitive Summary section (Gemini integration!)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CyberNeonGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ADVANCED COGNITIVE ANALYSIS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberNeonGreen,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Reload/Analyze trigger
                            IconButton(
                                onClick = { viewModel.queryAISummaryForRecord(activeRec) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Trigger AI Analysis",
                                    tint = CyberNeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (activeRec.aiSummary.isNullOrBlank()) {
                            // loading or unrequested
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.queryAISummaryForRecord(activeRec) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = CyberNeonGreen,
                                    strokeWidth = 1.5.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Tap to initialize smart on-site AI synthesis...",
                                    color = CyberMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // Renders AI Insights
                            Text(
                                text = activeRec.aiSummary!!,
                                color = CyberWhite,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // High risk suspicious URLs warning popup overlay
            AnimatedVisibility(
                visible = showThreatConfirmationAlert,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE60A0C0D))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberWarningRed, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security Alarm",
                                tint = CyberWarningRed,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "AI Security Handshake Warning!",
                                color = CyberWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "LENS Security Engine detected suspicious indicators in this destination target:\n\n• Target: ${activeRec.rawContent}\n\n• Type: $securityNotice\n\nAutomatic redirected navigation is strictly prohibited for security, preventing automated malware injection or keylogging exploits.",
                                color = CyberMuted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        showThreatConfirmationAlert = false
                                        isAutoOpenEnabled = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberDarkCard, contentColor = CyberWhite),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Abort")
                                }
                                Button(
                                    onClick = {
                                        showThreatConfirmationAlert = false
                                        // open anyway
                                        var finalUrl = activeRec.rawContent
                                        if (!finalUrl.startsWith("http://", ignoreCase = true) && !finalUrl.startsWith("https://", ignoreCase = true)) {
                                            finalUrl = "https://$finalUrl"
                                        }
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                        context.startActivity(browserIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberWarningRed, contentColor = CyberWhite),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Override Risk")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadgeHeader(type: String) {
    val config = remember(type) {
        when (type) {
            "URL" -> Triple("Secure Domain", Icons.Default.OpenInBrowser, CyberNeonGreen)
            "IMAGE" -> Triple("Visual Photo", Icons.Default.OpenInNew, CyberNeonCyan)
            "VIDEO" -> Triple("Embedded Media Player", Icons.Default.OpenInNew, CyberNeonGreen)
            "PDF" -> Triple("PDF Document", Icons.Default.Info, CyberNeonCyan)
            "WIFI" -> Triple("Hotspot Credentials", Icons.Default.Wifi, CyberNeonGreen)
            "VCARD" -> Triple("VCard Social Badge", Icons.Default.PersonAdd, CyberNeonCyan)
            "LOCATION" -> Triple("Geographic Landmark", Icons.Default.LocationOn, CyberNeonGreen)
            "PAYMENT" -> Triple("Financial Gateway", Icons.Default.Payment, CyberWarningRed)
            else -> Triple("Plain Text Code", Icons.Default.Info, CyberMuted)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(config.third.copy(alpha = 0.15f))
                .border(1.5.dp, config.third, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = config.second,
                contentDescription = null,
                tint = config.third,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = config.first,
            color = CyberWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
        Text(
            text = "Detected Content Category",
            color = CyberMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun UrlPreviewPanel(
    rawUrl: String,
    type: String,
    securityNotice: String,
    securityColor: Color,
    isSuspicious: Boolean,
    isAutoOpenEnabled: Boolean,
    onToggleAutoOpen: (Boolean) -> Unit,
    countdownFraction: Float,
    onTriggerRedirect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Security Certificate marker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(securityColor.copy(alpha = 0.1f))
                    .border(0.5.dp, securityColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSuspicious) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = securityColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = securityNotice,
                    color = securityColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Extra display for IMAGE URLs inside actual frame
            if (type == "IMAGE") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberDarkCard),
                    contentAlignment = Alignment.Center
                ) {
                    val painter = rememberAsyncImagePainter(model = rawUrl)
                    Image(
                        painter = painter,
                        contentDescription = "Image preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Auto-redirect scheduler bar
            if (!isSuspicious) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleAutoOpen(!isAutoOpenEnabled) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAutoOpenEnabled,
                        onCheckedChange = { onToggleAutoOpen(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberNeonGreen,
                            uncheckedColor = CyberMuted,
                            checkmarkColor = CyberDarkBg
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auto-launch link in secure browser",
                        color = CyberWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isAutoOpenEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Timer bar visual progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CyberDarkCard)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = countdownFraction)
                                .background(CyberNeonGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Button(
                onClick = onTriggerRedirect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSuspicious) CyberWarningRed else CyberNeonGreen,
                    contentColor = CyberDarkBg
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSuspicious) "Safety Override & Open" else "Open in Secure Browser",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WifiPreviewPanel(
    rawWifi: String,
    onCopyPassword: (String) -> Unit
) {
    // Parser: WIFI:S:<SSID>;T:<WPA|WEP|nopass>;P:<PASSWORD>;H:<true|false>;;
    val ssid = remember(rawWifi) {
        val part = rawWifi.split(";").firstOrNull { it.startsWith("WIFI:S:", ignoreCase = true) || it.startsWith("S:", ignoreCase = true) }
        part?.replace("WIFI:S:", "", ignoreCase = true)?.replace("S:", "", ignoreCase = true) ?: "Unknown SSID"
    }

    val pass = remember(rawWifi) {
        val part = rawWifi.split(";").firstOrNull { it.startsWith("P:", ignoreCase = true) }
        part?.replace("P:", "", ignoreCase = true) ?: ""
    }

    val encType = remember(rawWifi) {
        val part = rawWifi.split(";").firstOrNull { it.startsWith("T:", ignoreCase = true) }
        part?.replace("T:", "", ignoreCase = true) ?: "WPA/WPA2"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = CyberNeonGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "NETWORK NAME (SSID)", fontSize = 10.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                    Text(text = ssid, fontSize = 18.sp, color = CyberWhite, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "SECURITY ENCRYPTION", fontSize = 10.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                    Text(text = encType, fontSize = 14.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
                }

                if (pass.isNotEmpty()) {
                    Button(
                        onClick = { onCopyPassword(pass) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberDarkCard, contentColor = CyberNeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Copy Wifi Pass", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (pass.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("NETWORK PASSWORD", fontSize = 9.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pass,
                            color = CyberWhite,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "💡 Connection Guide:\n• Open your device Android Settings -> Network -> WiFi\n• Discover SSID \"$ssid\"\n• Use password listed above to connect immediately.",
                fontSize = 11.sp,
                color = CyberMuted,
                lineHeight = 16.sp,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
}

@Composable
fun VCardPreviewPanel(
    rawVCard: String,
    onTriggerSave: () -> Unit
) {
    val name = remember(rawVCard) {
        val fn = rawVCard.lines().firstOrNull { it.startsWith("FN:", ignoreCase = true) }
        fn?.replace("FN:", "", ignoreCase = true) ?: "VCard Contact"
    }

    val phone = remember(rawVCard) {
        val tel = rawVCard.lines().firstOrNull { it.startsWith("TEL", ignoreCase = true) }
        // Extracts whatever is after the last colon in string
        tel?.split(":")?.lastOrNull() ?: ""
    }

    val email = remember(rawVCard) {
        val mail = rawVCard.lines().firstOrNull { it.startsWith("EMAIL", ignoreCase = true) }
        mail?.split(":")?.lastOrNull() ?: ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "VCARD DETECTED",
                fontSize = 10.sp,
                color = CyberNeonCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name,
                fontSize = 20.sp,
                color = CyberWhite,
                fontWeight = FontWeight.ExtraBold
            )

            if (phone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "PHONE NUMBER", fontSize = 9.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                Text(text = phone, fontSize = 15.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
            }

            if (email.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "EMAIL BOX", fontSize = 9.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                Text(text = email, fontSize = 15.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTriggerSave,
                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan, contentColor = CyberDarkBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Mobile Address Book", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun LocationPreviewPanel(
    rawGeo: String,
    onOpenNavigation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CyberNeonGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "COORDINATES", fontSize = 10.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                    Text(text = rawGeo, fontSize = 15.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenNavigation,
                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonGreen, contentColor = CyberDarkBg),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Launch Navigation in Google Maps", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaymentPreviewPanel(rawPayment: String) {
    var upiAmount = remember(rawPayment) {
        try {
            val idx = rawPayment.indexOf("am=")
            if (idx != -1) {
                rawPayment.substring(idx + 3).split("&").firstOrNull() ?: ""
            } else ""
        } catch (e: Exception) { "" }
    }

    var merchant = remember(rawPayment) {
        try {
            val idx = rawPayment.indexOf("pn=")
            if (idx != -1) {
                rawPayment.substring(idx + 3).split("&").firstOrNull()?.replace("%20", " ") ?: ""
            } else ""
        } catch (e: Exception) { "" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = CyberWarningRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FINANCIAL TRANSACTION QR",
                    fontSize = 11.sp,
                    color = CyberWarningRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (merchant.isNotEmpty()) {
                Text(text = "MERCHANT DESTINATION", fontSize = 9.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                Text(text = merchant, fontSize = 16.sp, color = CyberWhite, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (upiAmount.isNotEmpty()) {
                Text(text = "AMOUNT REQUESTED", fontSize = 9.sp, color = CyberMuted, fontWeight = FontWeight.Bold)
                Text(text = "$$upiAmount", fontSize = 24.sp, color = CyberNeonGreen, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Security warning
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberWarningRed.copy(alpha = 0.05f))
                    .padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CyberWarningRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify payment address and merchant credentials. Clicking links outside registered mobile money interfaces poses phishing risks.",
                    color = CyberWarningRed,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Parses raw VCard or MeCard text and constructs an insert contact Intent.
 */
fun createSaveContactIntent(rawVCard: String): Intent {
    val name = run {
        val fn = rawVCard.lines().firstOrNull { it.startsWith("FN:", ignoreCase = true) }
        fn?.replace("FN:", "", ignoreCase = true) ?: "VCard Contact"
    }

    val phone = run {
        val tel = rawVCard.lines().firstOrNull { it.startsWith("TEL", ignoreCase = true) }
        tel?.split(":")?.lastOrNull() ?: ""
    }

    val email = run {
        val mail = rawVCard.lines().firstOrNull { it.startsWith("EMAIL", ignoreCase = true) }
        mail?.split(":")?.lastOrNull() ?: ""
    }

    return Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
        putExtra(ContactsContract.Intents.Insert.NAME, name)
        putExtra(ContactsContract.Intents.Insert.PHONE, phone)
        putExtra(ContactsContract.Intents.Insert.EMAIL, email)
    }
}

/**
 * Quick analysis for malicious target properties.
 */
fun checkUrlThreat(rawUrl: String): Triple<Boolean, String, Color> {
    val trimmed = rawUrl.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
        return Triple(false, "Offline / Local Directory Protocol", CyberNeonCyan)
    }

    val lower = trimmed.lowercase()
    if (lower.startsWith("http://")) {
        return Triple(true, "Unsecured HTTP Plaintext Domain", CyberWarningRed)
    }

    val suspiciousPhishKeywords = listOf("phish", "scam", "rewards-free", "login-verify", "update-bank", "winner-claim", "giftcard", "paypal-auth", "netflix-secure")
    for (keyword in suspiciousPhishKeywords) {
        if (lower.contains(keyword)) {
            return Triple(true, "Malicious Metadata Match: '$keyword'", CyberWarningRed)
        }
    }

    if (lower.contains(".exe") || lower.contains(".apk") || lower.contains(".zip") || lower.contains(".msi")) {
        return Triple(true, "Malicious Binary target (.apk / .exe download)", CyberWarningRed)
    }

    return Triple(false, "Verified HTTPS Node Secure Protection", CyberNeonGreen)
}
