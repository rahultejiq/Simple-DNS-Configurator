package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DnsServer
import com.example.data.model.QueryLog
import com.example.data.network.DnsResolverResult

// Clean Minimalism Theme Colors (Aesthetic Light & Airy Palette)
val BgDark = Color(0xFFFDFBFF)       // Light canvas background, was BgDark
val CardDark = Color(0xFFFFFFFF)     // Clean white cards for contrast, was CardDark
val AccentMint = Color(0xFF0061A4)   // High-contrast deep blue primary, was electric mint
val SubtextGray = Color(0xFF44474E)  // Secondary medium detail text slate, was SubtextGray
val MutedNavy = Color(0xFFC4C6D0)    // Border and line gray, was MutedNavy
val SuccessGreen = Color(0xFF1B7A3E) // Calmer organic forest green, was SuccessGreen
val ErrorRed = Color(0xFFBA1A1A)     // Soft crimson error red, was ErrorRed
val WarningAmber = Color(0xFF9E6400) // Deep muted warning safety gold, was WarningAmber

// Clean Minimalism dedicated layout colors
val TextDark = Color(0xFF1B1B1F)     // Main body text dark slate
val ActiveContainerBlue = Color(0xFFD3E4FF) // Warm selected blue container badge
val ActiveTextBlue = Color(0xFF001D36) // Dark blue text for active badge
val LightGrayContainer = Color(0xFFF2F3F9) // Light grey rounded container/inner forms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsAppScreen(
    viewModel: DnsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Gather Live States from StateFlows
    val servers by viewModel.allServers.collectAsStateWithLifecycle()
    val logs by viewModel.recentQueryLogs.collectAsStateWithLifecycle()

    val serverName by viewModel.newServerName.collectAsStateWithLifecycle()
    val serverType by viewModel.newServerType.collectAsStateWithLifecycle()
    val serverEndpoint by viewModel.newServerEndpoint.collectAsStateWithLifecycle()
    val serverPort by viewModel.newServerPort.collectAsStateWithLifecycle()
    val serverError by viewModel.serverFormError.collectAsStateWithLifecycle()

    val queryDomain by viewModel.queryDomain.collectAsStateWithLifecycle()
    val queryType by viewModel.queryType.collectAsStateWithLifecycle()
    val isQuerying by viewModel.isQuerying.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastQueryResult.collectAsStateWithLifecycle()

    var showForm by remember { mutableStateOf(false) }

    val activeServer = servers.find { it.isActive }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dns_cat_logo),
                            contentDescription = "Logo of a cat holding letter d",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.2.dp, AccentMint.copy(alpha = 0.6f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "Private DNS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextDark
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.runDnsQuery()
                        },
                        modifier = Modifier.testTag("refresh_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test active resolver",
                            tint = AccentMint
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextDark,
                    actionIconContentColor = AccentMint
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. ACTIVE SERVER & SYSTEM CONNECTION GUIDE CARD
            item {
                ActiveServerCard(
                    activeServer = activeServer,
                    onOpenSettings = {
                        try {
                            val intent = Intent("android.settings.PRIVATE_DNS_SETTINGS")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                context.startActivity(intent)
                            } catch (ex: Exception) {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    },
                    onCopyHostname = { host ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Private DNS Server", host)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied DNS hostname: $host", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. DNS QUERY SANDBOX TERMINAL
            item {
                QuerySandboxCard(
                    domain = queryDomain,
                    type = queryType,
                    isQuerying = isQuerying,
                    lastResult = lastResult,
                    onDomainChange = { viewModel.setQueryDomain(it) },
                    onTypeChange = { viewModel.setQueryType(it) },
                    onRunQuery = {
                        keyboardController?.hide()
                        viewModel.runDnsQuery()
                    }
                )
            }

            // 3. SERVER LIST CARD
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, MutedNavy.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DNS Servers List",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            Button(
                                onClick = { showForm = !showForm },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showForm) LightGrayContainer else AccentMint,
                                    contentColor = if (showForm) AccentMint else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("toggle_add_server")
                            ) {
                                Icon(
                                    imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showForm) "Close" else "Custom",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Add Custom Server Expandable Section
                        AnimatedVisibility(
                            visible = showForm,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            CustomServerForm(
                                name = serverName,
                                type = serverType,
                                endpoint = serverEndpoint,
                                port = serverPort,
                                error = serverError,
                                onNameChange = { viewModel.setNewServerName(it) },
                                onTypeChange = { viewModel.setNewServerType(it) },
                                onEndpointChange = { viewModel.setNewServerEndpoint(it) },
                                onPortChange = { viewModel.setNewServerPort(it) },
                                onAddClick = {
                                    viewModel.addCustomServer()
                                    showForm = false
                                    keyboardController?.hide()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Seed records inside static items
                        servers.forEach { server ->
                            ServerItemRow(
                                server = server,
                                onActivate = { viewModel.selectActiveServer(server.id) },
                                onDelete = { viewModel.deleteServer(server) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (servers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Initializing standard servers...",
                                    color = SubtextGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. HISTORICAL LATENCY LOGS CARD
            item {
                LogsDashboardCard(
                    logs = logs,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
        }
    }
}

@Composable
fun ActiveServerCard(
    activeServer: DnsServer?,
    onOpenSettings: () -> Unit,
    onCopyHostname: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, MutedNavy.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val isDoT = activeServer?.type == "DOT"
            val bannerBg = if (activeServer != null) ActiveContainerBlue else LightGrayContainer
            val bannerContentColor = if (activeServer != null) ActiveTextBlue else TextDark

            // Interactive status bubble matched to Clean Minimalism HTML header SPEC
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bannerBg)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Elevated, solid status round indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (activeServer != null) ActiveTextBlue else SubtextGray.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(
                                    width = 2.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (activeServer != null) "Connected" else "Private DNS Off",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = bannerContentColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (activeServer != null) {
                            if (isDoT) "Active via DNS-over-TLS" else "Active via DNS-over-HTTPS"
                        } else {
                            "Disable secure private DNS resolution"
                        },
                        fontSize = 13.sp,
                        color = bannerContentColor.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Information Detail based on type (DoH or DoT)
            if (activeServer != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightGrayContainer, shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isDoT) "DNS-over-TLS Server Hostname" else "DNS-over-HTTPS Endpoint URI",
                            color = SubtextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeServer.endpoint,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentMint,
                                modifier = Modifier.weight(1f)
                            )

                            if (isDoT) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onCopyHostname(activeServer.endpoint) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentMint,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("copy_hostname_button")
                                ) {
                                    Text("Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isDoT) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Port: ${activeServer.port ?: 853}",
                                color = SubtextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Select a pre-populated or custom DNS resolver from the listing below to initiate secure lookups.",
                    color = SubtextGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guidance & System Integration Helper
            Text(
                text = "Apply settings system-wide:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "1. Select a DNS-over-TLS (DoT) server from the list below.\n" +
                       "2. Copy the hostname (e.g. dns.cloudflare.com) to your clipboard.\n" +
                       "3. Click \"Open Android Settings\" below.\n" +
                       "4. Go to Network & Internet -> Private DNS.\n" +
                       "5. Choose \"Private DNS provider hostname\" and paste.",
                color = SubtextGray,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightGrayContainer,
                    contentColor = AccentMint
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MutedNavy.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_dns_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Private DNS Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ServerItemRow(
    server: DnsServer,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val isSelected = server.isActive
    val cardBg = if (isSelected) ActiveContainerBlue else LightGrayContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onActivate)
            .testTag("server_row_${server.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.getWeight())
            ) {
                // Radio button circle matching HTML SPEC
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            width = if (isSelected) 6.dp else 2.dp,
                            color = if (isSelected) AccentMint else SubtextGray.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.name,
                            color = if (isSelected) ActiveTextBlue else TextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Server Type badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (server.type == "DOT") Color(0xFF0061A4).copy(alpha = 0.15f)
                                            else Color(0xFFBA1A1A).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = server.type,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (server.type == "DOT") Color(0xFF0061A4) else Color(0xFFBA1A1A)
                            )
                        }
                    }
                    Text(
                        text = server.endpoint,
                        color = if (isSelected) ActiveTextBlue.copy(alpha = 0.7f) else SubtextGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1
                    )
                }
            }

            // Right utilities: Trash button if custom
            if (server.isCustom) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_server_${server.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete custom server",
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomServerForm(
    name: String,
    type: String,
    endpoint: String,
    port: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onEndpointChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(LightGrayContainer, shape = RoundedCornerShape(20.dp))
            .border(1.dp, MutedNavy.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Configure Custom DNS Server",
                color = AccentMint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Validation feedback label
            if (error != null) {
                Text(
                    text = error,
                    color = ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Server Name") },
                placeholder = { Text("e.g. My Private AdBlock") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedBorderColor = AccentMint,
                    unfocusedBorderColor = MutedNavy,
                    focusedLabelColor = AccentMint,
                    unfocusedLabelColor = SubtextGray,
                    focusedPlaceholderColor = SubtextGray,
                    unfocusedPlaceholderColor = SubtextGray
                )
            )

            // Segmented DNS-type selector matching HTML design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf("DOH", "DOT")
                types.forEach { t ->
                    val isSelected = type == t
                    Button(
                        onClick = { onTypeChange(t) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AccentMint else CardDark,
                            contentColor = if (isSelected) Color.White else TextDark
                        ),
                        border = if (!isSelected) BorderStroke(1.dp, MutedNavy.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = if (t == "DOH") "DoH (HTTPS)" else "DoT (TLS)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Service Endpoint or Hostname depending on selection
            OutlinedTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                label = { Text(if (type == "DOH") "Endpoint URL" else "Hostname or IP") },
                placeholder = {
                    Text(
                        if (type == "DOH") "e.g. https://dns.example.com/query"
                        else "e.g. dns.example.com"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = if (type == "DOT") ImeAction.Next else ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_endpoint_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedBorderColor = AccentMint,
                    unfocusedBorderColor = MutedNavy,
                    focusedLabelColor = AccentMint,
                    unfocusedLabelColor = SubtextGray,
                    focusedPlaceholderColor = SubtextGray,
                    unfocusedPlaceholderColor = SubtextGray
                )
            )

            // Port (Only active/visible if DOT is selected)
            if (type == "DOT") {
                OutlinedTextField(
                    value = port,
                    onValueChange = onPortChange,
                    label = { Text("Port (Strict TLS)") },
                    placeholder = { Text("853") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_port_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = MutedNavy,
                        focusedLabelColor = AccentMint,
                        unfocusedLabelColor = SubtextGray,
                        focusedPlaceholderColor = SubtextGray,
                        unfocusedPlaceholderColor = SubtextGray
                    )
                )
            }

            // Save/Add Button action
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("add_server_submit")
            ) {
                Text(
                    text = "Add to Listing",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun QuerySandboxCard(
    domain: String,
    type: String,
    isQuerying: Boolean,
    lastResult: DnsResolverResult?,
    onDomainChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onRunQuery: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, MutedNavy.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Interactive Query Tester",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verify secure DNS lookups to your active resolver locally.",
                fontSize = 11.sp,
                color = SubtextGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sandbox form inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = onDomainChange,
                    placeholder = { Text("e.g. google.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onRunQuery() }
                    ),
                    modifier = Modifier
                        .weight(1.getWeight())
                        .testTag("query_domain_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedBorderColor = AccentMint,
                        unfocusedBorderColor = MutedNavy,
                        focusedPlaceholderColor = SubtextGray,
                        unfocusedPlaceholderColor = SubtextGray
                    )
                )

                // Inline type selection
                var showTypeDropdown by remember { mutableStateOf(false) }
                val queryTypesList = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS")

                Box {
                    Button(
                        onClick = { showTypeDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LightGrayContainer, contentColor = TextDark),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("query_type_trigger")
                    ) {
                        Text(text = type, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false },
                        modifier = Modifier.background(CardDark)
                    ) {
                        queryTypesList.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(text = t, color = TextDark, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    onTypeChange(t)
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trigger Lookup Button
            Button(
                onClick = onRunQuery,
                enabled = !isQuerying && domain.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentMint,
                    contentColor = Color.White,
                    disabledContainerColor = LightGrayContainer,
                    disabledContentColor = SubtextGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("run_query_button")
            ) {
                if (isQuerying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Resolving Securely...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Query Selected Server", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Expandable query output results container
            if (lastResult != null) {
                Spacer(modifier = Modifier.height(14.dp))
                TerminalOutputView(result = lastResult)
            }
        }
    }
}

@Composable
fun TerminalOutputView(result: DnsResolverResult) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Diagnostic Log Details",
                fontSize = 12.sp,
                color = SubtextGray,
                fontWeight = FontWeight.Bold
            )

            // Performance tag indicator
            val time = when (result) {
                is DnsResolverResult.Success -> result.latencyMs
                is DnsResolverResult.Failure -> result.latencyMs
            }
            val timeColor = when {
                time < 50 -> SuccessGreen
                time < 150 -> WarningAmber
                else -> ErrorRed
            }
            Text(
                text = "Latency: ${time}ms",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = timeColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(LightGrayContainer, shape = RoundedCornerShape(12.dp))
                .border(1.dp, MutedNavy.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
                .testTag("diagnostic_terminal_output")
        ) {
            when (result) {
                is DnsResolverResult.Success -> {
                    val codeName = result.response.rCodeName
                    val recordsCount = result.response.answers.size

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RCODE: ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SubtextGray
                            )
                            Text(
                                text = codeName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (codeName == "NOERROR") SuccessGreen else WarningAmber
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "RECORDS: ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SubtextGray
                            )
                            Text(
                                text = "$recordsCount answers",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentMint
                            )
                        }

                        Divider(color = MutedNavy.copy(alpha = 0.5f), thickness = 0.5.dp)

                        if (result.response.answers.isEmpty()) {
                            Text(
                                text = ";; SUCCESS BUT NO RECORDS FOUND (NXDOMAIN/NODATA)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SubtextGray
                            )
                        } else {
                            result.response.answers.forEach { r ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = r.name,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = TextDark
                                        )
                                        Row {
                                            Text(
                                                text = "${r.typeName}  ",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentMint
                                            )
                                            Text(
                                                text = "${r.ttl}s",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = SubtextGray
                                            )
                                        }
                                    }
                                    Text(
                                        text = "  └─> ${r.data}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentMint,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                is DnsResolverResult.Failure -> {
                    Column {
                        Text(
                            text = "RESOLVER PROTOCOL FAILURE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.errorMessage,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogsDashboardCard(
    logs: List<QueryLog>,
    onClearLogs: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, MutedNavy.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historical Query Logs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = onClearLogs,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear lookup logs",
                            tint = SubtextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Trace log latency metrics of recently resolved queries.",
                fontSize = 11.sp,
                color = SubtextGray
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MutedNavy,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recorded log transactions.",
                            color = SubtextGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.forEach { log ->
                        QueryLogItemRow(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun QueryLogItemRow(log: QueryLog) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGrayContainer, shape = RoundedCornerShape(12.dp))
            .border(1.dp, MutedNavy.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (log.responseCode == "NOERROR") SuccessGreen.copy(alpha = 0.15f)
                                        else ErrorRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.responseCode,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.responseCode == "NOERROR") SuccessGreen else ErrorRed
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.domainName,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "${log.latencyMs}ms",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (log.latencyMs < 50) SuccessGreen else if (log.latencyMs < 150) WarningAmber else ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${log.serverName} ",
                        fontSize = 11.sp,
                        color = SubtextGray
                    )
                    Box(
                        modifier = Modifier
                            .background(MutedNavy.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = log.serverType,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentMint
                        )
                    }
                }

                Text(
                    text = "Record: [${log.queryType.uppercase()}]",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AccentMint
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-nest parsed answers description
            Text(
                text = log.answerSection,
                color = SubtextGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// 1.getWeight() is a helper to allow compilation without warnings on older configurations
private fun Int.getWeight(): Float = this.toFloat()
