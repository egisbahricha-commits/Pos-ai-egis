package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.SaleEntity
import com.example.data.repository.CartItem
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BadgeType {
    NORMAL, SUCCESS, DANGER, WARNING, INFO
}

@Composable
fun DesignStatusBadge(
    text: String,
    type: BadgeType = BadgeType.NORMAL,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        BadgeType.NORMAL -> SurfaceStrong to Slate700
        BadgeType.SUCCESS -> Emerald700 to Color.White
        BadgeType.DANGER -> Rose500 to Color.White
        BadgeType.WARNING -> Amber400 to Slate900
        BadgeType.INFO -> AccentBlue to Color.White
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DesignSegmentedTabs(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceStrong,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = selectedKey == key
                Surface(
                    onClick = { onSelect(key) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) SurfaceWhite else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Slate900 else Slate500,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DesignFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) ChipActive else SurfaceWhite,
        border = BorderStroke(1.dp, if (selected) ChipActive else BorderDefault),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else Slate700,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
fun DesignLabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    prefixText: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Slate700,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(color = Slate400)) }
            } else null,
            prefix = if (prefixText != null) {
                {
                    Text(
                        prefixText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Slate500,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            } else null,
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (enabled) SurfaceWhite else SurfaceStrong,
                unfocusedContainerColor = if (enabled) SurfaceWhite else SurfaceStrong,
                disabledContainerColor = SurfaceStrong,
                focusedBorderColor = AccentBlue,
                unfocusedBorderColor = BorderInput,
                errorBorderColor = Rose500,
                disabledBorderColor = BorderDefault,
                focusedTextColor = Slate900,
                unfocusedTextColor = Slate900,
                disabledTextColor = Slate500
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Rose500,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Rose500,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun DesignEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    ctaText: String? = null,
    onCtaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Slate400,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Slate900
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 13.sp),
            textAlign = TextAlign.Center
        )
        if (ctaText != null && onCtaClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCtaClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(ctaText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun DesignToastSnackbar(snackbarData: SnackbarData) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate800,
        shadowElevation = 4.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snackbarData.visuals.message,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 13.sp),
                modifier = Modifier.weight(1f)
            )
            snackbarData.visuals.actionLabel?.let { actionLabel ->
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.clickable { snackbarData.performAction() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasGoTopBar(
    title: String,
    activeRole: String,
    onMenuClick: () -> Unit,
    cartItemCount: Int = 0,
    onCartClick: (() -> Unit)? = null,
    headerColor: Color = ModuleBeranda,
    icon: ImageVector = Icons.Default.PointOfSale,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick, modifier = Modifier.testTag("menu_button")) {
                Icon(Icons.Default.Menu, contentDescription = "Menu Navigasi", tint = Color.White)
            }
        },
        actions = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = activeRole,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (actionIcon != null && onActionClick != null) {
                IconButton(onClick = onActionClick) {
                    Icon(actionIcon, contentDescription = null, tint = Color.White)
                }
            }

            if (onCartClick != null) {
                IconButton(onClick = onCartClick, modifier = Modifier.testTag("topbar_cart_button")) {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    containerColor = Color.White,
                                    contentColor = headerColor
                                ) {
                                    Text("$cartItemCount", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Keranjang", tint = Color.White)
                    }
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifikasi", tint = Color.White)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = headerColor,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun KasGoListRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = ModulePengaturan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingBadge: String? = null,
    trailingValue: String? = null,
    badgeColor: Color = AccentBlue,
    badgeType: BadgeType? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, BorderDefault),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceStrong),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trailingValue != null || trailingBadge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (trailingValue != null) {
                        Text(
                            text = trailingValue,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                        )
                    }
                    if (trailingBadge != null) {
                        val resolvedType = badgeType ?: when {
                            badgeColor == Emerald600 || badgeColor == Emerald700 -> BadgeType.SUCCESS
                            badgeColor == Rose500 -> BadgeType.DANGER
                            badgeColor == Amber500 || badgeColor == Amber400 -> BadgeType.WARNING
                            badgeColor == AccentBlue || badgeColor == HeaderBlue -> BadgeType.INFO
                            else -> BadgeType.NORMAL
                        }
                        if (trailingValue != null) Spacer(modifier = Modifier.height(2.dp))
                        DesignStatusBadge(text = trailingBadge, type = resolvedType)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Buka",
                tint = Slate400,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
fun KasGoStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = AccentBlue,
    modifier: Modifier = Modifier,
    trendText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, BorderDefault),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (trendText != null) {
                    DesignStatusBadge(text = trendText, type = BadgeType.SUCCESS)
                }
            }
        }
    }
}

