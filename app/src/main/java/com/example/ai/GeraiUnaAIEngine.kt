package com.example.ai

import com.example.data.entity.ProductAliasEntity
import com.example.data.entity.ProductEntity
import com.example.data.repository.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class AIIntent {
    CREATE_PRODUCT,
    UPDATE_PRODUCT,
    PURCHASE_ORDER,
    RECEIVE_PURCHASE,
    STOCK_IN,
    STOCK_OUT,
    EXPENSE,
    INCOME,
    SALES_QUERY,
    STOCK_QUERY,
    SUPPLIER_QUERY,
    CUSTOMER_QUERY,
    DEBT_QUERY,
    REPORT_QUERY,
    UNKNOWN
}

data class ParsedItem(
    val matchedProduct: ProductEntity?,
    val rawName: String,
    val quantity: Double,
    val unit: String,
    val price: Double
)

data class AIParseResult(
    val intent: AIIntent,
    val rawMessage: String,
    val summary: String,
    val draftJson: String,
    val items: List<ParsedItem> = emptyList(),
    val supplierName: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val notes: String = "",
    val confirmationRequired: Boolean = true,
    val isExecutable: Boolean = true,
    val requiresClarification: Boolean = false,
    val clarificationQuestion: String? = null
)

class GeraiUnaAIEngine(private val repository: PosRepository) {

    suspend fun processCommand(message: String): AIParseResult = withContext(Dispatchers.IO) {
        val trimmed = message.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        val products = repository.allProducts.first()
        val aliases = repository.aliases.first()
        val suppliers = repository.suppliers.first()

        // 1. Check for Query Intents First
        if (isStockQuery(lower)) {
            return@withContext handleStockQuery(lower, products, aliases)
        }
        if (isDebtQuery(lower)) {
            return@withContext handleDebtQuery(lower, suppliers)
        }
        if (isReportQuery(lower) || isSalesQuery(lower)) {
            return@withContext handleReportQuery(lower)
        }

        // 2. Check for Financial Intents (Expense & Income)
        if (isExpenseCommand(lower)) {
            return@withContext handleExpenseCommand(trimmed, lower)
        }
        if (isIncomeCommand(lower)) {
            return@withContext handleIncomeCommand(trimmed, lower)
        }

        // 3. Check for Stock Movement Commands
        if (isStockInCommand(lower)) {
            return@withContext handleStockInCommand(trimmed, lower, products, aliases)
        }

        // 4. Check for Purchase Order / Belanja Toko
        if (isPurchaseCommand(lower)) {
            return@withContext handlePurchaseCommand(trimmed, lower, products, aliases, suppliers)
        }

        // 5. Check for New Product Creation
        if (isCreateProductCommand(lower)) {
            return@withContext handleCreateProductCommand(trimmed, lower, products)
        }

        // Default Unknown intent
        return@withContext AIParseResult(
            intent = AIIntent.UNKNOWN,
            rawMessage = trimmed,
            summary = "Maaf, perintah belum dikenali. Contoh perintah:\n• \"Belanja Aqua 2 dus harga 45 ribu\"\n• \"Tambah produk Rinso cair 800ml modal 14rb jual 18rb\"\n• \"Catat pengeluaran listrik 250 ribu\"\n• \"Tambah stok Indomie 20 pcs\"\n• \"Berapa stok Aqua?\"\n• \"Berapa utang ke grosir?\"",
            draftJson = JSONObject().put("intent", "UNKNOWN").toString(2),
            confirmationRequired = false,
            isExecutable = false
        )
    }

    // --- Intent Detection Helpers ---
    private fun isStockQuery(text: String) =
        text.contains("stok") && (text.contains("berapa") || text.contains("cek") || text.contains("ada berapa") || text.contains("sisa"))

    private fun isDebtQuery(text: String) =
        (text.contains("utang") || text.contains("hutang") || text.contains("piutang") || text.contains("kasbon")) &&
            (text.contains("berapa") || text.contains("cek") || text.contains("ke") || text.contains("siapa") || text.contains("daftar"))

