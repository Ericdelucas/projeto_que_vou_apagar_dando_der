package com.example.app.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.ItemBlockedUserBinding
import com.example.app.network.BlockedUser

class BlockedUsersAdapter(private val onUnblock: (BlockedUser) -> Unit) :
    ListAdapter<BlockedUser, BlockedUsersAdapter.Holder>(object : DiffUtil.ItemCallback<BlockedUser>() {
        override fun areItemsTheSame(a: BlockedUser, b: BlockedUser) = a.id == b.id
        override fun areContentsTheSame(a: BlockedUser, b: BlockedUser) = a == b
    }) {
    private var busy = false
    fun update(state: SettingsState) {
        if (busy != state.busy) { busy = state.busy; notifyItemRangeChanged(0, itemCount) }
        submitList(state.items)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemBlockedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.ui.apply {
            blockedName.text = item.user.name?.takeIf { it.isNotBlank() }
                ?: root.context.getString(R.string.settings_unnamed_user)
            blockedRole.setText(if (item.user.role == "vendedor") R.string.users_seller else R.string.users_buyer)
            Glide.with(root).load(com.example.app.network.MediaUrl.resolve(item.user.avatarUrl)).circleCrop()
                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(blockedAvatar)
            unblockButton.isEnabled = !busy
            unblockButton.setOnClickListener { onUnblock(item) }
        }
    }
    override fun onViewRecycled(holder: Holder) {
        Glide.with(holder.itemView).clear(holder.ui.blockedAvatar)
        super.onViewRecycled(holder)
    }
    class Holder(val ui: ItemBlockedUserBinding) : RecyclerView.ViewHolder(ui.root)
}
