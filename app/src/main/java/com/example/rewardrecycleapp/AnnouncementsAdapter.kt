package com.example.rewardrecycleapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rewardrecycleapp.databinding.ItemAnnouncementBinding

class AnnouncementsAdapter(
    private val items: List<Announcement>,
    private val onAnnouncementClick: (Announcement) -> Unit
) : RecyclerView.Adapter<AnnouncementsAdapter.AnnouncementViewHolder>() {

    private val legacyAnnouncementImages = listOf(
        "https://asianmirror.lk/wp-content/uploads/2025/02/10.jpg",
        "https://www.redcross.lk/wp-content/uploads/2016/06/IMG_0564.jpg",
        "https://www.navy.lk/assets/img/cleanSL/36.webp"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnnouncementViewHolder(binding, onAnnouncementClick)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        val imageUrl = items[position].imageUrl.takeIf { it.startsWith("http", ignoreCase = true) }
            ?: legacyAnnouncementImages[position % legacyAnnouncementImages.size]
        holder.bind(items[position], imageUrl)
    }

    override fun getItemCount(): Int = items.size

    class AnnouncementViewHolder(
        private val binding: ItemAnnouncementBinding,
        private val onAnnouncementClick: (Announcement) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Announcement, imageUrl: String) {
            binding.tvAnnouncementTitle.text = item.title
            binding.tvAnnouncementDescription.text = item.description
            Glide.with(binding.root.context)
                .load(imageUrl)
                .centerCrop()
                .into(binding.ivAnnouncementImage)

            binding.root.setOnClickListener { onAnnouncementClick(item) }
        }
    }
}
