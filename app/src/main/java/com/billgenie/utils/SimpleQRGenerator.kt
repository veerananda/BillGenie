package com.billgenie.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.billgenie.model.MenuItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class SimpleQRGenerator {
    companion object {
        private const val MENU_BASE_URL = "https://veerananda.github.io/billgenie/"
        
        private fun encodeMenuData(menuItems: List<MenuItem>): String {
            // Convert menu items to simple data objects
            val menuData = menuItems.map { item ->
                mapOf(
                    "name" to item.name,
                    "price" to item.price,
                    "category" to item.category,
                    "isVegetarian" to item.isVegetarian
                )
            }
            
            // Convert to JSON and encode for URL
            val json = com.google.gson.Gson().toJson(menuData)
            return java.net.URLEncoder.encode(json, "UTF-8")
        }
        
        fun generateQRCode(menuItems: List<MenuItem>, width: Int, height: Int): Bitmap {
            // Generate menu URL with data
            val menuUrl = "${MENU_BASE_URL}?data=${encodeMenuData(menuItems)}"

            // Configure QR code generation
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }

            // Generate QR code bit matrix
            val bitMatrix = QRCodeWriter().encode(
                menuUrl,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            // Convert to bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            return bitmap
        }
    }
}