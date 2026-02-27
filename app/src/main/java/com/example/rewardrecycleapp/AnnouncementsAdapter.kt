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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnnouncementViewHolder {
        val binding = ItemAnnouncementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnnouncementViewHolder(binding, onAnnouncementClick)
    }

    override fun onBindViewHolder(holder: AnnouncementViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AnnouncementViewHolder(
        private val binding: ItemAnnouncementBinding,
        private val onAnnouncementClick: (Announcement) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Announcement) {
            binding.tvAnnouncementTitle.text = item.title
            binding.tvAnnouncementDescription.text = item.description
            if (item.imageUrl.startsWith("http", ignoreCase = true)) {
                Glide.with(binding.root.context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(binding.ivAnnouncementImage)
            } else {
                binding.ivAnnouncementImage.setImageResource(R.drawable.cleanoo2)
            }

            binding.root.setOnClickListener { onAnnouncementClick(item) }
        }
    }
}
