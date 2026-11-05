package com.example.app.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.ItemProfileMediaBinding
import com.example.app.network.ProfileMedia

class ProfileMediaAdapter(private val onClick: (ProfileMedia) -> Unit) :
    ListAdapter<ProfileMedia, ProfileMediaAdapter.Holder>(object : DiffUtil.ItemCallback<ProfileMedia>() {
        override fun areItemsTheSame(old: ProfileMedia, new: ProfileMedia) = old.mediaId == new.mediaId
        override fun areContentsTheSame(old: ProfileMedia, new: ProfileMedia) = old == new
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemProfileMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val media = getItem(position)
        holder.ui.apply {
            mediaTitle.text = media.title
            mediaImage.contentDescription = media.title
            Glide.with(root).load(com.example.app.network.MediaUrl.resolve(media.imageUrl)).placeholder(R.drawable.ic_steering_wheel)
                .error(R.drawable.ic_steering_wheel).into(mediaImage)
            playIcon.visibility = if (media.videoUrl.isNullOrBlank()) View.GONE else View.VISIBLE
            root.setOnClickListener { onClick(media) }
        }
    }
    override fun onViewRecycled(holder: Holder) {
        Glide.with(holder.itemView).clear(holder.ui.mediaImage)
        super.onViewRecycled(holder)
    }
    class Holder(val ui: ItemProfileMediaBinding) : RecyclerView.ViewHolder(ui.root)
}