@Composable
fun KasGoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cari...",
    onBarcodeScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(color = Slate400)) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Cari", tint = Slate500)
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Slate400)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceStrong)
                        .clickable { onBarcodeScanClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = Slate700,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = BorderInput
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun KasGoSectionHeader(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )
            }
        }
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                ),
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun ReceiptDialog(
    sale: SaleEntity,
    items: List<CartItem>,
    onDismiss: () -> Unit,
    onShareWhatsApp: (receiptText: String) -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(sale.date))

    val receiptText = buildString {
        append("🧾 *STRUK PEMBELIAN GERAI UNA*\n")
        append("Creator: Egis Bahricha\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("No. Faktur: ${sale.invoiceNumber}\n")
        append("Waktu     : $dateStr\n")
        append("Kasir     : ${sale.cashierName}\n")
        append("Pelanggan : ${sale.customerName}\n")
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        items.forEach { item ->
            append("${item.productName}\n")
            append("  ${item.quantity.toInt()} x Rp ${formatRupiah(item.unitPrice)} = Rp ${formatRupiah(item.quantity * item.unitPrice)}\n")
        }
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("Subtotal   : Rp ${formatRupiah(sale.subtotal)}\n")
        if (sale.discount > 0) append("Diskon     : -Rp ${formatRupiah(sale.discount)}\n")
        if (sale.tax > 0) append("Pajak      : +Rp ${formatRupiah(sale.tax)}\n")
        append("*TOTAL     : Rp ${formatRupiah(sale.finalAmount)}*\n")
        append("Metode     : ${sale.paymentMethod}\n")
        if (sale.paymentMethod == "TUNAI") {
            append("Bayar      : Rp ${formatRupiah(sale.amountPaid)}\n")
            append("Kembalian  : Rp ${formatRupiah(sale.changeAmount)}\n")
        }
        append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        append("Terima kasih telah berbelanja di Gerai Una!\n")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Struk Transaksi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                    }
                }

                // Thermal Monospace Receipt Ticket (Matches Design System Screenshot)
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderDefault),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "GERAI UNA SMART POS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Slate900,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${sale.invoiceNumber} • $dateStr",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${item.quantity.toInt()}x ${item.productName}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    formatRupiah(item.quantity * item.unitPrice),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Slate900
                                )
                            }
                        }

                        Text(
                            "--------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        if (sale.discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diskon", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Rose500)
                                Text("-${formatRupiah(sale.discount)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Rose500)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                            Text(formatRupiah(sale.finalAmount), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                        }
                        if (sale.paymentMethod == "TUNAI" || sale.paymentMethod == "SPLIT") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bayar", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate700)
                                Text(formatRupiah(sale.amountPaid), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate700)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kembali", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate700)
                                Text(formatRupiah(sale.changeAmount), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate700)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Metode", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate700)
                                Text(sale.paymentMethod, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Slate700)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, BorderInput),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup", color = Slate700, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onShareWhatsApp(receiptText) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("share_whatsapp_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim WhatsApp", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PinPadDialog(
    title: String = "Masukkan PIN",
    expectedPin: String = "123456",
    onVerifyPin: ((String) -> Boolean)? = null,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate400)
                    }
                }

                // 6-Dot PIN Indicator (Matches Design System)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> Rose500
                                        isFilled -> AccentBlue
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isError) Rose500 else if (isFilled) AccentBlue else BorderInput,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isError) "PIN salah. Coba lagi." else title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isError) Rose500 else Slate700,
                        fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Number Grid
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowKeys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            rowKeys.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(56.dp))
                                } else {
                                    Surface(
                                        onClick = {
                                            isError = false
                                            if (key == "DEL") {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            } else if (enteredPin.length < 6) {
                                                enteredPin += key
                                                if (enteredPin.length == 6) {
                                                    val isValid = onVerifyPin?.invoke(enteredPin) ?: (enteredPin == expectedPin)
                                                    if (isValid) {
                                                        onSuccess()
                                                    } else {
                                                        isError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = SurfaceStrong,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (key == "DEL") {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Backspace,
                                                    contentDescription = "Hapus",
                                                    tint = Slate700,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = key,
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Slate900
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "PIN default kasir: 123456",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                )
            }
        }
    }
}

// Backwards compatibility aliases
@Composable
fun GeraiUnaTopBar(
    title: String,
    activeRole: String,
    onMenuClick: () -> Unit,
    cartItemCount: Int = 0,
    onCartClick: (() -> Unit)? = null
) {
    KasGoTopBar(title, activeRole, onMenuClick, cartItemCount, onCartClick)
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color = SurfaceWhite,
    accentColor: Color = AccentBlue,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    KasGoStatCard(title, value, subtitle, icon, accentColor, modifier, null, onClick)
}

@Composable
fun BarcodeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Cari produk atau scan barcode...",
    onBarcodeScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KasGoSearchBar(query, onQueryChange, placeholder, onBarcodeScanClick, modifier)
}

fun formatRupiah(amount: Double): String {
    return NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amount.toLong())
}
