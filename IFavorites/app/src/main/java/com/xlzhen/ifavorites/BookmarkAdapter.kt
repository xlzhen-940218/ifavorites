// File: app/src/main/java/com/example/cloudfavorites/BookmarkAdapter.kt
package com.xlzhen.ifavorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BookmarkAdapter(
    private val bookmarks: List<Bookmark>,
    private val isGrid: Boolean,
    private val baseUrl: String,
    private val onClick: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.title)
        val descText: TextView = view.findViewById(R.id.description)
        val coverImage: ImageView? = view.findViewById(R.id.cover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (isGrid) R.layout.item_bookmark_grid else R.layout.item_bookmark_list
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.titleText.text = bookmark.title
        holder.descText.text = bookmark.description
        holder.coverImage?.let {
            if (bookmark.cover != null) {
                
                Glide.with(holder.itemView.context).load(baseUrl + bookmark.cover.replace("\\", "/")).into(it)
            }
        }
        holder.itemView.setOnClickListener { onClick(bookmark) }
    }

    override fun getItemCount(): Int = bookmarks.size
}