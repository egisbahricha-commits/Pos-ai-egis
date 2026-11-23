package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firebase.LicenseAccessStatus
import com.example.firebase.LicenseUiState
import com.example.ui.components.DesignToastSnackbar
import com.example.ui.components.KasLiteTopBar
import com.example.ui.components.PrinterManagerDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.formatRupiah
import com.example.ui.screens.AIChatScreen
import com.example.ui.screens.BarcodeScanCartScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtReceivableScreen
import com.example.ui.screens.ExpensesIncomesScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ManualProductManagementScreen
import com.example.ui.screens.MultiKasScreen
import com.example.ui.screens.PosCashierScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.PurchaseScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.ui.screens.SemuaMenuScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockInventoryScreen
import com.example.ui.screens.TarifAdminScreen
import com.example.ui.screens.WhatsAppScreen
import com.example.ui.screens.SuperAdminScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SUPER_ADMIN_EMAIL
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.isScreenAllowedWhenDemoLocked
import kotlinx.coroutines.launch

import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            GeraiUnaTheme(darkTheme = darkTheme) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncCustomerLicenseSilently(force = true)
    }
}

data class DrawerMenuItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector,
    val sectionHeader: String? = null
)

private enum class TabletSidebarMode {
    EXPANDED, MINIMIZED, HIDDEN
}

