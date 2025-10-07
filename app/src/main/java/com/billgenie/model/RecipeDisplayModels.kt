package com.billgenie.model

// Display models for ingredients management
data class RecipeDisplayItem(
    val id: Long = 0,
    val menuItemId: Long,
    val ingredientId: Long,
    val quantityRequired: Double,
    val unit: String,
    val notes: String?,
    val isOptional: Boolean,
    val preparationStep: Int,
    val createdAt: Long,
    val ingredientName: String,
    val ingredientUnit: String,
    val menuItemName: String
) {
    // Helper property to get Recipe object
    val recipe: Recipe
        get() = Recipe(
            id = id,
            menuItemId = menuItemId,
            ingredientId = ingredientId,
            quantityRequired = quantityRequired,
            unit = unit,
            notes = notes,
            isOptional = isOptional,
            preparationStep = preparationStep,
            createdAt = createdAt
        )
}

data class MenuItemWithIngredients(
    val menuItem: MenuItem,
    val ingredients: List<RecipeDisplayItem>
)

data class IngredientWithRecipes(
    val ingredient: Ingredient,
    val usedInItems: List<MenuItem>
)