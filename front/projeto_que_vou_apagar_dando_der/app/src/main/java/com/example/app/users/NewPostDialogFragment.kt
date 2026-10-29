package com.example.app.users

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.DialogNewPostBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NewPostDialogFragment : DialogFragment() {
    private var binding: DialogNewPostBinding? = null
    private lateinit var model: PostComposerViewModel
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            model.select(uri)
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        model = ViewModelProvider(this)[PostComposerViewModel::class.java]
        val ui = DialogNewPostBinding.inflate(layoutInflater)
        binding = ui
        if (model.state.value?.photo == null) savedInstanceState?.getString("photo")?.let { model.select(android.net.Uri.parse(it)) }
        ui.descriptionInput.setText(savedInstanceState?.getString("caption").orEmpty())
        ui.selectPhoto.setOnClickListener { picker.launch(arrayOf("image/*")) }
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.new_post_title)
            .setView(ui.root).setPositiveButton(R.string.new_post_publish, null)
            .setNegativeButton(android.R.string.cancel, null).create()
        model.state.observe(this) { state ->
            if (state.published) {
                parentFragmentManager.setFragmentResult("post_created", Bundle())
                dismiss(); return@observe
            }
            binding?.apply {
                selectPhoto.isEnabled = !state.loading
                descriptionInput.isEnabled = !state.loading
                postProgress.visibility = if (state.loading) View.VISIBLE else View.GONE
                postError.visibility = if (state.error != null) View.VISIBLE else View.GONE
                state.error?.let { postError.setText(it) }
                photoPreview.visibility = if (state.photo != null) View.VISIBLE else View.GONE
                Glide.with(this@NewPostDialogFragment).load(state.photo).into(photoPreview)
            }
            isCancelable = !state.loading
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !state.loading
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !state.loading
        }
        return dialog
    }
    override fun onStart() {
        super.onStart()
        (dialog as? AlertDialog)?.apply {
            getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = model.state.value?.loading != true
            getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = model.state.value?.loading != true
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { model.publish(binding?.descriptionInput?.text.toString().trim()) }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("photo", model.state.value?.photo?.toString())
        outState.putString("caption", binding?.descriptionInput?.text?.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
