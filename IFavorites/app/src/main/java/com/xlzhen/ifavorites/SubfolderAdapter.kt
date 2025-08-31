// File: app/src/main/java/com/example/cloudfavorites/SubfolderAdapter.kt
package com.xlzhen.ifavorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubfolderAdapter(private val folders: List<Folder>, private val onClick: (Folder) -> Unit) : RecyclerView.Adapter<SubfolderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.subfolder_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subfolder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.nameText.text = folder.name
        holder.itemView.setOnClickListener { onClick(folder) }
    }

    override fun getItemCount(): Int = folders.size
}