@Composable
fun MainAppContent(viewModel: PosViewModel) {
    val context = LocalContext.current
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val licenseUiState by viewModel.licenseUiState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val menuPermissions by viewModel.menuPermissions.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val lastCompletedSale by viewModel.lastCompletedSale.collectAsStateWithLifecycle()
    val lastCompletedSaleItems by viewModel.lastCompletedSaleItems.collectAsStateWithLifecycle()
    val printers by viewModel.printers.collectAsStateWithLifecycle()
    val activePrinter by viewModel.activePrinter.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    if (!authUiState.hasEnteredApp) {
        LoginScreen(viewModel = viewModel)
        return
    }

    val isProductOnlyLocked = !authUiState.isSuperAdmin && licenseUiState.isLockedToProductOnly
    LaunchedEffect(isProductOnlyLocked, authUiState.isSuperAdmin, currentScreen) {
        if (isProductOnlyLocked && !isScreenAllowedWhenDemoLocked(currentScreen)) {
            viewModel.navigateTo(Screen.PRODUK)
        }
    }

    LaunchedEffect(authUiState.hasEnteredApp, authUiState.isSuperAdmin) {
        if (authUiState.hasEnteredApp && !authUiState.isSuperAdmin) {
            while (true) {
                kotlinx.coroutines.delay(45_000L)
                viewModel.syncCustomerLicenseSilently()
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var tabletSidebarMode by remember { mutableStateOf(TabletSidebarMode.MINIMIZED) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showReceiptPrinterManager by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cartBadgeCount = remember(cart) {
        cart.sumOf { kotlin.math.ceil(it.quantity).toInt().coerceAtLeast(1) }
    }
    val debtorCount = remember(customers) { customers.count { it.totalReceivable > 0.0 } }
    val notificationCount = lowStockProducts.size + debtorCount

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            viewModel.clearUserMessage()
            val action = if (msg == "Produk dihapus") "Urungkan" else null
            val res = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = action,
                duration = SnackbarDuration.Short
            )
            if (res == SnackbarResult.ActionPerformed && msg == "Produk dihapus") {
                viewModel.undoDeleteProduct()
            }
        }
    }

    val homeFallbackScreen = if (isProductOnlyLocked) Screen.PRODUK else Screen.DASHBOARD
    BackHandler(enabled = drawerState.isOpen || currentScreen != homeFallbackScreen) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentScreen != homeFallbackScreen) {
            viewModel.navigateTo(homeFallbackScreen)
        }
    }

    val menuItems = remember {
        listOf(
            DrawerMenuItem(Screen.DASHBOARD, "Dashboard Utama", Icons.Default.Dashboard, "PENJUALAN"),
            DrawerMenuItem(Screen.POS_KASIR, "Kasir POS", Icons.Default.PointOfSale),
            DrawerMenuItem(Screen.SCAN_BARCODE, "Scan Barcode Kasir", Icons.Default.QrCodeScanner),
            DrawerMenuItem(Screen.RIWAYAT_TRANSAKSI, "Riwayat & Retur", Icons.AutoMirrored.Filled.ReceiptLong),
            DrawerMenuItem(Screen.PRODUK, "Daftar Produk", Icons.Default.Category, "PRODUK & INVENTARIS"),
            DrawerMenuItem(Screen.MANAJEMEN_PRODUK, "Kelola Produk Manual", Icons.Default.Edit),
            DrawerMenuItem(Screen.PEMBELIAN, "Pembelian Barang", Icons.Default.ShoppingBag),
            DrawerMenuItem(Screen.STOK_INVENTARIS, "Stok & Opname", Icons.Default.Inventory),
            DrawerMenuItem(Screen.MULTI_KAS, "Multi Kas (Bank & Pulsa)", Icons.Default.AccountBalanceWallet, "KEUANGAN"),
            DrawerMenuItem(Screen.TARIF_ADMIN, "Tarif Admin Konter PPOB", Icons.Default.CreditCard),
            DrawerMenuItem(Screen.KEUANGAN, "Buku Kas & Arus Kas", Icons.Default.AccountBalanceWallet),
            DrawerMenuItem(Screen.PENGELUARAN, "Pengeluaran & Jasa", Icons.Default.MoneyOff),
            DrawerMenuItem(Screen.UTANG_PIUTANG, "Utang & Piutang", Icons.Default.CreditCard),
            DrawerMenuItem(Screen.LAPORAN, "Laporan & Analitik (24 Lap)", Icons.Default.QueryStats, "LAPORAN"),
            DrawerMenuItem(Screen.AI_CHAT, "AI Chat KASLITE", Icons.Default.AutoAwesome, "AI & WHATSAPP"),
            DrawerMenuItem(Screen.WHATSAPP, "WhatsApp Integration", Icons.Default.Sms),
            DrawerMenuItem(Screen.SEMUA_MENU, "Semua Menu", Icons.Default.MoreHoriz, "NAVIGASI"),
            DrawerMenuItem(Screen.KONTAK, "Pelanggan & Pemasok", Icons.Default.Contacts),
            DrawerMenuItem(Screen.PENGATURAN, "Pengaturan & Backup", Icons.Default.Settings)
        )
    }

    val currentTitle = when (currentScreen) {
        Screen.DASHBOARD -> "Beranda"
        Screen.POS_KASIR -> "Kasir"
        Screen.SCAN_BARCODE -> "Scan Barcode"
        Screen.RIWAYAT_TRANSAKSI -> "Riwayat Transaksi"
        Screen.PRODUK -> "Produk"
        Screen.MANAJEMEN_PRODUK -> "Kelola Produk Manual"
        Screen.PEMBELIAN, Screen.PENERIMAAN_BARANG -> "Pembelian Barang"
        Screen.STOK_INVENTARIS -> "Stok & Inventaris"
        Screen.MULTI_KAS -> "Multi Kas"
        Screen.TARIF_ADMIN -> "Tarif Admin"
        Screen.PENGELUARAN, Screen.PENDAPATAN -> "Pengeluaran & Jasa"
        Screen.UTANG_PIUTANG -> "Utang & Piutang"
        Screen.KEUANGAN -> "Keuangan & Kas"
        Screen.LAPORAN -> "Laporan"
        Screen.AI_CHAT -> "AI Chat KASLITE"
        Screen.WHATSAPP -> "WhatsApp Command"
        Screen.KONTAK -> "Pelanggan & Pemasok"
        Screen.PENGATURAN -> "Pengaturan"
        Screen.SEMUA_MENU -> "Semua Menu"
        Screen.SUPER_ADMIN -> "Admin Pusat"
    }

    val (headerColor, headerIcon) = when (currentScreen) {
        Screen.DASHBOARD -> ModuleBeranda to Icons.Default.Dashboard
        Screen.POS_KASIR, Screen.RIWAYAT_TRANSAKSI -> ModuleKasir to Icons.Default.PointOfSale
        Screen.SCAN_BARCODE -> ModuleKasir to Icons.Default.QrCodeScanner
        Screen.PRODUK, Screen.MANAJEMEN_PRODUK, Screen.PEMBELIAN, Screen.PENERIMAAN_BARANG, Screen.STOK_INVENTARIS -> ModuleProduk to Icons.Default.Inventory2
        Screen.LAPORAN -> ModuleLaporan to Icons.Default.QueryStats
        Screen.MULTI_KAS -> ModuleKas to Icons.Default.AccountBalanceWallet
        Screen.TARIF_ADMIN -> ModuleKasir to Icons.Default.CreditCard
        Screen.AI_CHAT, Screen.WHATSAPP -> ModuleKasir to Icons.Default.AutoAwesome
        Screen.KEUANGAN, Screen.PENGELUARAN, Screen.PENDAPATAN,
        Screen.UTANG_PIUTANG, Screen.KONTAK, Screen.PENGATURAN, Screen.SEMUA_MENU -> ModulePengaturan to Icons.Default.Settings
        Screen.SUPER_ADMIN -> ModulePengaturan to Icons.Default.AdminPanelSettings
    }

    val storeDisplayName = storeProfile?.name?.ifBlank { "KASLITE" } ?: "KASLITE"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isTablet || drawerState.isOpen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = SurfaceWhite,
                    modifier = Modifier.widthIn(min = 240.dp, max = 290.dp)
                ) {
                    // Drawer Header with Explicit Minimize / Close Button
                    Surface(
                        color = ModuleBeranda,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Store, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            storeDisplayName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            activeRole.ifBlank { "Kasir POS" },
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                                        )
                                    }
                                }

                                // Explicit minimize / close drawer button
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.close() } },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("minimize_drawer_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuOpen,
                                        contentDescription = "Minimalkan menu kiri",
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = if (authUiState.isSuperAdmin)
                                            "Super Admin: ${authUiState.userEmail}"
                                        else
                                            "Kasir Lokal: $activeUserName (${licenseUiState.planType})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = Color.White),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        viewModel.logoutToLoginScreen()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Keluar ke Login",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Slate200)

                    // Drawer Links List
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(menuItems) { item ->
                            if (item.sectionHeader != null) {
                                Text(
                                    text = item.sectionHeader,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate400,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp)
                                )
                            }

                            val isSelected = currentScreen == item.screen
                            val isAllowed = remember(activeRole, activeUserName, menuPermissions, licenseUiState, authUiState.isSuperAdmin, item.screen) {
                                viewModel.hasMenuPermission(item.screen)
                            }
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            item.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!isAllowed) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = "Dibatasi",
                                                tint = Slate400,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Slate600
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.navigateTo(item.screen)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = AccentBlue,
                                    unselectedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                                    .alpha(if (isAllowed) 1f else 0.55f)
                            )
                        }
                    }
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // TABLET COLLAPSIBLE / MINIMIZABLE LEFT SIDEBAR
                if (isTablet) {
                    val sidebarWidth by animateDpAsState(
                        targetValue = when (tabletSidebarMode) {
                            TabletSidebarMode.EXPANDED -> 252.dp
                            TabletSidebarMode.MINIMIZED -> 76.dp
                            TabletSidebarMode.HIDDEN -> 0.dp
                        },
                        label = "sidebarWidth"
                    )

                    AnimatedVisibility(
                        visible = tabletSidebarMode != TabletSidebarMode.HIDDEN,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        Row(modifier = Modifier.fillMaxHeight()) {
                            if (tabletSidebarMode == TabletSidebarMode.EXPANDED) {
                                // Full Expanded Collapsible Left Sidebar
                                Column(
                                    modifier = Modifier
                                        .width(sidebarWidth)
                                        .fillMaxHeight()
                                        .background(SurfaceWhite)
                                ) {
                                    Surface(color = ModuleBeranda, modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Store, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    storeDisplayName,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { tabletSidebarMode = TabletSidebarMode.MINIMIZED },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("collapse_sidebar_button")
                                            ) {
                                                Icon(Icons.Default.ChevronLeft, contentDescription = "Minimalkan sidebar", tint = Color.White)
                                            }
                                        }
                                    }

                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(menuItems) { item ->
                                            if (item.sectionHeader != null) {
                                                Text(
                                                    text = item.sectionHeader,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate400, fontSize = 10.sp),
                                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                                                )
                                            }
                                            val isSelected = currentScreen == item.screen
                                            val isAllowed = remember(activeRole, activeUserName, menuPermissions, licenseUiState, authUiState.isSuperAdmin, item.screen) {
                                                viewModel.hasMenuPermission(item.screen)
                                            }
                                            Surface(
                                                onClick = { viewModel.navigateTo(item.screen) },
                                                color = if (isSelected) AccentBlue else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    .alpha(if (isAllowed) 1f else 0.55f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        item.icon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.White else Slate600,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        item.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) Color.White else Slate700
                                                        ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (!isAllowed) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(
                                                            Icons.Default.Lock,
                                                            contentDescription = "Dibatasi",
                                                            tint = Slate400,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Compact Minimized Icon Rail
                                NavigationRail(
                                    containerColor = SurfaceWhite,
                                    header = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(ModuleBeranda)
                                                    .clickable { tabletSidebarMode = TabletSidebarMode.EXPANDED },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.ChevronRight, contentDescription = "Perluas menu kiri", tint = Color.White, modifier = Modifier.size(22.dp))
                                            }
                                            IconButton(
                                                onClick = { tabletSidebarMode = TabletSidebarMode.HIDDEN },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.MenuOpen, contentDescription = "Sembunyikan menu kiri", tint = Slate500, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.DASHBOARD,
                                        onClick = { viewModel.navigateTo(Screen.DASHBOARD) },
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Beranda") },
                                        label = { Text("Beranda", fontSize = 10.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = AccentBlue,
                                            selectedTextColor = AccentBlue,
                                            indicatorColor = AccentBlueSoft
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.POS_KASIR,
                                        onClick = { viewModel.navigateTo(Screen.POS_KASIR) },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (cartBadgeCount > 0) {
                                                        Badge(containerColor = AccentBlue, contentColor = Color.White) {
                                                            Text("$cartBadgeCount")
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.PointOfSale, contentDescription = "Kasir")
                                            }
                                        },
                                        label = { Text("Kasir", fontSize = 10.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = AccentBlue,
                                            selectedTextColor = AccentBlue,
                                            indicatorColor = AccentBlueSoft
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.RIWAYAT_TRANSAKSI,
                                        onClick = { viewModel.navigateTo(Screen.RIWAYAT_TRANSAKSI) },
                                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Riwayat") },
                                        label = { Text("Riwayat", fontSize = 10.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = AccentBlue,
                                            selectedTextColor = AccentBlue,
                                            indicatorColor = AccentBlueSoft
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.PRODUK,
                                        onClick = { viewModel.navigateTo(Screen.PRODUK) },
                                        icon = { Icon(Icons.Default.Category, contentDescription = "Produk") },
                                        label = { Text("Produk", fontSize = 10.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = AccentBlue,
                                            selectedTextColor = AccentBlue,
                                            indicatorColor = AccentBlueSoft
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.LAPORAN,
                                        onClick = { viewModel.navigateTo(Screen.LAPORAN) },
                                        icon = { Icon(Icons.Default.QueryStats, contentDescription = "Laporan") },
                                        label = { Text("Laporan", fontSize = 10.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = AccentBlue,
                                            selectedTextColor = AccentBlue,
                                            indicatorColor = AccentBlueSoft
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.AI_CHAT,
                                        onClick = { viewModel.navigateTo(Screen.AI_CHAT) },
                                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
                                        label = { Text("AI Chat", fontSize = 10.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = ModuleKasir,
                                            selectedTextColor = ModuleKasir,
                                            indicatorColor = Emerald100
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentScreen == Screen.SEMUA_MENU,
                                        onClick = { viewModel.navigateTo(Screen.SEMUA_MENU) },
                                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Menu") },
                                        label = { Text("Menu", fontSize = 10.sp) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = AccentBlue,
                                            selectedTextColor = AccentBlue,
                                            indicatorColor = AccentBlueSoft
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(BorderDefault)
                            )
                        }
                    }
                }

                // Main App Area
                Scaffold(
                    topBar = {
                        KasLiteTopBar(
                            title = currentTitle,
                            activeRole = activeRole,
                            onMenuClick = {
                                if (isTablet) {
                                    tabletSidebarMode = when (tabletSidebarMode) {
                                        TabletSidebarMode.EXPANDED -> TabletSidebarMode.MINIMIZED
                                        TabletSidebarMode.MINIMIZED -> TabletSidebarMode.HIDDEN
                                        TabletSidebarMode.HIDDEN -> TabletSidebarMode.EXPANDED
                                    }
                                } else {
                                    coroutineScope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                }
                            },
                            cartItemCount = cartBadgeCount,
                            onCartClick = { viewModel.navigateTo(Screen.POS_KASIR) },
                            headerColor = headerColor,
                            icon = headerIcon,
                            notificationCount = notificationCount,
                            onNotificationClick = { showNotificationsDialog = true }
                        )
                    },
                    bottomBar = {
                        if (!isTablet) {
                            NavigationBar(
                                containerColor = SurfaceWhite,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.DASHBOARD,
                                    onClick = { viewModel.navigateTo(Screen.DASHBOARD) },
                                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Beranda") },
                                    label = { Text("Beranda", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentBlue,
                                        selectedTextColor = AccentBlue,
                                        unselectedIconColor = Slate400,
                                        unselectedTextColor = Slate500,
                                        indicatorColor = AccentBlueSoft
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen == Screen.POS_KASIR,
                                    onClick = { viewModel.navigateTo(Screen.POS_KASIR) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (cartBadgeCount > 0) {
                                                    Badge(containerColor = AccentBlue, contentColor = Color.White) {
                                                        Text("$cartBadgeCount", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.PointOfSale, contentDescription = "Kasir")
                                        }
                                    },
                                    label = { Text("Kasir", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentBlue,
                                        selectedTextColor = AccentBlue,
                                        unselectedIconColor = Slate400,
                                        unselectedTextColor = Slate500,
                                        indicatorColor = AccentBlueSoft
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen == Screen.RIWAYAT_TRANSAKSI,
                                    onClick = { viewModel.navigateTo(Screen.RIWAYAT_TRANSAKSI) },
                                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Riwayat") },
                                    label = { Text("Riwayat", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentBlue,
                                        selectedTextColor = AccentBlue,
                                        unselectedIconColor = Slate400,
                                        unselectedTextColor = Slate500,
                                        indicatorColor = AccentBlueSoft
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen == Screen.LAPORAN,
                                    onClick = { viewModel.navigateTo(Screen.LAPORAN) },
                                    icon = { Icon(Icons.Default.QueryStats, contentDescription = "Laporan") },
                                    label = { Text("Laporan", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentBlue,
                                        selectedTextColor = AccentBlue,
                                        unselectedIconColor = Slate400,
                                        unselectedTextColor = Slate500,
                                        indicatorColor = AccentBlueSoft
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentScreen == Screen.SEMUA_MENU,
                                    onClick = { viewModel.navigateTo(Screen.SEMUA_MENU) },
                                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Menu") },
                                    label = { Text("Menu", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AccentBlue,
                                        selectedTextColor = AccentBlue,
                                        unselectedIconColor = Slate400,
                                        unselectedTextColor = Slate500,
                                        indicatorColor = AccentBlueSoft
                                    )
                                )
                            }
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            DesignToastSnackbar(snackbarData = data)
                        }
                    },
                    containerColor = BgCanvas,
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (!authUiState.isSuperAdmin && licenseUiState.isDemoActive) {
                            val daysLeft = ((licenseUiState.remainingMillis.coerceAtLeast(0L) + 86_399_999L) / 86_400_000L)
                            Surface(
                                color = Sapphire50,
                                border = BorderStroke(1.dp, Sapphire200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Mode Demo 3 Hari Aktif • Sisa $daysLeft hari",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Sapphire800
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { viewModel.navigateTo(Screen.PENGATURAN) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("Aktivasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (isProductOnlyLocked) {
                            Surface(
                                color = Rose100,
                                border = BorderStroke(1.dp, Rose500.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Rose500, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (licenseUiState.accessStatus == LicenseAccessStatus.DEMO_EXPIRED)
                                                "Demo 3 Hari Berakhir • Hanya Menu Produk & Pengaturan Aktif"
                                            else
                                                "Akun Terkunci • Hanya Menu Produk & Pengaturan Aktif",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Rose500
                                            ),
                                            maxLines = 2
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.navigateTo(Screen.PENGATURAN) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("Aktivasi / Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Rose500)
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (isProductOnlyLocked && !isScreenAllowedWhenDemoLocked(currentScreen)) {
                                DemoExpiredLockView(
                                    licenseUiState = licenseUiState,
                                    onOpenProducts = { viewModel.navigateTo(Screen.PRODUK) },
                                    onOpenManualProducts = { viewModel.navigateTo(Screen.MANAJEMEN_PRODUK) },
                                    onOpenSettings = { viewModel.navigateTo(Screen.PENGATURAN) }
                                )
                            } else {
                                when (currentScreen) {
                                    Screen.DASHBOARD -> DashboardScreen(viewModel)
                                    Screen.POS_KASIR -> PosCashierScreen(viewModel)
                                    Screen.SCAN_BARCODE -> BarcodeScanCartScreen(viewModel)
                                    Screen.RIWAYAT_TRANSAKSI -> SalesHistoryScreen(viewModel)
                                    Screen.SEMUA_MENU -> SemuaMenuScreen(viewModel)
                                    Screen.PRODUK -> ProductsScreen(viewModel)
                                    Screen.MANAJEMEN_PRODUK -> ManualProductManagementScreen(viewModel)
                                    Screen.PEMBELIAN, Screen.PENERIMAAN_BARANG -> PurchaseScreen(viewModel)
                                    Screen.STOK_INVENTARIS -> StockInventoryScreen(viewModel)
                                    Screen.MULTI_KAS -> MultiKasScreen(viewModel)
                                    Screen.TARIF_ADMIN -> TarifAdminScreen(viewModel)
                                    Screen.PENGELUARAN, Screen.PENDAPATAN -> ExpensesIncomesScreen(viewModel)
                                    Screen.UTANG_PIUTANG -> DebtReceivableScreen(viewModel)
                                    Screen.KEUANGAN -> FinanceScreen(viewModel)
                                    Screen.LAPORAN -> ReportsScreen(viewModel)
                                    Screen.AI_CHAT -> AIChatScreen(viewModel)
                                    Screen.WHATSAPP -> WhatsAppScreen(viewModel)
                                    Screen.KONTAK -> ContactsScreen(viewModel)
                                    Screen.PENGATURAN -> SettingsScreen(viewModel)
                                    Screen.SUPER_ADMIN -> SuperAdminScreen(viewModel) { viewModel.navigateTo(Screen.PENGATURAN) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsDialog) {
        Dialog(onDismissRequest = { showNotificationsDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Notifikasi Toko ($notificationCount)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    if (notificationCount == 0) {
                        Text(
                            "Semua stok produk aman dan tidak ada piutang jatuh tempo.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )
                    } else {
                        if (lowStockProducts.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    showNotificationsDialog = false
                                    viewModel.navigateTo(Screen.STOK_INVENTARIS)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = AmberWarningBg,
                                border = BorderStroke(1.dp, AmberWarningBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "${lowStockProducts.size} Produk Stok Menipis / Habis",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                    )
                                    Text(
                                        lowStockProducts.take(3).joinToString(", ") { it.name },
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (debtorCount > 0) {
                            val totalReceivable = customers.sumOf { it.totalReceivable }
                            Surface(
                                onClick = {
                                    showNotificationsDialog = false
                                    viewModel.navigateTo(Screen.UTANG_PIUTANG)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Rose100,
                                border = BorderStroke(1.dp, Rose500.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "$debtorCount Pelanggan Memiliki Kasbon",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Rose500)
                                    )
                                    Text(
                                        "Total piutang aktif: Rp ${formatRupiah(totalReceivable)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        onClick = { showNotificationsDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        color = AccentBlue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("Tutup", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    lastCompletedSale?.let { completedSale ->
        ReceiptDialog(
            sale = completedSale,
            items = lastCompletedSaleItems,
            storeProfile = storeProfile,
            activePrinter = activePrinter,
            onDismiss = { viewModel.dismissReceiptModal() },
            onShareWhatsApp = { text ->
                viewModel.whatsAppCommandCenter.shareViaWhatsApp(context, text)
            },
            onPrintReceipt = {
                viewModel.printSaleReceipt(
                    context = context,
                    sale = completedSale,
                    items = lastCompletedSaleItems
                )
            },
            onPrintSystemPdf = {
                viewModel.printSaleReceipt(
                    context = context,
                    sale = completedSale,
                    items = lastCompletedSaleItems,
                    forceSystemPrint = true
                )
            },
            onOpenPrinterSettings = {
                showReceiptPrinterManager = true
            }
        )
    }

    if (showReceiptPrinterManager) {
        PrinterManagerDialog(
            printers = printers,
            activePrinterId = activePrinter?.id ?: 0L,
            onSelectPrimaryPrinter = { printerId -> viewModel.selectPreferredPrinter(printerId) },
            onSavePrinter = { printer, setAsPrimary -> viewModel.savePrinter(printer, setAsPrimary) },
            onDeletePrinter = { printer -> viewModel.deletePrinter(printer) },
            onTestPrint = { printer, cb -> viewModel.printTestReceipt(context, printer, cb) },
            onDismiss = { showReceiptPrinterManager = false }
        )
    }
}

@Composable
private fun DemoExpiredLockView(
    licenseUiState: LicenseUiState,
    onOpenProducts: () -> Unit,
    onOpenManualProducts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val reasonTitle = when (licenseUiState.accessStatus) {
        LicenseAccessStatus.SUSPENDED -> "Akun Dinonaktifkan Admin Pusat"
        LicenseAccessStatus.DEVICE_MISMATCH -> "Lisensi Terikat Perangkat Lain"
        else -> "Masa Demo 3 Hari Telah Berakhir"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, BorderDefault),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Rose100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Fitur Terkunci",
                        tint = Rose500,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = reasonTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Selama masa demo habis, menu Kasir, Laporan, Keuangan, dan lainnya dikunci. Anda tetap dapat menambah/mengelola produk serta melakukan backup data atau aktivasi lisensi penuh melalui Pengaturan.\n\nHubungi Admin Pusat: $SUPER_ADMIN_EMAIL",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600, lineHeight = 19.sp),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onOpenProducts,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka Daftar Produk", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onOpenManualProducts,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kelola Produk Manual")
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aktivasi Lisensi & Backup (.kaslite)")
                }
            }
        }
    }
}