    private fun isReportQuery(text: String) =
        text.contains("laporan") || text.contains("rekap") || (text.contains("total") && (text.contains("penjualan") || text.contains("pembelian") || text.contains("omset")))

    private fun isSalesQuery(text: String) =
        text.contains("penjualan hari ini") || text.contains("omzet") || text.contains("omset")

    private fun isExpenseCommand(text: String) =
        text.contains("pengeluaran") || text.contains("bayar listrik") || text.contains("beli bensin") ||
                text.contains("biaya") || text.contains("keluar uang") || text.contains("ongkos")

    private fun isIncomeCommand(text: String) =
        text.contains("pemasukan") || text.contains("jasa transfer") || text.contains("komisi") || text.contains("uang masuk")

    private fun isStockInCommand(text: String) =
        (text.contains("tambah stok") || text.contains("masuk stok") || text.contains("stok masuk")) && !text.contains("tambah produk baru")

    private fun isPurchaseCommand(text: String) =
        text.contains("belanja") || text.contains("beli barang") || text.contains("faktur") ||
                text.contains("kulakan") || text.contains("pesan ke")

    private fun isCreateProductCommand(text: String) =
        text.contains("tambah produk") || text.contains("buat produk") || text.contains("produk baru")

    // --- Parsers ---

    private fun handleStockQuery(
        lower: String,
        products: List<ProductEntity>,
        aliases: List<ProductAliasEntity>
    ): AIParseResult {
        val queryWord = lower.replace("stok", "")
            .replace("sekarang", "")
            .replace("berapa", "")
            .replace("cek", "")
            .replace("ada", "")
            .replace("sisa", "")
            .replace("?", "")
            .trim()

        val matched = findBestMatchingProduct(queryWord, products, aliases)
        val summary = if (matched != null) {
            "📊 Informasi Stok Aktual:\n*${matched.name}*\n• Stok saat ini: *${matched.stock} ${matched.baseUnit}*\n• Batas minimum: ${matched.minStock} ${matched.baseUnit}\n• Harga jual: Rp ${formatRupiah(matched.sellingPrice)}"
        } else {
            "🔍 Tidak menemukan produk untuk kata kunci \"$queryWord\". Silakan periksa nama produk di modul Produk."
        }

        val json = JSONObject().apply {
            put("intent", "STOCK_QUERY")
            put("query", queryWord)
            put("matched_product", matched?.name ?: "NOT_FOUND")
            put("stock", matched?.stock ?: 0)
        }

        return AIParseResult(
            intent = AIIntent.STOCK_QUERY,
            rawMessage = lower,
            summary = summary,
            draftJson = json.toString(2),
            confirmationRequired = false,
            isExecutable = false
        )
    }

    private suspend fun handleDebtQuery(
        lower: String,
        suppliers: List<com.example.data.entity.SupplierEntity>
    ): AIParseResult {
        val customers = repository.customers.first()
        val askReceivable = lower.contains("piutang") || lower.contains("kasbon") || lower.contains("pelanggan")
        val askSupplierDebt = lower.contains("utang") || lower.contains("hutang") || lower.contains("grosir") || lower.contains("pemasok") || lower.contains("supplier")

        val totalDebt = suppliers.sumOf { it.totalDebt }
        val totalReceivable = customers.sumOf { it.totalReceivable }
        val sb = StringBuilder()

        if (askReceivable && !lower.contains("grosir") && !lower.contains("pemasok") && !lower.contains("supplier")) {
            sb.append("📋 Ringkasan Piutang / Kasbon Pelanggan:\n")
            sb.append("• Total Seluruh Piutang: *Rp ${formatRupiah(totalReceivable)}*\n\n")
            val debtors = customers.filter { it.totalReceivable > 0 }
            if (debtors.isEmpty()) {
                sb.append("Tidak ada pelanggan dengan piutang aktif.")
            } else {
                sb.append("Rincian per pelanggan:\n")
                debtors.forEach { c ->
                    sb.append("• ${c.name}: Rp ${formatRupiah(c.totalReceivable)}\n")
                }
            }
        } else {
            sb.append("📋 Ringkasan Utang Pemasok / Grosir:\n")
            sb.append("• Total Seluruh Utang: *Rp ${formatRupiah(totalDebt)}*\n\n")
            if (suppliers.isEmpty()) {
                sb.append("Tidak ada data pemasok tersimpan.\n")
            } else {
                sb.append("Rincian per pemasok:\n")
                suppliers.forEach { s ->
                    sb.append("• ${s.name}: Rp ${formatRupiah(s.totalDebt)}\n")
                }
            }
            if (totalReceivable > 0 && !askSupplierDebt) {
                sb.append("\n• Total Piutang Pelanggan: *Rp ${formatRupiah(totalReceivable)}*")
            }
        }

        val json = JSONObject().apply {
            put("intent", "DEBT_QUERY")
            put("total_debt", totalDebt)
            put("total_receivable", totalReceivable)
        }

        return AIParseResult(
            intent = AIIntent.DEBT_QUERY,
            rawMessage = lower,
            summary = sb.toString().trim(),
            draftJson = json.toString(2),
            confirmationRequired = false,
            isExecutable = false
        )
    }

