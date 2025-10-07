package com.billgenie.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.InventoryDisplayItem
import com.billgenie.utils.UnitConverter
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*

class InventoryAdapter(
    private val onQuantityChanged: (InventoryDisplayItem, Double) -> Unit,
    private val onFullQuantityChanged: (InventoryDisplayItem, Double) -> Unit,
    private val onRefreshData: () -> Unit = {}  // Add refresh callback with default empty implementation
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private var inventoryItems = listOf<InventoryDisplayItem>()

    fun updateInventory(newItems: List<InventoryDisplayItem>) {
        val diffCallback = InventoryDiffCallback(inventoryItems, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        inventoryItems = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    // DiffUtil callback for efficient updates
    private class InventoryDiffCallback(
        private val oldList: List<InventoryDisplayItem>,
        private val newList: List<InventoryDisplayItem>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].ingredientId == newList[newItemPosition].ingredientId
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(inventoryItems[position])
    }

    override fun getItemCount() = inventoryItems.size

    inner class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardInventory: MaterialCardView = itemView.findViewById(R.id.cardInventory)
        private val textIngredientName: TextView = itemView.findViewById(R.id.textIngredientName)
        private val textUnit: TextView = itemView.findViewById(R.id.textUnit)
        private val textUnitFull: TextView = itemView.findViewById(R.id.textUnitFull)
        private val editTextCurrentStock: TextInputEditText = itemView.findViewById(R.id.editTextCurrentStock)
        private val editTextFullQuantity: TextInputEditText = itemView.findViewById(R.id.editTextFullQuantity)
        private val textMinimumStock: TextView = itemView.findViewById(R.id.textMinimumStock)
        private val textStockStatus: TextView = itemView.findViewById(R.id.textStockStatus)
        private val textStockPercentage: TextView = itemView.findViewById(R.id.textStockPercentage)
        private val textCriticalAlert: TextView = itemView.findViewById(R.id.textCriticalAlert)

        private var currentItem: InventoryDisplayItem? = null
        private var isUpdating = false
        
        // Track which fields are being edited to prevent cursor jumping
        private var isEditingCurrentStock = false
        private var isEditingFullQuantity = false
        
        // Debouncing for text changes
        private var currentStockJob: Job? = null
        private var fullQuantityJob: Job? = null

        fun bind(inventoryItem: InventoryDisplayItem) {
            currentItem = inventoryItem
            isUpdating = true

            // Get display-friendly values
            val (currentDisplayQty, currentDisplayUnit) = UnitConverter.getBestDisplayUnit(
                inventoryItem.currentStock, inventoryItem.ingredientUnit
            )
            val (fullDisplayQty, fullDisplayUnit) = UnitConverter.getBestDisplayUnit(
                inventoryItem.fullQuantity, inventoryItem.ingredientUnit
            )
            val (minDisplayQty, minDisplayUnit) = UnitConverter.getBestDisplayUnit(
                inventoryItem.minimumStock, inventoryItem.ingredientUnit
            )

            textIngredientName.text = inventoryItem.ingredientName
            textUnit.text = currentDisplayUnit
            textUnitFull.text = fullDisplayUnit
            
            // Only update EditText content if user is not actively editing
            if (!isEditingCurrentStock) {
                val currentStockText = if (currentDisplayQty == currentDisplayQty.toInt().toDouble()) {
                    currentDisplayQty.toInt().toString()
                } else {
                    String.format("%.1f", currentDisplayQty)
                }
                editTextCurrentStock.setText(currentStockText)
                editTextCurrentStock.setSelection(editTextCurrentStock.text?.length ?: 0)
            }
            
            if (!isEditingFullQuantity) {
                val fullQuantityText = if (fullDisplayQty == fullDisplayQty.toInt().toDouble()) {
                    fullDisplayQty.toInt().toString()
                } else {
                    String.format("%.1f", fullDisplayQty)
                }
                editTextFullQuantity.setText(fullQuantityText)
                editTextFullQuantity.setSelection(editTextFullQuantity.text?.length ?: 0)
            }
            
            textMinimumStock.text = UnitConverter.formatDisplayText(inventoryItem.minimumStock, inventoryItem.ingredientUnit).let { 
                "Min: $it" 
            }

            // Update stock percentage
            val percentage = inventoryItem.stockPercentage
            textStockPercentage.text = "Stock Level: ${String.format("%.1f", percentage)}%"

            // Update critical alert visibility
            if (inventoryItem.isCriticallyLow) {
                textCriticalAlert.visibility = View.VISIBLE
                textStockPercentage.setTextColor(itemView.context.getColor(R.color.error))
            } else {
                textCriticalAlert.visibility = View.GONE
                textStockPercentage.setTextColor(itemView.context.getColor(R.color.text_primary))
            }

            // Update stock status
            if (inventoryItem.isLowStock || inventoryItem.isCriticallyLow) {
                textStockStatus.text = if (inventoryItem.isCriticallyLow) "Critical" else "Low Stock"
                textStockStatus.setTextColor(itemView.context.getColor(R.color.error))
                cardInventory.strokeColor = itemView.context.getColor(R.color.error)
                cardInventory.strokeWidth = 2
            } else {
                textStockStatus.text = "In Stock"
                textStockStatus.setTextColor(itemView.context.getColor(R.color.success))
                cardInventory.strokeColor = itemView.context.getColor(R.color.surface_variant)
                cardInventory.strokeWidth = 1
            }

            isUpdating = false

            // Set up focus listeners to track editing state
            editTextCurrentStock.setOnFocusChangeListener { _, hasFocus ->
                isEditingCurrentStock = hasFocus
                if (!hasFocus) {
                    // User finished editing, refresh data to get updated percentages
                    onRefreshData()
                }
            }
            
            editTextFullQuantity.setOnFocusChangeListener { _, hasFocus ->
                isEditingFullQuantity = hasFocus
                if (!hasFocus) {
                    // User finished editing, refresh data to get updated percentages
                    onRefreshData()
                }
            }

            // Set up text watcher for current stock changes with debouncing
            editTextCurrentStock.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdating && currentItem != null) {
                        currentStockJob?.cancel()
                        currentStockJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(1000) // Increased delay to 1 second
                            val newQuantityText = s.toString().trim()
                            if (newQuantityText.isNotEmpty()) {
                                val newQuantity = newQuantityText.toDoubleOrNull()
                                if (newQuantity != null && newQuantity >= 0) {
                                    // Convert display unit back to base unit
                                    val (_, currentDisplayUnit) = UnitConverter.getBestDisplayUnit(
                                        currentItem!!.currentStock, currentItem!!.ingredientUnit
                                    )
                                    val (baseQuantity, _) = UnitConverter.toBaseUnit(newQuantity, currentDisplayUnit)
                                    isEditingCurrentStock = false
                                    onQuantityChanged(currentItem!!, baseQuantity)
                                }
                            }
                        }
                    }
                }
            })

            // Set up text watcher for full stock changes with debouncing
            editTextFullQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdating && currentItem != null) {
                        fullQuantityJob?.cancel()
                        fullQuantityJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(1000) // Increased delay to 1 second
                            val newQuantityText = s.toString().trim()
                            if (newQuantityText.isNotEmpty()) {
                                val newQuantity = newQuantityText.toDoubleOrNull()
                                if (newQuantity != null && newQuantity > 0) {
                                    // Convert display unit back to base unit
                                    val (_, fullDisplayUnit) = UnitConverter.getBestDisplayUnit(
                                        currentItem!!.fullQuantity, currentItem!!.ingredientUnit
                                    )
                                    val (baseQuantity, _) = UnitConverter.toBaseUnit(newQuantity, fullDisplayUnit)
                                    isEditingFullQuantity = false
                                    onFullQuantityChanged(currentItem!!, baseQuantity)
                                }
                            }
                        }
                    }
                }
            })
        }
    }
}