package com.billgenie

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityQrMenuBinding
import com.billgenie.utils.SimpleQRGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class QRMenuActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityQrMenuBinding
    private lateinit var database: BillGenieDatabase
    private var qrBitmap: Bitmap? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupDatabase()
        setupButtons()
        generateQRCode()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Menu QR Code"
    }
    
    private fun setupDatabase() {
        database = BillGenieDatabase.getDatabase(this)
    }
    
    private fun setupButtons() {
        binding.btnShare.isEnabled = false // Disable until QR is generated
        binding.btnShare.setOnClickListener { shareQRCode() }
        binding.btnRefresh.setOnClickListener { 
            binding.imageViewQR.setImageBitmap(null) // Clear existing image
            binding.textViewInstructions.text = "Generating QR code..."
            generateQRCode() 
        }
        // Generate QR code immediately when screen opens
        generateQRCode()
    }
    
    private fun generateQRCode() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Get menu items
                val menuItems = database.menuItemDao().getAllEnabledMenuItemsSync()
                withContext(Dispatchers.Main) {
                    Log.d("QRMenuActivity", "Found ${menuItems.size} menu items")
                    binding.textViewInstructions.text = "Found ${menuItems.size} menu items"
                }
                
                if (menuItems.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@QRMenuActivity, "Please add and enable some menu items first", Toast.LENGTH_LONG).show()
                        binding.textViewInstructions.text = "No enabled menu items found. Please add and enable menu items."
                    }
                    return@launch
                }
                
                // Step 2: Use larger size for better scanning
                val width = 500 // Increased size for better readability
                val height = 500 // Increased size for better readability
                
                withContext(Dispatchers.Main) {
                    Log.d("QRMenuActivity", "Starting QR generation with size ${width}x${height}")
                    binding.textViewInstructions.text = "Generating QR code..."
                }
                
                // Generate QR code with SimpleQRGenerator
                qrBitmap = SimpleQRGenerator.generateQRCode(menuItems, width, height)
                
                withContext(Dispatchers.Main) {
                    if (qrBitmap != null) {
                        Log.d("QRMenuActivity", "QR code generated successfully")
                        binding.imageViewQR.setImageBitmap(qrBitmap)
                        binding.btnShare.isEnabled = true
                        binding.textViewInstructions.text = "Scan this QR code to view the menu"
                    } else {
                        Log.e("QRMenuActivity", "Generated bitmap is null")
                        binding.textViewInstructions.text = "Failed to generate QR code"
                        Toast.makeText(this@QRMenuActivity, "Failed to generate QR code", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("QRMenuActivity", "Error in generateQRCode: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.textViewInstructions.text = "Error: ${e.message}"
                    Toast.makeText(this@QRMenuActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun shareQRCode() {
        val qrCode = qrBitmap ?: return
        
        lifecycleScope.launch {
            try {
                // Create menu directory in cache
                val cachePath = File(cacheDir, "menu")
                cachePath.mkdirs()
                
                // Save QR code image
                val qrFile = File(cachePath, "menu_qr.png")
                withContext(Dispatchers.IO) {
                    val outputStream = FileOutputStream(qrFile)
                    qrCode.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.close()
                }
                
                // Get URI for QR code image
                val qrUri = FileProvider.getUriForFile(
                    this@QRMenuActivity,
                    "${applicationContext.packageName}.provider",
                    qrFile
                )
                
                // Share just the QR code image
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, qrUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Restaurant Menu QR Code")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                startActivity(Intent.createChooser(shareIntent, "Share Menu"))
            } catch (e: Exception) {
                Toast.makeText(this@QRMenuActivity, "Error sharing menu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}