    private suspend fun handleReportQuery(lower: String): AIParseResult {
        val sales = repository.allSales.first()
        val purchases = repository.allPurchases.first()
        val expenses = repository.expenses.first()

        val activeSales = sales.filter { it.status == "COMPLETED" }
        val totalSales = activeSales.sumOf { it.finalAmount }
        val totalPurchases = purchases.filter { it.status == "DITERIMA" }.sumOf { it.finalAmount }
        val totalExpenses = expenses.sumOf { it.amount }

        val summary = """
            📈 Ringkasan Laporan Toko KASLITE:
            • Total Penjualan: *Rp ${formatRupiah(totalSales)}* (${activeSales.size} transaksi)
            • Total Pembelian Diterima: *Rp ${formatRupiah(totalPurchases)}*
            • Total Pengeluaran Operasional: *Rp ${formatRupiah(totalExpenses)}*
            • Estimasi Kas Bersih: *Rp ${formatRupiah(totalSales - totalPurchases - totalExpenses)}*
        """.trimIndent()

        val json = JSONObject().apply {
            put("intent", "REPORT_QUERY")
            put("total_sales", totalSales)
            put("total_purchases", totalPurchases)
            put("total_expenses", totalExpenses)
        }

        return AIParseResult(
            intent = AIIntent.REPORT_QUERY,
            rawMessage = lower,
            summary = summary,
            draftJson = json.toString(2),
            confirmationRequired = false,
            isExecutable = false
        )
    }

    private fun handleExpenseCommand(trimmed: String, lower: String): AIParseResult {
        val amount = extractAmount(lower)
        var category = "Operasional"
        when {
            lower.contains("listrik") -> category = "Listrik"
            lower.contains("air") || lower.contains("pdam") -> category = "Air"
            lower.contains("bensin") || lower.contains("transport") || lower.contains("ongkos") -> category = "Transportasi"
            lower.contains("makan") || lower.contains("minum") || lower.contains("konsumsi") -> category = "Konsumsi"
            lower.contains("atk") || lower.contains("kertas") || lower.contains("pulpen") -> category = "ATK"
            lower.contains("plastik") || lower.contains("kresek") || lower.contains("perlengkapan") -> category = "Perlengkapan"
            lower.contains("servis") || lower.contains("perawatan") -> category = "Perawatan"
        }

        val json = JSONObject().apply {
            put("intent", "EXPENSE")
            put("category", category)
            put("amount", amount)
            put("notes", trimmed)
            put("confirmation_required", true)
            put("status", "DRAFT")
        }

        val summary = """
            📝 Draft Pencatatan Pengeluaran:
            • Kategori: *$category*
            • Nominal: *Rp ${formatRupiah(amount)}*
            • Keterangan: "$trimmed"
            
            ⚠️ Apakah Anda ingin menyimpan pengeluaran ini ke buku kas?
        """.trimIndent()

        return AIParseResult(
            intent = AIIntent.EXPENSE,
            rawMessage = trimmed,
            summary = summary,
            draftJson = json.toString(2),
            category = category,
            amount = amount,
            notes = trimmed,
            confirmationRequired = true,
            isExecutable = amount > 0
        )
    }

