package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.model.InventoryCategory
import com.example.data.model.InventoryItem
import com.example.data.model.OrderRecord
import com.example.data.model.PartyBooking
import com.example.data.model.RestaurantProfile
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintAndShareHelper {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    fun printKotHtml(
        context: Context,
        profile: RestaurantProfile,
        order: OrderRecord
    ) {
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val html = """
            <html>
            <body style="font-family: monospace; padding: 16px; max-width: 320px;">
              <h2 style="text-align:center; margin:0;">${escapeHtml(profile.restaurantName)}</h2>
              <p style="text-align:center; margin:4px 0;"><strong>*** KITCHEN ORDER TICKET (KOT) ***</strong></p>
              <hr/>
              <p><strong>KOT #:</strong> ${escapeHtml(order.orderNumber)}<br/>
                 <strong>Channel:</strong> ${escapeHtml(order.channel)} ${if (order.tableOrRef.isNotBlank()) "(${escapeHtml(order.tableOrRef)})" else ""}<br/>
                 <strong>Time:</strong> $timeStr<br/>
                 ${if (order.customerName.isNotBlank()) "<strong>Guest:</strong> ${escapeHtml(order.customerName)}<br/>" else ""}
              </p>
              <hr/>
              <h3>ITEMS (${order.itemsCount})</h3>
              <p style="font-size:15px; white-space:pre-wrap;">${escapeHtml(order.itemsSummary)}</p>
              ${if (order.notes.isNotBlank()) "<hr/><p><strong>Notes:</strong> ${escapeHtml(order.notes)}</p>" else ""}
              <hr/>
              <p style="text-align:center;">Printed via Mara Manager</p>
            </body>
            </html>
        """.trimIndent()
        printHtmlDocument(context, "KOT_${order.orderNumber}", html)
    }

    fun printCustomerBillHtml(
        context: Context,
        profile: RestaurantProfile,
        order: OrderRecord
    ) {
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val gstLine = if (order.gstEnabled) {
            "<tr><td>GST (${String.format(Locale.US, "%.1f", order.gstPercent)}%)</td><td align='right'>₹${String.format(Locale.US, "%.2f", order.gstAmount)}</td></tr>"
        } else {
            "<tr><td>GST</td><td align='right'>Waived / Cancelled</td></tr>"
        }
        val tipLine = if (order.tipAmount > 0) {
            "<tr><td>Tip</td><td align='right'>₹${String.format(Locale.US, "%.2f", order.tipAmount)}</td></tr>"
        } else ""

        val html = """
            <html>
            <body style="font-family: sans-serif; padding: 16px; max-width: 340px;">
              <h2 style="text-align:center; margin-bottom:2px;">${escapeHtml(profile.restaurantName)}</h2>
              <p style="text-align:center; font-size:12px; margin-top:0;">
                ${escapeHtml(profile.address)}<br/>
                ${if (profile.phone.isNotBlank()) "Ph: ${escapeHtml(profile.phone)}<br/>" else ""}
                ${if (profile.gstin.isNotBlank()) "GSTIN: ${escapeHtml(profile.gstin)}" else ""}
              </p>
              <hr/>
              <p style="font-size:12px;">
                <strong>Bill #:</strong> ${escapeHtml(order.orderNumber)} &nbsp; <strong>Type:</strong> ${escapeHtml(order.channel)}<br/>
                <strong>Date:</strong> $timeStr<br/>
                ${if (order.tableOrRef.isNotBlank()) "<strong>Ref/Table:</strong> ${escapeHtml(order.tableOrRef)}<br/>" else ""}
                <strong>Payment:</strong> ${escapeHtml(order.paymentMethod)}
              </p>
              <hr/>
              <p style="font-size:14px;"><strong>Order Items (${order.itemsCount}):</strong><br/>
              ${escapeHtml(order.itemsSummary)}</p>
              <hr/>
              <table width="100%" style="font-size:14px;">
                <tr><td>Subtotal</td><td align="right">₹${String.format(Locale.US, "%.2f", order.subtotal)}</td></tr>
                $gstLine
                $tipLine
                <tr><td colspan="2"><hr/></td></tr>
                <tr><td><strong>GRAND TOTAL</strong></td><td align="right"><strong>₹${String.format(Locale.US, "%.2f", order.totalAmount + order.tipAmount)}</strong></td></tr>
              </table>
              <hr/>
              <p style="text-align:center; font-size:12px;">Thank you for dining with us!</p>
            </body>
            </html>
        """.trimIndent()
        printHtmlDocument(context, "Bill_${order.orderNumber}", html)
    }

    private fun printHtmlDocument(context: Context, jobName: String, html: String) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val adapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
                    } else {
                        Toast.makeText(context, "Print service unavailable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "Print failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatInventoryWhatsAppReport(
        restaurantName: String,
        items: List<InventoryItem>
    ): String {
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("*$restaurantName — Inventory Report*\n")
        sb.append("Date: $dateStr\n\n")

        val lowItems = items.filter { it.isLowStock }
        if (lowItems.isNotEmpty()) {
            sb.append("⚠️ *LOW / OUT OF STOCK ALERTS (${lowItems.size}):*\n")
            lowItems.forEach {
                sb.append("• ${it.name}: ${formatQty(it.quantity)} ${it.unit} (Min: ${formatQty(it.minThreshold)})\n")
            }
            sb.append("\n")
        }

        InventoryCategory.entries.forEach { cat ->
            val catItems = items.filter { it.category == cat.key }
            if (catItems.isNotEmpty()) {
                val title = when (cat) {
                    InventoryCategory.KITCHEN -> "🍳 Kitchen Items"
                    InventoryCategory.PACKING -> "📦 Packing Material"
                    InventoryCategory.TANDOOR_COAL -> "🔥 Tandoor Coal"
                    InventoryCategory.COLD_DRINKS -> "🥤 Cold Drinks (Coke/Pepsi)"
                }
                sb.append("*$title*\n")
                catItems.forEach { item ->
                    val flag = if (item.isLowStock) " [LOW]" else ""
                    val exp = if (item.expiryDate.isNotBlank()) " (Exp: ${item.expiryDate})" else ""
                    sb.append("- ${item.name}: ${formatQty(item.quantity)} ${item.unit}$flag$exp\n")
                }
                sb.append("\n")
            }
        }
        return sb.toString().trim()
    }

    fun formatPartyChefWhatsAppReport(
        restaurantName: String,
        booking: PartyBooking
    ): String {
        return buildString {
            append("*$restaurantName — Party Chef Requirement*\n")
            append("🎉 Party: ${booking.partyName}\n")
            if (booking.partyPhone.isNotBlank()) append("📞 Phone: ${booking.partyPhone}\n")
            append("📅 Event Date: ${booking.eventDate} | Guests: ${booking.guestCount}\n\n")
            append("👨‍🍳 *Chef Inventory Required:*\n")
            append(booking.chefRequirements.ifBlank { "No chef items listed yet" })
            append("\n\n📦 *Inventory Used:*\n")
            append(booking.inventoryUsedSummary.ifBlank { "Not logged yet" })
        }
    }

    fun shareToWhatsAppOrChooser(context: Context, text: String, title: String = "Share via WhatsApp") {
        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(waIntent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                title
            )
            context.startActivity(chooser)
        }
    }

    fun copyUriToInternalWallpaper(context: Context, uri: Uri): String? {
        return try {
            val destFile = File(context.filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
            // Clean older wallpaper files
            context.filesDir.listFiles()?.filter { it.name.startsWith("custom_wallpaper_") }?.forEach {
                it.delete()
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun restartApp(context: Context) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(context.packageName) ?: return
        val component = launchIntent.component ?: return
        val restartIntent = Intent.makeRestartActivityTask(component)
        context.startActivity(restartIntent)
        Runtime.getRuntime().exit(0)
    }

    private fun formatQty(q: Double): String =
        if (q % 1.0 == 0.0) q.toInt().toString() else String.format(Locale.US, "%.1f", q)

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
