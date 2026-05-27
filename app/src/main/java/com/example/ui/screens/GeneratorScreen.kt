package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.CyberDarkCard
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyberMuted
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.CyberWhite
import com.example.utils.QRGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = URL/Text, 1 = WiFi, 2 = Contact

    // Text state fields
    var payloadText by remember { mutableStateOf("") }

    // Wifi state fields
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") } // WPA, WEP, Open

    // Contact state fields
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    // Output Generated QR Code bitmap
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showQRResultCard by remember { mutableStateOf(false) }

    fun processQRCodeGeneration() {
        val finalPayload = when (selectedTabIndex) {
            0 -> payloadText.trim()
            1 -> {
                val secType = if (wifiSecurity == "Open") "nopass" else wifiSecurity
                "WIFI:S:${wifiSsid.trim()};T:$secType;P:${wifiPassword.trim()};;"
            }
            else -> {
                "BEGIN:VCARD\nVERSION:3.0\nFN:${contactName.trim()}\nTEL:${contactPhone.trim()}\nEMAIL:${contactEmail.trim()}\nEND:VCARD"
            }
        }

        if (finalPayload.isNotBlank()) {
            val qr = QRGenerator.generateQRCode(finalPayload, 512)
            if (qr != null) {
                generatedBitmap = qr
                showQRResultCard = true
            }
        }
    }

    // Share bitmap helper using Intent
    fun shareGeneratedQRImage(bitmap: Bitmap) {
        try {
            @Suppress("DEPRECATION")
            val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "QR_Code", "Generated QR Code")
            if (path != null) {
                val uri = Uri.parse(path)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/jpeg"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share QR Code Artwork"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "QR CODE CREATOR",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDarkSurface
                )
            )
        },
        containerColor = CyberDarkBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            // Option selectors
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CyberDarkSurface,
                contentColor = CyberNeonGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = CyberNeonGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        showQRResultCard = false
                    },
                    text = { Text("URL / TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                    selectedContentColor = CyberNeonGreen,
                    unselectedContentColor = CyberMuted
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        showQRResultCard = false
                    },
                    text = { Text("WIFI NET", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                    selectedContentColor = CyberNeonGreen,
                    unselectedContentColor = CyberMuted
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        showQRResultCard = false
                    },
                    text = { Text("CONTACT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                    selectedContentColor = CyberNeonGreen,
                    unselectedContentColor = CyberMuted
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {

                // Option inputs
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (selectedTabIndex) {
                            0 -> {
                                // Simple Text or URL Input
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = CyberNeonGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enter site url or raw text", fontSize = 12.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = payloadText,
                                    onValueChange = { payloadText = it },
                                    placeholder = { Text("e.g. https://google.com/build", color = CyberMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CyberWhite,
                                        unfocusedTextColor = CyberWhite,
                                        focusedBorderColor = CyberNeonGreen,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("url_text_input")
                                )
                            }
                            1 -> {
                                // Wifi Hotspot Form
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = CyberNeonGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("WiFi Hotspot Settings", fontSize = 12.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = wifiSsid,
                                    onValueChange = { wifiSsid = it },
                                    label = { Text("Network SSID (Name)", color = CyberMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CyberWhite,
                                        unfocusedTextColor = CyberWhite,
                                        focusedBorderColor = CyberNeonGreen,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = wifiPassword,
                                    onValueChange = { wifiPassword = it },
                                    label = { Text("Hotspot Password", color = CyberMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CyberWhite,
                                        unfocusedTextColor = CyberWhite,
                                        focusedBorderColor = CyberNeonGreen,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Select chips
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("WPA", "WEP", "Open").forEach { opt ->
                                        val selected = wifiSecurity == opt
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) CyberNeonGreen.copy(alpha = 0.15f) else CyberDarkCard)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (selected) CyberNeonGreen else CyberBorder,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { wifiSecurity = opt }
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = opt,
                                                color = if (selected) CyberNeonGreen else CyberWhite,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                // VCard Contact Form
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = CyberNeonGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Contact social inputs", fontSize = 12.sp, color = CyberWhite, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = contactName,
                                    onValueChange = { contactName = it },
                                    label = { Text("Display Full Name", color = CyberMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CyberWhite,
                                        unfocusedTextColor = CyberWhite,
                                        focusedBorderColor = CyberNeonGreen,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = contactPhone,
                                    onValueChange = { contactPhone = it },
                                    label = { Text("Cell Number", color = CyberMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CyberWhite,
                                        unfocusedTextColor = CyberWhite,
                                        focusedBorderColor = CyberNeonGreen,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = contactEmail,
                                    onValueChange = { contactEmail = it },
                                    label = { Text("Email destination", color = CyberMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = CyberWhite,
                                        unfocusedTextColor = CyberWhite,
                                        focusedBorderColor = CyberNeonGreen,
                                        unfocusedBorderColor = CyberBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { processQRCodeGeneration() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonGreen, contentColor = CyberDarkBg),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Generate QR Art", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Output Result Artwork Card
                AnimatedVisibility(
                    visible = showQRResultCard,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (generatedBitmap != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyberNeonGreen, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "GENERATED ARTWORK PREVIEW",
                                    fontSize = 11.sp,
                                    color = CyberNeonGreen,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Render QR code Bitmap image on styled frame
                                Image(
                                    bitmap = generatedBitmap!!.asImageBitmap(),
                                    contentDescription = "Created QR Artwork",
                                    modifier = Modifier
                                        .size(240.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(androidx.compose.ui.graphics.Color.White)
                                        .padding(16.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { shareGeneratedQRImage(generatedBitmap!!) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberDarkBg, contentColor = CyberNeonGreen),
                                    border = BorderStroke(1.dp, CyberNeonGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Export Art / Share QR", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
