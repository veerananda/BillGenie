package com.billgenie.utils

import android.graphics.Bitmap
import android.util.Base64
import com.billgenie.model.MenuItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class QRMenuGenerator {
    companion object {
        private val MENU_HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Menu</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 16px;
                        background: #f5f5f5;
                    }
                    .menu-container {
                        max-width: 600px;
                        margin: 0 auto;
                    }
                    .category {
                        margin: 16px 0;
                        background: white;
                        border-radius: 8px;
                        padding: 16px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .category-title {
                        color: #1976D2;
                        margin: 0 0 12px 0;
                        font-size: 1.2em;
                    }
                    .menu-item {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        padding: 8px 0;
                        border-bottom: 1px solid #eee;
                    }
                    .menu-item:last-child {
                        border-bottom: none;
                    }
                    .item-name {
                        flex: 1;
                    }
                    .veg-indicator {
                        width: 12px;
                        height: 12px;
                        border-radius: 50%;
                        display: inline-block;
                        margin-right: 8px;
                        background: #4CAF50;
                    }
                    .non-veg-indicator {
                        background: #F44336;
                    }
                    .item-price {
                        color: #1976D2;
                        font-weight: bold;
                        margin-left: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="menu-container">
                    %s
                </div>
            </body>
            </html>
        """.trimIndent()

        /**
         * Generates the menu HTML content from a list of menu items
         */
        private fun generateMenuHtml(items: List<MenuItem>): String {
            val categoryMap = items.groupBy { it.category }
            val categoriesHtml = categoryMap.map { (category, categoryItems) ->
                """
                <div class="category">
                    <h2 class="category-title">$category</h2>
                    ${
                    categoryItems.joinToString("\n") { item ->
                        """
                        <div class="menu-item">
                            <div class="item-name">
                                <span class="veg-indicator${if (!item.isVegetarian) " non-veg-indicator" else ""}"></span>
                                ${item.name}
                            </div>
                            <div class="item-price">₹${String.format("%.2f", item.price)}</div>
                        </div>
                        """.trimIndent()
                    }
                    }
                </div>
                """.trimIndent()
            }.joinToString("\n")
            
            return String.format(MENU_HTML_TEMPLATE, categoriesHtml)
        }

        /**
         * Compresses the given string using GZIP
         */
        private fun compressString(str: String): String {
            val outputStream = ByteArrayOutputStream()
            val gzipOutputStream = GZIPOutputStream(outputStream)
            gzipOutputStream.write(str.toByteArray())
            gzipOutputStream.close()
            return Base64.encodeToString(outputStream.toByteArray(), Base64.URL_SAFE)
        }

        /**
         * Generates a QR code bitmap for the given menu items
         */
        fun generateQRCode(menuItems: List<MenuItem>, width: Int, height: Int): Bitmap {
            try {
                // Step 1: Generate simple text menu content
                val menuText = buildString {
                    append("MENU\n\n")
                    menuItems.groupBy { it.category }.forEach { (category, items) ->
                        append("$category:\n")
                        items.forEach { item ->
                            val veg = if (item.isVegetarian) "V" else "N"
                            append("[$veg] ${item.name} - ₹${String.format("%.2f", item.price)}\n")
                        }
                        append("\n")
                    }
                }.trim()
                
                android.util.Log.d("QRMenuGenerator", "Generated menu HTML")
                
                android.util.Log.d("QRMenuGenerator", "Menu text to encode: $menuText")
                
                // Step 2: Configure QR generation with maximum reliability
                val writer = MultiFormatWriter()
                val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
                hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"
                hints[com.google.zxing.EncodeHintType.MARGIN] = 2
                
                android.util.Log.d("QRMenuGenerator", "Encoding HTML with size ${width}x${height}")
                
                // Step 3: Generate QR code with plain text
                val bitMatrix = writer.encode(
                    menuText,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    hints
                )
                
                android.util.Log.d("QRMenuGenerator", "BitMatrix generated, creating bitmap")
                
                // Step 4: Convert to bitmap
                val barcodeEncoder = BarcodeEncoder()
                return barcodeEncoder.createBitmap(bitMatrix)
            } catch (e: Exception) {
                android.util.Log.e("QRMenuGenerator", "Error generating QR code", e)
                throw RuntimeException("QR Generation failed: ${e.message}", e)
            }
        }
    }
}