package com.example.app.favorites

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

fun Fragment.observeFavorites(render: (FavoritesState) -> Unit): FavoritesViewModel {
    val model = ViewModelProvider(requireActivity())[FavoritesViewModel::class.java]
    model.load() // Confere a sessão antes de expor dados de um usuário anterior.
    model.state.observe(viewLifecycleOwner) { render(it) }
    model.message.observe(viewLifecycleOwner) { message ->
        if (message != null) {
            model.consumeMessage()
            val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
            if (message == R.string.favorites_session_expired) {
                snackbar.setAction(R.string.login_enter) { openFavoritesLogin() }
            } else snackbar.setAction(R.string.feed_retry) { model.load() }
            snackbar.show()
        }
    }
    return model
}

fun Fragment.openFavoritesLogin() {
    findNavController().navigate(R.id.nav_login, null,
        NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
}

fun MaterialButton.bindFavorite(productId: Int, state: FavoritesState, onClick: (Int) -> Unit) {
    val saved = productId in state.productIds
    isEnabled = state.canToggle
    isSelected = saved
    setText(when {
        state.pendingProductId == productId -> R.string.favorites_saving
        saved -> R.string.favorites_saved
        else -> R.string.favorites_save
    })
    contentDescription = context.getString(if (saved) R.string.favorites_remove else R.string.favorites_save)
    setIconResource(R.drawable.ic_heart)
    iconTint = ColorStateList.valueOf(ContextCompat.getColor(context,
        if (saved) R.color.red_primary else R.color.gray_text))
    setOnClickListener { onClick(productId) }
}
