package com.billgenie.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

object QRCodeGenerator {
    
    /**
     * Generate QR code bitmap from text
     */
    fun generateQRCode(text: String, width: Int, height: Int): Bitmap? {
        return try {
            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Generate UPI payment QR code
     */
    fun generateUPIQRCode(
        merchantUPI: String,
        merchantName: String,
        amount: Double,
        currency: String = "INR",
        width: Int = 300,
        height: Int = 300
    ): Bitmap? {
        val upiString = "upi://pay?pa=$merchantUPI&pn=$merchantName&am=$amount&cu=$currency"
        return generateQRCode(upiString, width, height)
    }
}