    private fun handleIncomeCommand(trimmed: String, lower: String): AIParseResult {
        val amount = extractAmount(lower)
        var source = "Pendapatan Lainnya"
        when {
            lower.contains("transfer") -> source = "Jasa Transfer"
            lower.contains("komisi") -> source = "Komisi Penjualan"
            lower.contains("titipan") -> source = "Jasa Titip"
        }

        val json = JSONObject().apply {
            put("intent", "INCOME")
            put("source", source)
            put("amount", amount)
            put("notes", trimmed)
            put("confirmation_required", true)
            put("status", "DRAFT")
        }

        val summary = """
            💰 Draft Pencatatan Pemasukan:
            • Sumber: *$source*
            • Nominal: *Rp ${formatRupiah(amount)}*
            • Keterangan: "$trimmed"
            
            ⚠️ Apakah Anda ingin menyimpan pemasukan ini ke buku kas?
        """.trimIndent()

        return AIParseResult(
            intent = AIIntent.INCOME,
            rawMessage = trimmed,
            summary = summary,
            draftJson = json.toString(2),
            category = source,
            amount = amount,
            notes = trimmed,
            confirmationRequired = true,
            isExecutable = amount > 0
        )
    }

    private fun handleStockInCommand(
        trimmed: String,
        lower: String,
        products: List<ProductEntity>,
        aliases: List<ProductAliasEntity>
    ): AIParseResult {
        val cleaned = lower.replace("tambah stok", "")
            .replace("masuk stok", "")
            .replace("stok masuk", "")
            .replace("sebanyak", "")
            .trim()

        val qty = extractQuantity(cleaned)
        val unit = extractUnit(cleaned)
        val productNameCandidate = cleanCandidateName(cleaned)
        val matched = findBestMatchingProduct(productNameCandidate, products, aliases)

        if (matched == null) {
            return AIParseResult(
                intent = AIIntent.STOCK_IN,
                rawMessage = trimmed,
                summary = "⚠️ Tidak menemukan produk yang cocok dengan nama \"$productNameCandidate\". Pastikan nama produk terdaftar atau tambahkan produk baru terlebih dahulu.",
                draftJson = JSONObject().put("error", "PRODUCT_NOT_FOUND").toString(2),
                confirmationRequired = false,
                isExecutable = false
            )
        }

        val json = JSONObject().apply {
            put("intent", "STOCK_IN")
            put("product_id", matched.id)
            put("product_name", matched.name)
            put("quantity", qty)
            put("unit", unit.ifBlank { matched.baseUnit })
            put("confirmation_required", true)
            put("status", "DRAFT")
        }

        val summary = """
            📦 Draft Penambahan Stok:
            • Produk: *${matched.name}*
            • Jumlah: *${formatQty(qty)} ${unit.ifBlank { matched.baseUnit }}*
            • Stok sekarang: ${formatQty(matched.stock)} ${matched.baseUnit}
            • Estimasi stok baru: *${formatQty(matched.stock + qty)} ${matched.baseUnit}*
            
            ⚠️ Konfirmasi penambahan stok ini sekarang?
        """.trimIndent()

        return AIParseResult(
            intent = AIIntent.STOCK_IN,
            rawMessage = trimmed,
            summary = summary,
            draftJson = json.toString(2),
            items = listOf(ParsedItem(matched, matched.name, qty, unit.ifBlank { matched.baseUnit }, 0.0)),
            notes = "Stok masuk via AI: $trimmed",
            confirmationRequired = true,
            isExecutable = true
        )
    }

