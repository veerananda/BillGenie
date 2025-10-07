package com.billgenie.utils

/**
 * Utility class for converting between different units
 * Standardizes all units to base units for storage:
 * - Weight: grams
 * - Volume: ml
 * - Count: pieces
 */
object UnitConverter {
    
    // Base units for standardization
    const val BASE_WEIGHT_UNIT = "grams"
    const val BASE_VOLUME_UNIT = "ml"
    const val BASE_COUNT_UNIT = "pieces"
    
    // Weight conversion factors to grams
    private val weightConversions = mapOf(
        "grams" to 1.0,
        "g" to 1.0,
        "kg" to 1000.0,
        "kilograms" to 1000.0,
        "oz" to 28.3495,
        "ounces" to 28.3495,
        "lbs" to 453.592,
        "pounds" to 453.592
    )
    
    // Volume conversion factors to ml
    private val volumeConversions = mapOf(
        "ml" to 1.0,
        "milliliters" to 1.0,
        "liters" to 1000.0,
        "l" to 1000.0,
        "cups" to 236.588,
        "tbsp" to 14.7868,
        "tablespoons" to 14.7868,
        "tsp" to 4.92892,
        "teaspoons" to 4.92892,
        "fl oz" to 29.5735,
        "fluid ounces" to 29.5735
    )
    
    // Count units (no conversion needed)
    private val countUnits = setOf(
        "pieces", "pcs", "items", "units", "count"
    )
    
    /**
     * Convert any unit to its base unit
     */
    fun toBaseUnit(quantity: Double, unit: String): Pair<Double, String> {
        val normalizedUnit = unit.lowercase().trim()
        
        return when {
            weightConversions.containsKey(normalizedUnit) -> {
                val baseQuantity = quantity * weightConversions[normalizedUnit]!!
                Pair(baseQuantity, BASE_WEIGHT_UNIT)
            }
            volumeConversions.containsKey(normalizedUnit) -> {
                val baseQuantity = quantity * volumeConversions[normalizedUnit]!!
                Pair(baseQuantity, BASE_VOLUME_UNIT)
            }
            countUnits.contains(normalizedUnit) -> {
                Pair(quantity, BASE_COUNT_UNIT)
            }
            else -> {
                // Unknown unit, treat as base unit
                Pair(quantity, unit)
            }
        }
    }
    
    /**
     * Convert from base unit to display unit
     */
    fun fromBaseUnit(baseQuantity: Double, baseUnit: String, targetUnit: String): Double {
        val normalizedTarget = targetUnit.lowercase().trim()
        
        return when (baseUnit) {
            BASE_WEIGHT_UNIT -> {
                val factor = weightConversions[normalizedTarget] ?: 1.0
                baseQuantity / factor
            }
            BASE_VOLUME_UNIT -> {
                val factor = volumeConversions[normalizedTarget] ?: 1.0
                baseQuantity / factor
            }
            BASE_COUNT_UNIT -> baseQuantity
            else -> baseQuantity
        }
    }
    
    /**
     * Get the most appropriate display unit for a base quantity
     */
    fun getBestDisplayUnit(baseQuantity: Double, baseUnit: String): Pair<Double, String> {
        return when (baseUnit) {
            BASE_WEIGHT_UNIT -> {
                when {
                    baseQuantity >= 1000 -> {
                        val kgQuantity = baseQuantity / 1000.0
                        Pair(kgQuantity, "kg")
                    }
                    else -> Pair(baseQuantity, "grams")
                }
            }
            BASE_VOLUME_UNIT -> {
                when {
                    baseQuantity >= 1000 -> {
                        val literQuantity = baseQuantity / 1000.0
                        Pair(literQuantity, "liters")
                    }
                    else -> Pair(baseQuantity, "ml")
                }
            }
            BASE_COUNT_UNIT -> Pair(baseQuantity, "pieces")
            else -> Pair(baseQuantity, baseUnit)
        }
    }
    
    /**
     * Format display text for quantities
     */
    fun formatDisplayText(quantity: Double, unit: String): String {
        val (displayQuantity, displayUnit) = getBestDisplayUnit(quantity, unit)
        return when {
            displayQuantity == displayQuantity.toInt().toDouble() -> {
                "${displayQuantity.toInt()} $displayUnit"
            }
            else -> {
                String.format("%.1f %s", displayQuantity, displayUnit)
            }
        }
    }
    
    /**
     * Check if a unit is a weight unit
     */
    fun isWeightUnit(unit: String): Boolean {
        return weightConversions.containsKey(unit.lowercase().trim())
    }
    
    /**
     * Check if a unit is a volume unit
     */
    fun isVolumeUnit(unit: String): Boolean {
        return volumeConversions.containsKey(unit.lowercase().trim())
    }
    
    /**
     * Check if a unit is a count unit
     */
    fun isCountUnit(unit: String): Boolean {
        return countUnits.contains(unit.lowercase().trim())
    }
}