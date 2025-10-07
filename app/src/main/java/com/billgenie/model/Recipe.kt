package com.billgenie.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipes",
    foreignKeys = [
        ForeignKey(
            entity = MenuItem::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["menuItemId"]),
        Index(value = ["ingredientId"]),
        Index(value = ["menuItemId", "ingredientId"], unique = true)
    ]
)
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val menuItemId: Long,
    val ingredientId: Long,
    val quantityRequired: Double, // quantity of ingredient needed for one serving
    val unit: String, // unit for this recipe (e.g., "grams", "ml", "pieces")
    val notes: String? = null, // e.g., "finely chopped", "optional"
    val isOptional: Boolean = false,
    val preparationStep: Int = 1, // order of adding ingredient in preparation
    val createdAt: Long = System.currentTimeMillis()
)