package com.billgenie.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.billgenie.R
import com.billgenie.model.RecipeDisplayItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class RecipeIngredientAdapter(
    private val onEditIngredient: (RecipeDisplayItem) -> Unit,
    private val onDeleteIngredient: (RecipeDisplayItem) -> Unit
) : RecyclerView.Adapter<RecipeIngredientAdapter.IngredientViewHolder>() {

    private var ingredients = listOf<RecipeDisplayItem>()

    fun updateIngredients(newIngredients: List<RecipeDisplayItem>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_ingredient, parent, false)
        return IngredientViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(ingredients[position])
    }

    override fun getItemCount() = ingredients.size

    inner class IngredientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardIngredient: MaterialCardView = itemView.findViewById(R.id.cardIngredient)
        private val textIngredientName: TextView = itemView.findViewById(R.id.textIngredientName)
        private val textQuantity: TextView = itemView.findViewById(R.id.textQuantity)
        private val textCost: TextView = itemView.findViewById(R.id.textCost)
        private val textNotes: TextView = itemView.findViewById(R.id.textNotes)
        private val buttonEditIngredient: MaterialButton = itemView.findViewById(R.id.buttonEditIngredient)
        private val buttonDeleteIngredient: MaterialButton = itemView.findViewById(R.id.buttonDeleteIngredient)

        fun bind(recipeItem: RecipeDisplayItem) {
            textIngredientName.text = recipeItem.ingredientName
            textQuantity.text = "${recipeItem.quantityRequired} ${recipeItem.unit}"
            
            // Calculate estimated cost (quantity * cost per unit would need ingredient cost data)
            textCost.text = "Recipe item"

            // Show notes if available
            if (!recipeItem.notes.isNullOrEmpty()) {
                textNotes.text = recipeItem.notes
                textNotes.visibility = View.VISIBLE
            } else {
                textNotes.visibility = View.GONE
            }

            // Highlight optional ingredients
            if (recipeItem.isOptional) {
                cardIngredient.strokeWidth = 2
                cardIngredient.strokeColor = itemView.context.getColor(R.color.warning)
                textIngredientName.text = "${recipeItem.ingredientName} (Optional)"
            } else {
                cardIngredient.strokeWidth = 1
                cardIngredient.strokeColor = itemView.context.getColor(R.color.surface_variant)
            }

            buttonEditIngredient.setOnClickListener {
                onEditIngredient(recipeItem)
            }

            buttonDeleteIngredient.setOnClickListener {
                onDeleteIngredient(recipeItem)
            }
        }
    }
}