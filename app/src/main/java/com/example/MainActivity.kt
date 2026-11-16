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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DesignToastSnackbar
import com.example.ui.components.KasGoTopBar
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.AIChatScreen
import com.example.ui.screens.CloudMonitorScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtReceivableScreen
import com.example.ui.screens.ExpensesIncomesScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.LoginScreen
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeraiUnaTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
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
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val activeUserName by viewModel.activeUserName.collectAsStateWithLifecycle()
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val menuPermissions by viewModel.menuPermissions.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val lastCompletedSale by viewModel.lastCompletedSale.collectAsStateWithLifecycle()
    val lastCompletedSaleItems by viewModel.lastCompletedSaleItems.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    if (!authUiState.hasEnteredApp) {
        LoginScreen(viewModel = viewModel)
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var tabletSidebarMode by remember { mutableStateOf(TabletSidebarMode.MINIMIZED) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    BackHandler(enabled = drawerState.isOpen || currentScreen != Screen.DASHBOARD) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentScreen != Screen.DASHBOARD) {
            viewModel.navigateTo(Screen.DASHBOARD)
        }
    }

    val menuItems = remember {
        listOf(
            DrawerMenuItem(Screen.DASHBOARD, "Dashboard Utama", Icons.Default.Dashboard, "PENJUALAN"),
            DrawerMenuItem(Screen.POS_KASIR, "Kasir POS", Icons.Default.PointOfSale),
            DrawerMenuItem(Screen.RIWAYAT_TRANSAKSI, "Riwayat & Retur", Icons.AutoMirrored.Filled.ReceiptLong),
            DrawerMenuItem(Screen.PRODUK, "Daftar Produk", Icons.Default.Category, "PRODUK & INVENTARIS"),
            DrawerMenuItem(Screen.PEMBELIAN, "Pembelian Barang", Icons.Default.ShoppingBag),
            DrawerMenuItem(Screen.STOK_INVENTARIS, "Stok & Opname", Icons.Default.Inventory),
            DrawerMenuItem(Screen.MULTI_KAS, "Multi Kas (Bank & Pulsa)", Icons.Default.AccountBalanceWallet, "KEUANGAN"),
            DrawerMenuItem(Screen.CLOUD_MONITOR, "Pantau Cloud & Arus Kas", Icons.Default.CloudQueue),
            DrawerMenuItem(Screen.TARIF_ADMIN, "Tarif Admin Konter PPOB", Icons.Default.CreditCard),
            DrawerMenuItem(Screen.KEUANGAN, "Buku Kas & Arus Kas", Icons.Default.AccountBalanceWallet),
            DrawerMenuItem(Screen.PENGELUARAN, "Pengeluaran & Jasa", Icons.Default.MoneyOff),
            DrawerMenuItem(Screen.UTANG_PIUTANG, "Utang & Piutang", Icons.Default.CreditCard),
            DrawerMenuItem(Screen.LAPORAN, "Laporan & Analitik (24 Lap)", Icons.Default.QueryStats, "LAPORAN"),
            DrawerMenuItem(Screen.AI_CHAT, "AI Chat Gerai Una", Icons.Default.AutoAwesome, "AI & WHATSAPP"),
            DrawerMenuItem(Screen.WHATSAPP, "WhatsApp Integration", Icons.Default.Sms),
            DrawerMenuItem(Screen.SEMUA_MENU, "Semua Menu", Icons.Default.MoreHoriz, "NAVIGASI"),
            DrawerMenuItem(Screen.KONTAK, "Pelanggan & Pemasok", Icons.Default.Contacts),
            DrawerMenuItem(Screen.PENGATURAN, "Pengaturan & Role", Icons.Default.Settings)
        )
    }

    val currentTitle = when (currentScreen) {
        Screen.DASHBOARD -> "Beranda"
        Screen.POS_KASIR -> "Kasir"
        Screen.RIWAYAT_TRANSAKSI -> "Riwayat Transaksi"
        Screen.PRODUK -> "Produk"
        Screen.PEMBELIAN, Screen.PENERIMAAN_BARANG -> "Pembelian Barang"
        Screen.STOK_INVENTARIS -> "Stok & Inventaris"
        Screen.MULTI_KAS -> "Multi Kas"
        Screen.CLOUD_MONITOR -> "Pantau Cloud Firebase"
        Screen.TARIF_ADMIN -> "Tarif Admin"
        Screen.PENGELUARAN, Screen.PENDAPATAN -> "Pengeluaran & Jasa"
        Screen.UTANG_PIUTANG -> "Utang & Piutang"
        Screen.KEUANGAN -> "Keuangan & Kas"
        Screen.LAPORAN -> "Laporan"
        Screen.AI_CHAT -> "AI Chat Gerai Una"
        Screen.WHATSAPP -> "WhatsApp Command"
        Screen.KONTAK -> "Pelanggan & Pemasok"
        Screen.PENGATURAN -> "Pengaturan"
        Screen.SEMUA_MENU -> "Semua Menu"
    }

    val (headerColor, headerIcon) = when (currentScreen) {
        Screen.DASHBOARD -> ModuleBeranda to Icons.Default.Dashboard
        Screen.POS_KASIR, Screen.RIWAYAT_TRANSAKSI -> ModuleKasir to Icons.Default.PointOfSale
        Screen.PRODUK, Screen.PEMBELIAN, Screen.PENERIMAAN_BARANG, Screen.STOK_INVENTARIS -> ModuleProduk to Icons.Default.Category
        Screen.LAPORAN -> ModuleLaporan to Icons.Default.QueryStats
        Screen.MULTI_KAS, Screen.CLOUD_MONITOR -> ModuleKas to Icons.Default.CloudDone
        Screen.TARIF_ADMIN -> ModuleKasir to Icons.Default.CreditCard
        Screen.AI_CHAT, Screen.WHATSAPP -> ModuleKasir to Icons.Default.AutoAwesome
        Screen.KEUANGAN, Screen.PENGELUARAN, Screen.PENDAPATAN,
        Screen.UTANG_PIUTANG, Screen.KONTAK, Screen.PENGATURAN, Screen.SEMUA_MENU -> ModulePengaturan to Icons.Default.Settings
    }

    val storeDisplayName = storeProfile?.name?.ifBlank { "GERAI UNA POS" } ?: "GERAI UNA POS"

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
                                            "By Egis Bahricha",
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
                                        text = if (authUiState.isCloudSignedIn) "Cloud: ${authUiState.userEmail}" else "Offline: $activeUserName",
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
                            val isAllowed = remember(activeRole, menuPermissions, item.screen) {
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
                                            Surface(
                                                onClick = { viewModel.navigateTo(item.screen) },
                                                color = if (isSelected) AccentBlue else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
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
                                                        overflow = TextOverflow.Ellipsis
                                                    )
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
                                                    if (cart.isNotEmpty()) {
                                                        Badge(containerColor = AccentBlue, contentColor = Color.White) {
                                                            Text("${cart.sumOf { it.quantity.toInt() }}")
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
                        KasGoTopBar(
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
                            cartItemCount = cart.sumOf { it.quantity.toInt() },
                            onCartClick = { viewModel.navigateTo(Screen.POS_KASIR) },
                            headerColor = headerColor,
                            icon = headerIcon
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
                                                if (cart.isNotEmpty()) {
                                                    Badge(containerColor = AccentBlue, contentColor = Color.White) {
                                                        Text("${cart.sumOf { it.quantity.toInt() }}", fontWeight = FontWeight.Bold)
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(viewModel)
                            Screen.POS_KASIR -> PosCashierScreen(viewModel)
                            Screen.RIWAYAT_TRANSAKSI -> SalesHistoryScreen(viewModel)
                            Screen.SEMUA_MENU -> SemuaMenuScreen(viewModel)
                            Screen.PRODUK -> ProductsScreen(viewModel)
                            Screen.PEMBELIAN, Screen.PENERIMAAN_BARANG -> PurchaseScreen(viewModel)
                            Screen.STOK_INVENTARIS -> StockInventoryScreen(viewModel)
                            Screen.MULTI_KAS -> MultiKasScreen(viewModel)
                            Screen.CLOUD_MONITOR -> CloudMonitorScreen(viewModel)
                            Screen.TARIF_ADMIN -> TarifAdminScreen(viewModel)
                            Screen.PENGELUARAN, Screen.PENDAPATAN -> ExpensesIncomesScreen(viewModel)
                            Screen.UTANG_PIUTANG -> DebtReceivableScreen(viewModel)
                            Screen.KEUANGAN -> FinanceScreen(viewModel)
                            Screen.LAPORAN -> ReportsScreen(viewModel)
                            Screen.AI_CHAT -> AIChatScreen(viewModel)
                            Screen.WHATSAPP -> WhatsAppScreen(viewModel)
                            Screen.KONTAK -> ContactsScreen(viewModel)
                            Screen.PENGATURAN -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    if (lastCompletedSale != null) {
        ReceiptDialog(
            sale = lastCompletedSale!!,
            items = lastCompletedSaleItems,
            onDismiss = { viewModel.dismissReceiptModal() },
            onShareWhatsApp = { text ->
                viewModel.whatsAppCommandCenter.shareViaWhatsApp(context, text)
            }
        )
    }
}
