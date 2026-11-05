package com.example.app.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.ItemUserBinding
import com.example.app.network.UserSearchItem

class UsersAdapter(private val onClick: (Int) -> Unit) :
    ListAdapter<UserSearchItem, UsersAdapter.Holder>(object : DiffUtil.ItemCallback<UserSearchItem>() {
        override fun areItemsTheSame(old: UserSearchItem, new: UserSearchItem) = old.id == new.id
        override fun areContentsTheSame(old: UserSearchItem, new: UserSearchItem) = old == new
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val user = getItem(position)
        holder.ui.apply {
            userName.text = user.name ?: root.context.getString(R.string.feed_unknown_author)
            userRole.setText(if (user.role == "vendedor") R.string.users_seller else R.string.users_buyer)
            activityChart.setActivity(user.activity)
            userActivity.text = root.context.getString(R.string.users_activity_total, user.activityCount)
            activityChart.contentDescription = root.context.getString(R.string.users_activity_description,
                user.activityCount, user.activity.orEmpty().joinToString(", "))
            Glide.with(root).load(com.example.app.network.MediaUrl.resolve(user.avatarUrl)).circleCrop().placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person).into(userAvatar)
            root.setOnClickListener { onClick(user.id) }
        }
    }
    override fun onViewRecycled(holder: Holder) {
        Glide.with(holder.itemView).clear(holder.ui.userAvatar)
        super.onViewRecycled(holder)
    }
    class Holder(val ui: ItemUserBinding) : RecyclerView.ViewHolder(ui.root)
}
