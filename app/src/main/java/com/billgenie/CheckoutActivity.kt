package com.billgenie

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.billgenie.adapter.CheckoutItemsAdapter
import com.billgenie.database.BillGenieDatabase
import com.billgenie.databinding.ActivityCheckoutBinding
import com.billgenie.model.BillItemDisplay
import com.billgenie.model.CustomerOrder
import com.billgenie.model.OrderStatus
import com.billgenie.model.SalesRecord
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CheckoutActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var database: BillGenieDatabase
    private lateinit var checkoutItemsAdapter: CheckoutItemsAdapter
    
    private var customerOrder: CustomerOrder? = null
    private var originalTotal: Double = 0.0
    private var discountAmount: Double = 0.0
    private var finalTotal: Double = 0.0
    
    private val rupeeFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    companion object {
        const val EXTRA_CUSTOMER_ORDER = "customer_order"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupDatabase()
        setupToolbar()
        setupRecyclerView()
        setupDiscountInput()
        setupClickListeners()
        loadOrderData()
    }
    
    private fun setupDatabase() {
        database = BillGenieDatabase.getDatabase(this)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Checkout"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        checkoutItemsAdapter = CheckoutItemsAdapter()
        binding.recyclerViewCheckoutItems.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            adapter = checkoutItemsAdapter
        }
    }
    
    private fun setupDiscountInput() {
        binding.etDiscountAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateFinalTotal()
            }
        })
        
        binding.etDiscountPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateFinalTotal()
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.btnPayCash.setOnClickListener {
            showCashPaymentDialog()
        }
        
        binding.btnPayUPI.setOnClickListener {
            showUPIPaymentDialog()
        }
    }
    
    private fun loadOrderData() {
        // Get customer order from intent
        val orderData = intent.getSerializableExtra(EXTRA_CUSTOMER_ORDER) as? CustomerOrder
        if (orderData == null) {
            Toast.makeText(this, "Error: Order data not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        customerOrder = orderData
        originalTotal = orderData.total
        finalTotal = originalTotal
        
        // Display order information
        displayOrderInfo(orderData)
        
        // Set up bill items
        checkoutItemsAdapter.submitList(orderData.orderItems)
        
        // Update totals
        updateTotalDisplay()
    }
    
    private fun displayOrderInfo(order: CustomerOrder) {
        binding.tvOrderNumber.text = "Order #${order.customerNumber}"
        
        if (order.tableName.isNotEmpty()) {
            binding.tvTableInfo.text = "Table: ${order.tableName}"
            binding.tvTableInfo.visibility = View.VISIBLE
        } else {
            binding.tvTableInfo.visibility = View.GONE
        }
        
        if (order.customerName.isNotEmpty()) {
            binding.tvCustomerName.text = "Customer: ${order.customerName}"
            binding.tvCustomerName.visibility = View.VISIBLE
        } else {
            binding.tvCustomerName.text = "Walk-in Customer"
            binding.tvCustomerName.visibility = View.VISIBLE
        }
        
        binding.tvOrderDate.text = "Date: ${dateFormatter.format(Date(order.orderTimestamp))}"
        
        val itemCount = order.orderItems.sumOf { it.quantity }
        binding.tvItemCount.text = "$itemCount item${if (itemCount != 1) "s" else ""}"
    }
    
    private fun calculateFinalTotal() {
        val discountAmountText = binding.etDiscountAmount.text.toString()
        val discountPercentageText = binding.etDiscountPercentage.text.toString()
        
        discountAmount = 0.0
        
        // Calculate discount from amount field
        if (discountAmountText.isNotEmpty()) {
            try {
                val enteredAmount = discountAmountText.toDouble()
                if (enteredAmount >= 0 && enteredAmount <= originalTotal) {
                    discountAmount = enteredAmount
                    // Clear percentage field to avoid confusion
                    if (discountPercentageText.isNotEmpty()) {
                        binding.etDiscountPercentage.setText("")
                    }
                }
            } catch (e: NumberFormatException) {
                // Invalid input, ignore
            }
        }
        // Calculate discount from percentage field (only if amount field is empty)
        else if (discountPercentageText.isNotEmpty()) {
            try {
                val enteredPercentage = discountPercentageText.toDouble()
                if (enteredPercentage >= 0 && enteredPercentage <= 100) {
                    discountAmount = originalTotal * (enteredPercentage / 100)
                }
            } catch (e: NumberFormatException) {
                // Invalid input, ignore
            }
        }
        
        finalTotal = originalTotal - discountAmount
        updateTotalDisplay()
    }
    
    private fun updateTotalDisplay() {
        binding.tvSubtotal.text = rupeeFormatter.format(originalTotal)
        binding.tvDiscountAmount.text = "- ${rupeeFormatter.format(discountAmount)}"
        binding.tvFinalTotal.text = rupeeFormatter.format(finalTotal)
        
        // Show/hide discount row based on whether there's a discount
        if (discountAmount > 0) {
            binding.layoutDiscount.visibility = View.VISIBLE
        } else {
            binding.layoutDiscount.visibility = View.GONE
        }
    }
    
    private fun showCashPaymentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cash_payment, null)
        val etCashAmount = dialogView.findViewById<EditText>(R.id.etCashAmount)
        val tvBillAmount = dialogView.findViewById<android.widget.TextView>(R.id.tvBillAmount)
        val tvChangeAmount = dialogView.findViewById<android.widget.TextView>(R.id.tvChangeAmount)
        val cardChangeAmount = dialogView.findViewById<android.view.View>(R.id.cardChangeAmount)
        val tvErrorMessage = dialogView.findViewById<android.widget.TextView>(R.id.tvErrorMessage)
        val btnCompletePayment = dialogView.findViewById<android.widget.Button>(R.id.btnCompletePayment)
        
        // Set bill amount
        tvBillAmount.text = rupeeFormatter.format(finalTotal)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("💵 Cash Payment")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()
        
        // Add text watcher for real-time change calculation
        etCashAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val cashAmountText = s.toString()
                
                if (cashAmountText.isEmpty()) {
                    // Hide change and error, disable button
                    cardChangeAmount.visibility = android.view.View.GONE
                    tvErrorMessage.visibility = android.view.View.GONE
                    btnCompletePayment.isEnabled = false
                    return
                }
                
                try {
                    val cashAmount = cashAmountText.toDouble()
                    
                    if (cashAmount < finalTotal) {
                        // Show insufficient amount error
                        cardChangeAmount.visibility = android.view.View.GONE
                        tvErrorMessage.visibility = android.view.View.VISIBLE
                        tvErrorMessage.text = "Insufficient amount (Need ${rupeeFormatter.format(finalTotal - cashAmount)} more)"
                        btnCompletePayment.isEnabled = false
                    } else {
                        // Show change amount and enable button
                        val change = cashAmount - finalTotal
                        tvErrorMessage.visibility = android.view.View.GONE
                        
                        if (change > 0) {
                            cardChangeAmount.visibility = android.view.View.VISIBLE
                            tvChangeAmount.text = rupeeFormatter.format(change)
                        } else {
                            cardChangeAmount.visibility = android.view.View.GONE
                        }
                        
                        btnCompletePayment.isEnabled = true
                    }
                } catch (e: NumberFormatException) {
                    // Invalid number format
                    cardChangeAmount.visibility = android.view.View.GONE
                    tvErrorMessage.visibility = android.view.View.VISIBLE
                    tvErrorMessage.text = "Please enter a valid amount"
                    btnCompletePayment.isEnabled = false
                }
            }
        })
        
        // Handle complete payment button click
        btnCompletePayment.setOnClickListener {
            val cashAmountText = etCashAmount.text.toString()
            try {
                val cashAmount = cashAmountText.toDouble()
                val change = cashAmount - finalTotal
                dialog.dismiss()
                processCashPayment(cashAmount, change)
            } catch (e: NumberFormatException) {
                // This shouldn't happen as button is disabled for invalid input
                Toast.makeText(this@CheckoutActivity, "Invalid amount", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    private fun showFinalPaymentConfirmation(cashGiven: Double, change: Double, onConfirm: () -> Unit) {
        val message = buildString {
            appendLine("💰 Payment Summary")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Bill Amount: ${rupeeFormatter.format(finalTotal)}")
            appendLine("Cash Given: ${rupeeFormatter.format(cashGiven)}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            if (change > 0) {
                appendLine("Change to Return: ${rupeeFormatter.format(change)}")
            } else {
                appendLine("No Change Required")
            }
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("💵 Payment Confirmation")
            .setMessage(message)
            .setPositiveButton("Complete Payment") { _, _ -> onConfirm() }
            .setNegativeButton("Back", null)
            .show()
    }
    
    private fun showUPIPaymentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_upi_payment, null)
        val tvAmount = dialogView.findViewById<android.widget.TextView>(R.id.tvUpiAmount)
        val tvCustomerName = dialogView.findViewById<android.widget.TextView>(R.id.tvUpiCustomerName)
        val tvCustomerPhone = dialogView.findViewById<android.widget.TextView>(R.id.tvUpiCustomerPhone)
        
        // Set amount
        tvAmount.text = rupeeFormatter.format(finalTotal)
        
        // Set customer details
        customerOrder?.let { order ->
            tvCustomerName.text = if (order.customerName.isNotEmpty()) order.customerName else "Walk-in Customer"
            tvCustomerPhone.text = "📱 Customer Phone" // You can add phone field to CustomerOrder model if needed
        }
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("UPI Payment Request")
            .setView(dialogView)
            .setPositiveButton("Payment Completed") { _, _ ->
                processUPIPayment()
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        // Set up UPI app buttons
        dialogView.findViewById<android.widget.Button>(R.id.btnGooglePay)?.setOnClickListener {
            Toast.makeText(this, "Sending payment request via Google Pay...", Toast.LENGTH_SHORT).show()
        }
        
        dialogView.findViewById<android.widget.Button>(R.id.btnPhonePe)?.setOnClickListener {
            Toast.makeText(this, "Sending payment request via PhonePe...", Toast.LENGTH_SHORT).show()
        }
        
        dialogView.findViewById<android.widget.Button>(R.id.btnPaytm)?.setOnClickListener {
            Toast.makeText(this, "Sending payment request via Paytm...", Toast.LENGTH_SHORT).show()
        }
        
        dialogView.findViewById<android.widget.Button>(R.id.btnBhim)?.setOnClickListener {
            Toast.makeText(this, "Sending payment request via BHIM UPI...", Toast.LENGTH_SHORT).show()
        }
        
        dialogView.findViewById<android.widget.Button>(R.id.btnGenericUpi)?.setOnClickListener {
            Toast.makeText(this, "Sending generic UPI payment request...", Toast.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }
    
    private fun processCashPayment(cashGiven: Double, change: Double) {
        lifecycleScope.launch {
            try {
                customerOrder?.let { order ->
                    // Update order status to completed
                    val completedOrder = order.copy(status = OrderStatus.COMPLETED)
                    database.customerOrderDao().updateOrder(completedOrder)
                    
                    // Save sales record
                    saveSalesRecord(order, "CASH", cashGiven, change)
                    
                    // Generate and show final bill
                    showFinalBill("CASH", cashGiven, change)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutActivity, "Error processing payment: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun processUPIPayment() {
        lifecycleScope.launch {
            try {
                customerOrder?.let { order ->
                    // Update order status to completed
                    val completedOrder = order.copy(status = OrderStatus.COMPLETED)
                    database.customerOrderDao().updateOrder(completedOrder)
                    
                    // Save sales record
                    saveSalesRecord(order, "UPI", finalTotal, 0.0)
                    
                    // Generate and show final bill
                    showFinalBill("UPI", finalTotal, 0.0)
                }
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutActivity, "Error processing payment: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private suspend fun saveSalesRecord(order: CustomerOrder, paymentMethod: String, amountPaid: Double, changeAmount: Double) {
        try {
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            
            val salesRecord = SalesRecord(
                orderNumber = order.customerNumber,
                tableName = order.tableName,
                customerName = order.customerName,
                orderItems = order.orderItems,
                subtotal = originalTotal,
                discountAmount = discountAmount,
                finalTotal = finalTotal,
                paymentMethod = paymentMethod,
                amountPaid = amountPaid,
                changeAmount = changeAmount,
                saleTimestamp = System.currentTimeMillis(),
                saleDate = dateFormat.format(currentDate),
                saleMonth = monthFormat.format(currentDate),
                saleYear = yearFormat.format(currentDate)
            )
            
            database.salesDao().insertSalesRecord(salesRecord)
            android.util.Log.d("CheckoutActivity", "Sales record saved successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("CheckoutActivity", "Error saving sales record: ${e.message}", e)
        }
    }
    
    private fun showFinalBill(paymentMethod: String, amountPaid: Double, change: Double) {
        val intent = Intent(this, FinalBillActivity::class.java)
        intent.putExtra("customer_order", customerOrder)
        intent.putExtra("discount_amount", discountAmount)
        intent.putExtra("final_total", finalTotal)
        intent.putExtra("payment_method", paymentMethod)
        intent.putExtra("amount_paid", amountPaid)
        intent.putExtra("change_amount", change)
        startActivity(intent)
        
        // Close checkout activity and return to orders
        setResult(RESULT_OK)
        finish()
    }
}