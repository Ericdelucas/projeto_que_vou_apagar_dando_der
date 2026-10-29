package com.example.app.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.databinding.ItemSellerProductBinding
import com.example.app.feed.formatPrice
import com.example.app.network.SellerProduct

class SellerAdapter(private val onRequestsClick: (SellerProduct) -> Unit) :
    ListAdapter<SellerProduct, SellerAdapter.Holder>(object : DiffUtil.ItemCallback<SellerProduct>() {
        override fun areItemsTheSame(old: SellerProduct, new: SellerProduct) = old.id == new.id
        override fun areContentsTheSame(old: SellerProduct, new: SellerProduct) = old == new
    }) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemSellerProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = getItem(position)
        holder.ui.apply {
            productTitle.text = product.title
            productStatus.text = root.context.getString(R.string.seller_price_status, formatPrice(product.price),
                root.context.getString(if (product.status == "ativo") R.string.seller_active else R.string.seller_inactive))
            requestsCount.text = product.requestsCount.toString()
            pendingCount.text = root.context.getString(R.string.seller_pending_short, product.pendingRequestsCount)
            requestsCell.contentDescription = root.context.getString(R.string.seller_requests_summary,
                product.requestsCount, product.pendingRequestsCount, product.acceptedRequestsCount, product.rejectedRequestsCount)
            requestsCell.setOnClickListener { onRequestsClick(product) }
        }
    }

    class Holder(val ui: ItemSellerProductBinding) : RecyclerView.ViewHolder(ui.root)
}
