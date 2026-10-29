package com.example.app.seller

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.databinding.DialogNewProductBinding
import com.example.app.network.NewSellerProduct
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NewProductDialogFragment : DialogFragment() {
    private var binding: DialogNewProductBinding? = null
    private lateinit var model: NewProductViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ui = DialogNewProductBinding.inflate(layoutInflater)
        binding = ui
        model = ViewModelProvider(this)[NewProductViewModel::class.java]
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.seller_new_product).setView(ui.root)
            .setPositiveButton(R.string.seller_publish, null)
            .setNegativeButton(android.R.string.cancel, null).create()
        model.state.observe(this) { state ->
            if (state.productId != null) { dismiss(); return@observe }
            binding?.apply {
                savingProgress.visibility = if (state.saving) View.VISIBLE else View.GONE
                errorMessage.visibility = if (state.error == null) View.GONE else View.VISIBLE
                state.error?.let { errorMessage.setText(it) }
                listOf(titleInput, priceInput, categoryInput, modelInput, imageInput, videoInput, locationInput)
                    .forEach { it.isEnabled = !state.saving }
            }
            isCancelable = !state.saving
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !state.saving && !state.mustClose
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !state.saving
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val product = readProduct() ?: return@setOnClickListener
            model.create(product)
        }
        // Os botões são criados depois de onCreateDialog, inclusive após uma rotação.
        val state = model.state.value ?: ProductCreationState()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !state.saving && !state.mustClose
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !state.saving
    }

    private fun readProduct(): NewSellerProduct? {
        val ui = binding ?: return null
        fun value(field: EditText) = field.text.toString().trim()
        for (field in listOf(ui.titleInput, ui.categoryInput, ui.modelInput)) {
            field.error = null
            if (value(field).isBlank() || value(field).length > 255) {
                field.error = getString(R.string.seller_required); field.requestFocus(); return null
            }
        }
        val price = value(ui.priceInput).replace(',', '.')
        if (!price.matches(Regex("\\d{1,8}(\\.\\d{1,2})?")) || price.toBigDecimal().signum() <= 0) {
            ui.priceInput.error = getString(R.string.seller_price_error); return null
        }
        val image = value(ui.imageInput).takeIf { it.isNotBlank() }
        val video = value(ui.videoInput).takeIf { it.isNotBlank() }
        if (image == null && video == null) {
            ui.imageInput.error = getString(R.string.seller_media_error); return null
        }
        for (field in listOf(ui.imageInput, ui.videoInput)) {
            field.error = null
            val url = value(field)
            if (url.isNotEmpty()) {
                val uri = runCatching { java.net.URI(url) }.getOrNull()
                if (uri?.scheme !in listOf("http", "https") || uri?.host.isNullOrBlank() || url.length > 2048) {
                    field.error = getString(R.string.seller_url_error); return null
                }
            }
        }
        return NewSellerProduct(value(ui.titleInput), price, value(ui.categoryInput), value(ui.modelInput),
            image, video, value(ui.locationInput).takeIf { it.isNotBlank() })
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (isAdded) parentFragmentManager.setFragmentResult("seller_refresh", Bundle())
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
