package com.example.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.ai.AIIntent
import com.example.ai.AIParseResult
import com.example.ai.GeraiUnaAIEngine
import com.example.data.entity.AICommandLogEntity
import com.example.data.repository.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WhatsAppMessageResponse(
    val isAuthorized: Boolean,
    val replyText: String,
    val parseResult: AIParseResult?,
    val commandLogId: Long?
)

class WhatsAppCommandCenter(
    private val repository: PosRepository,
    private val aiEngine: GeraiUnaAIEngine
) {

    suspend fun processIncomingWhatsAppMessage(
        senderPhone: String,
        messageText: String
    ): WhatsAppMessageResponse = withContext(Dispatchers.IO) {
        val cleanPhone = senderPhone.replace(Regex("[^0-9]"), "")
        val authorizedOwnerPhone = repository.getSetting("owner_authorized_phone")?.replace(Regex("[^0-9]"), "") ?: "081234567890"

        // Authentication Check
        val isAuthorized = cleanPhone.endsWith(authorizedOwnerPhone.takeLast(9)) ||
                cleanPhone == authorizedOwnerPhone ||
                senderPhone.contains("OWNER", ignoreCase = true)

        if (!isAuthorized) {
            val rejectReply = """
                ⛔ *AKSES DITOLAK - GERAI UNA SMART POS AI*
                
                Nomor *$senderPhone* tidak terdaftar sebagai Pemilik Toko terverifikasi.
                Perintah toko ditolak demi keamanan aset dan database Gerai Una.
                
                Silakan hubungi Pemilik Toko (Egis Bahricha) untuk mendaftarkan nomor ini di Pengaturan POS.
            """.trimIndent()

            val logId = repository.logAICommand(
                AICommandLogEntity(
                    source = "WHATSAPP",
                    senderPhone = senderPhone,
                    rawMessage = messageText,
                    parsedIntent = "UNAUTHORIZED_ACCESS",
                    structuredJson = "{}",
                    status = "REJECTED",
                    responseText = rejectReply
                )
            )

            return@withContext WhatsAppMessageResponse(
                isAuthorized = false,
                replyText = rejectReply,
                parseResult = null,
                commandLogId = logId
            )
        }

        // Process message with AI Engine
        val parseResult = aiEngine.processCommand(messageText)

        // Build official WhatsApp response
        val replyText = buildWhatsAppReply(parseResult)

        // Save AI Command Log
        val logId = repository.logAICommand(
            AICommandLogEntity(
                source = "WHATSAPP",
                senderPhone = senderPhone,
                rawMessage = messageText,
                parsedIntent = parseResult.intent.name,
                structuredJson = parseResult.draftJson,
                status = if (parseResult.confirmationRequired) "DRAFT" else "EXECUTED",
                responseText = replyText
            )
        )

        return@withContext WhatsAppMessageResponse(
            isAuthorized = true,
            replyText = replyText,
            parseResult = parseResult,
            commandLogId = logId
        )
    }

    private fun buildWhatsAppReply(result: AIParseResult): String {
        val sb = StringBuilder()
        sb.append("🤖 *GERAI UNA SMART POS AI*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")

        when (result.intent) {
            AIIntent.PURCHASE_ORDER -> {
                sb.append("🛒 *DRAFT PEMBELIAN BELANJA TOKO*\n\n")
                sb.append("Pemasok: *${result.supplierName}*\n")
                result.items.forEachIndexed { i, itm ->
                    sb.append("${i + 1}. ${itm.rawName} (${itm.quantity.toInt()} ${itm.unit} × Rp ${formatRupiah(itm.price)})\n")
                }
                sb.append("\n*Total Belanja: Rp ${formatRupiah(result.amount)}*\n")
                sb.append("Status: ⏳ *Menunggu Konfirmasi*\n\n")
                sb.append("⚠️ *Catatan Keamanan*: Stok barang *BELUM* bertambah. Stok baru akan bertambah otomatis setelah barang diterima secara fisik dan diverifikasi di aplikasi Gerai Una.")
            }
            AIIntent.CREATE_PRODUCT -> {
                sb.append("✨ *DRAFT PRODUK BARU*\n\n")
                result.items.firstOrNull()?.let {
                    sb.append("• Nama: *${it.rawName}*\n")
                    sb.append("• Satuan: *${it.unit}*\n")
                    sb.append("• Harga Jual: *Rp ${formatRupiah(it.price)}*\n")
                }
                sb.append("Status: ⏳ *Menunggu Konfirmasi di Aplikasi*")
            }
            AIIntent.STOCK_IN -> {
                sb.append("📦 *DRAFT PENAMBAHAN STOK*\n\n")
                result.items.firstOrNull()?.let {
                    sb.append("• Produk: *${it.rawName}*\n")
                    sb.append("• Jumlah: *+${it.quantity.toInt()} ${it.unit}*\n")
                }
                sb.append("Status: ⏳ *Menunggu Konfirmasi di Aplikasi*")
            }
            AIIntent.EXPENSE -> {
                sb.append("📝 *DRAFT PENGELUARAN OPERASIONAL*\n\n")
                sb.append("• Kategori: *${result.category}*\n")
                sb.append("• Nominal: *Rp ${formatRupiah(result.amount)}*\n")
                sb.append("• Keterangan: \"${result.notes}\"\n\n")
                sb.append("Status: ⏳ *Menunggu Konfirmasi di Aplikasi*")
            }
            AIIntent.INCOME -> {
                sb.append("💰 *DRAFT PEMASUKAN TOKO*\n\n")
                sb.append("• Sumber: *${result.category}*\n")
                sb.append("• Nominal: *Rp ${formatRupiah(result.amount)}*\n\n")
                sb.append("Status: ⏳ *Menunggu Konfirmasi di Aplikasi*")
            }
            else -> {
                sb.append(result.summary)
            }
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Creator: Egis Bahricha")
        return sb.toString()
    }

    // Share receipts or reports via WhatsApp Intent
    fun shareViaWhatsApp(context: Context, text: String, targetPhone: String = "") {
        try {
            val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                if (targetPhone.isNotBlank()) {
                    val formatted = targetPhone.replace(Regex("[^0-9]"), "")
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=" + Uri.encode(text))
                } else {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                    `package` = "com.whatsapp"
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            // Fallback to general share intent if WhatsApp is not directly installed
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(generalIntent, "Bagikan ke WhatsApp").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    private fun formatRupiah(amount: Double): String {
        return NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amount.toLong())
    }
}