    private fun handlePurchaseCommand(
        trimmed: String,
        lower: String,
        products: List<ProductEntity>,
        aliases: List<ProductAliasEntity>,
        suppliers: List<com.example.data.entity.SupplierEntity>
    ): AIParseResult {
        // Detect supplier if specified (e.g. "di grosir", "ke pt indofood")
        var supplierName = "Pemasok Grosir"
        suppliers.forEach { s ->
            if (lower.contains(s.name.lowercase())) {
                supplierName = s.name
            }
        }

        // Split multi-item clauses by comma or "dan"
        val clauses = lower.replace("baru belanja", "")
            .replace("belanja di grosir", "")
            .replace("belanja", "")
            .replace("kulakan", "")
            .split(Regex(",|\\bdan\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val parsedItems = mutableListOf<ParsedItem>()
        val jsonItems = JSONArray()

        for (clause in clauses) {
            val qty = extractQuantity(clause)
            val unit = extractUnit(clause)
            val price = extractAmount(clause)
            val candidate = cleanCandidateName(clause)
            val matched = findBestMatchingProduct(candidate, products, aliases)

            val effectiveName = matched?.name ?: candidate.capitalizeWords()
            val effectiveUnit = unit.ifBlank { matched?.baseUnit ?: "pcs" }
            val effectivePrice = if (price > 0) price else (matched?.purchasePrice ?: 0.0)

            parsedItems.add(ParsedItem(matched, effectiveName, qty, effectiveUnit, effectivePrice))

            jsonItems.put(JSONObject().apply {
                put("product_id", matched?.id ?: -1)
                put("product_name", effectiveName)
                put("quantity", qty)
                put("unit", effectiveUnit)
                put("purchase_price", effectivePrice)
                put("subtotal", qty * effectivePrice)
            })
        }

        val totalAmount = parsedItems.sumOf { it.quantity * it.price }

        val json = JSONObject().apply {
            put("intent", "PURCHASE_ORDER")
            put("supplier_name", supplierName)
            put("items", jsonItems)
            put("total_amount", totalAmount)
            put("confirmation_required", true)
            put("status", "DRAFT")
            put("stock_policy", "STOK_HANYA_BERTAMBAH_SETELAH_BARANG_DITERIMA")
        }

        val sb = StringBuilder()
        sb.append("🛒 Draft Pembelian Belanja Toko Dibuat:\n")
        sb.append("• Pemasok: *$supplierName*\n")
        sb.append("• Rincian Barang:\n")
        parsedItems.forEachIndexed { idx, itm ->
            sb.append("  ${idx + 1}. *${itm.rawName}* (${formatQty(itm.quantity)} ${itm.unit} × Rp ${formatRupiah(itm.price)})\n")
        }
        sb.append("• *Total Belanja: Rp ${formatRupiah(totalAmount)}*\n\n")
        sb.append("⚠️ *PENTING*: Status adalah *DRAFT*. Stok barang TIDAK bertambah sampai barang fisik diterima dan dikonfirmasi di menu Pembelian/Penerimaan.")

        return AIParseResult(
            intent = AIIntent.PURCHASE_ORDER,
            rawMessage = trimmed,
            summary = sb.toString(),
            draftJson = json.toString(2),
            items = parsedItems,
            supplierName = supplierName,
            amount = totalAmount,
            notes = "Pembelian via AI: $trimmed",
            confirmationRequired = true,
            isExecutable = parsedItems.isNotEmpty()
        )
    }

    private fun handleCreateProductCommand(
        trimmed: String,
        lower: String,
        existingProducts: List<ProductEntity>
    ): AIParseResult {
        // e.g. "Tambahkan produk baru Rinso cair 800 ml, harga modal 14 ribu, harga jual 18 ribu."
        var cleaned = lower.replace("tambahkan produk baru", "")
            .replace("tambah produk baru", "")
            .replace("tambahkan produk", "")
            .replace("tambah produk", "")
            .trim()

        val purchasePrice = extractModalPrice(cleaned)
        val sellingPrice = extractSellingPrice(cleaned)
        val unit = extractUnit(cleaned).ifBlank { "pcs" }

        // Clean out price words for name
        var nameCandidate = cleaned
            .replace(Regex("harga modal.*"), "")
            .replace(Regex("modal.*"), "")
            .replace(Regex("harga jual.*"), "")
            .replace(Regex("jual.*"), "")
            .replace(",", "")
            .trim()
            .capitalizeWords()

        if (nameCandidate.isBlank()) nameCandidate = "Produk Baru AI"

        // Check for duplicates
        val duplicate = existingProducts.find {
            it.name.equals(nameCandidate, ignoreCase = true) || calculateSimilarity(it.name.lowercase(), nameCandidate.lowercase()) > 0.85
        }

        val json = JSONObject().apply {
            put("intent", "CREATE_PRODUCT")
            put("name", nameCandidate)
            put("purchase_price", purchasePrice)
            put("selling_price", sellingPrice)
            put("base_unit", unit)
            put("is_possible_duplicate", duplicate != null)
            put("duplicate_candidate", duplicate?.name)
            put("confirmation_required", true)
            put("status", "DRAFT")
        }

        val duplicateWarning = if (duplicate != null) {
            "⚠️ PERINGATAN: Ditemukan produk mirip yang sudah ada: *\"${duplicate.name}\"*. Apakah tetap ingin membuat produk baru?\n\n"
        } else ""

        val summary = """
            $duplicateWarning✨ Draft Produk Baru:
            • Nama Produk: *$nameCandidate*
            • Satuan: *$unit*
            • Harga Modal: *Rp ${formatRupiah(purchasePrice)}*
            • Harga Jual: *Rp ${formatRupiah(sellingPrice)}*
            • Margin Keuntungan: Rp ${formatRupiah(sellingPrice - purchasePrice)}
            
            ⚠️ Konfirmasi pembuatan produk baru ini?
        """.trimIndent()

        return AIParseResult(
            intent = AIIntent.CREATE_PRODUCT,
            rawMessage = trimmed,
            summary = summary,
            draftJson = json.toString(2),
            items = listOf(ParsedItem(null, nameCandidate, 0.0, unit, sellingPrice)),
            amount = sellingPrice,
            notes = "Modal: $purchasePrice, Jual: $sellingPrice",
            confirmationRequired = true,
            isExecutable = nameCandidate.isNotBlank()
        )
    }

    // --- Fuzzy Matching & Typo Correction ---

    fun findBestMatchingProduct(
        query: String,
        products: List<ProductEntity>,
        aliases: List<ProductAliasEntity>
    ): ProductEntity? {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return null

        // 1. Direct match on product name
        products.find { it.name.lowercase(Locale.ROOT) == q }?.let { return it }

        // 2. Direct match on alias table
        val aliasMatch = aliases.find { it.alias.lowercase(Locale.ROOT) == q }
        if (aliasMatch != null) {
            products.find { it.id == aliasMatch.productId }?.let { return it }
        }

        // 3. Substring matching
        products.find { it.name.lowercase(Locale.ROOT).contains(q) || q.contains(it.name.lowercase(Locale.ROOT)) }?.let { return it }

        // 4. Fuzzy similarity matching against product names & aliases
        var bestScore = 0.0
        var bestProduct: ProductEntity? = null

        for (p in products) {
            val score = calculateSimilarity(p.name.lowercase(Locale.ROOT), q)
            if (score > bestScore) {
                bestScore = score
                bestProduct = p
            }
        }

        for (a in aliases) {
            val score = calculateSimilarity(a.alias.lowercase(Locale.ROOT), q)
            if (score > bestScore) {
                bestScore = score
                bestProduct = products.find { it.id == a.productId }
            }
        }

        // Return match if similarity is above threshold (e.g. 0.45 for typos like "indomi" vs "indomie")
        return if (bestScore >= 0.45) bestProduct else null
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val dist = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)
        return 1.0 - (dist.toDouble() / maxLen.toDouble())
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val len0 = lhs.length + 1
        val len1 = rhs.length + 1
        var cost = IntArray(len0)
        var newcost = IntArray(len0)

        for (i in 0 until len0) cost[i] = i

        for (j in 1 until len1) {
            newcost[0] = j
            for (i in 1 until len0) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = cost[i - 1] + match
                val costInsert = cost[i] + 1
                val costDelete = newcost[i - 1] + 1
                newcost[i] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newcost
            newcost = swap
        }
        return cost[len0 - 1]
    }

    // --- Currency and Value Extractors ---

    fun extractAmount(text: String): Double {
        // Matches e.g. "250 ribu", "250rb", "250k", "14 ribu", "1.5 jt", "Rp 50.000", "1.500.000", "45000"
        val regex = Regex("((?:\\d{1,3}(?:\\.\\d{3})+)|\\d+(?:[.,]\\d+)?)\\s*(juta|jt|ribu|rb|k)?\\b", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(text)
        val lowerText = text.lowercase(Locale.ROOT)

        for (m in matches) {
            val rawToken = m.groupValues[1]
            val unitStr = m.groupValues[2].lowercase(Locale.ROOT)
            val isGroupedThousands = rawToken.matches(Regex("\\d{1,3}(?:\\.\\d{3})+"))
            val numStr = if (isGroupedThousands && unitStr.isBlank()) {
                rawToken.replace(".", "")
            } else {
                rawToken.replace(",", ".")
            }
            val rawNum = numStr.toDoubleOrNull() ?: continue

            val lowerMatch = m.value.lowercase(Locale.ROOT)
            val isQtyContext = listOf("dus", "karton", "pcs", "botol", "pack", "bungkus", "kg", "liter", "lusin")
                .any { u -> lowerText.contains("$lowerMatch $u") }
            if (isQtyContext && !lowerText.contains("harga $lowerMatch")) continue

            val hasExplicitRpPrefix = Regex("\\brp\\.?\\s*${Regex.escape(rawToken)}\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(lowerText)
            return when {
                unitStr == "juta" || unitStr == "jt" -> rawNum * 1_000_000
                unitStr == "ribu" || unitStr == "rb" || unitStr == "k" -> rawNum * 1_000
                rawNum > 500 || hasExplicitRpPrefix -> rawNum // Direct nominal e.g. 45000, 1500000, or explicit "Rp 500"
                else -> rawNum * 1_000 // In Indonesia context, "45" often means "45 ribu"
            }
        }
        return 0.0
    }

    private fun extractModalPrice(text: String): Double {
        val modalIdx = text.indexOf("modal")
        if (modalIdx >= 0) {
            val sub = text.substring(modalIdx, min(text.length, modalIdx + 30))
            return extractAmount(sub)
        }
        return 0.0
    }

    private fun extractSellingPrice(text: String): Double {
        val jualIdx = text.indexOf("jual")
        if (jualIdx >= 0) {
            val sub = text.substring(jualIdx, min(text.length, jualIdx + 30))
            return extractAmount(sub)
        }
        return 0.0
    }

    private fun extractQuantity(text: String): Double {
        // e.g. "2 dus", "5 karton", "20 botol", "10 bungkus"
        val regex = Regex("(\\d+([.,]\\d+)?)\\s*(dus|karton|pack|botol|bungkus|pcs|kg|liter|lusin)?", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null) {
            val numStr = match.groupValues[1].replace(",", ".")
            val num = numStr.toDoubleOrNull() ?: 1.0
            return num
        }
        return 1.0
    }

    private fun extractUnit(text: String): String {
        val units = listOf("dus", "karton", "pack", "botol", "bungkus", "pcs", "kg", "gram", "liter", "ml", "lusin", "pouch")
        for (u in units) {
            if (text.contains(Regex("\\b$u\\b", RegexOption.IGNORE_CASE))) {
                return u
            }
        }
        return ""
    }

    private fun cleanCandidateName(text: String): String {
        return text
            .replace(Regex("\\b(harga|rp|ribu|rb|k|per)\\b.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(dus|karton|pack|botol|bungkus|pcs|kg|gram|liter|ml|lusin|pouch)\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\d+([.,]\\d+)?"), "")
            .replace(Regex("[,:;-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toLong().toString()
        else String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
    }

    private fun formatRupiah(amount: Double): String {
        return java.text.NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amount.toLong())
    }

    private fun String.capitalizeWords(): String = split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() } }
}
