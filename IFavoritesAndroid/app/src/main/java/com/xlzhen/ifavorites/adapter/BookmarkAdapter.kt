package com.xlzhen.ifavorites.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.xlzhen.ifavorites.R
import com.xlzhen.ifavorites.api.Bookmark
import com.xlzhen.ifavorites.api.RetrofitClient
import com.xlzhen.ifavorites.api.ServerConfig
import com.xlzhen.ifavorites.databinding.AdapterItemBookmarkBinding

class BookmarkAdapter(
    private val bookmarks: List<Bookmark>,
    private val onBookmarkClick: (Bookmark) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.BookmarkViewHolder>() {

    // ViewHolder 类，用于持有并绑定视图
    inner class BookmarkViewHolder(private val binding: AdapterItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: Bookmark) {
            // 绑定数据到视图
            binding.textViewTitle.text = bookmark.title
            binding.textViewDescription.text = bookmark.description
            binding.textViewLink.text = bookmark.link

            // 如果有封面图，使用 Glide 加载图片
            if (!bookmark.cover.isNullOrEmpty()) {

                val coverUrl = ServerConfig.BASE_URL + bookmark.cover.replace("\\", "/")
                Glide.with(binding.imageViewCover.context)
                    .load(coverUrl)
                    .placeholder(R.drawable.photo_error) // 占位图
                    .error(R.drawable.photo_error) // 错误图
                    .centerCrop()
                    .into(binding.imageViewCover)
                binding.imageViewCover.visibility = ViewGroup.VISIBLE
            } else {
                // 如果没有封面图，隐藏 ImageView
                binding.imageViewCover.visibility = ViewGroup.GONE
            }

            // 设置点击事件
            binding.root.setOnClickListener {
                onBookmarkClick(bookmark)
            }
        }
    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        // 使用 View Binding 方式加载布局
        val binding = AdapterItemBookmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookmarkViewHolder(binding)
    }

    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val bookmark = bookmarks[position]
        holder.bind(bookmark)
    }

    // 返回列表项总数
    override fun getItemCount(): Int = bookmarks.size
}