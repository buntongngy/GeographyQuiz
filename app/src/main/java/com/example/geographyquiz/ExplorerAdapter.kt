package com.example.geographyquiz

import ExplorerItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ExplorerAdapter(
    private val items: List<ExplorerItem>,
    private val onItemClick: (ExplorerItem) -> Unit
) : RecyclerView.Adapter<ExplorerAdapter.ExplorerViewHolder>() {

    class ExplorerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.explorerTitle)  // Matches XML ID
        val icon: ImageView = itemView.findViewById(R.id.explorerIcon)   // Matches XML ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExplorerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_explorer, parent, false)
        return ExplorerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExplorerViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title      // Now correctly references the TextView
        holder.icon.setImageResource(item.iconRes)  // Correctly references the ImageView

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}