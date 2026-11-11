package com.example.app.users

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.DialogAvatarBinding
import com.example.app.network.MediaUrl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class AvatarDialogFragment : DialogFragment() {
    private var binding: DialogAvatarBinding? = null
    private lateinit var model: ProfileViewModel
    private var selectedPhoto: Uri? = null
    private var initialVersion = 0
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            selectedPhoto = uri
            model.clearSaveError()
            render(model.state.value ?: ProfileState())
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        model = ViewModelProvider(requireParentFragment())[ProfileViewModel::class.java]
        initialVersion = savedInstanceState?.getInt("version") ?: (model.state.value?.savedVersion ?: 0)
        selectedPhoto = savedInstanceState?.getString("photo")?.let(Uri::parse)
        model.clearSaveError()
        val ui = DialogAvatarBinding.inflate(layoutInflater)
        binding = ui
        ui.choosePhoto.setOnClickListener {
            runCatching { picker.launch(arrayOf("image/*")) }.onFailure {
                ui.uploadError.setText(R.string.post_photo_read_error)
                ui.uploadError.visibility = View.VISIBLE
            }
        }
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.avatar_title)
            .setView(ui.root).setPositiveButton(R.string.avatar_save, null)
            .setNegativeButton(android.R.string.cancel, null).create()
        model.state.observe(this) { state ->
            if (state.savedVersion > initialVersion) {
                parentFragment?.view?.let { Snackbar.make(it, R.string.avatar_saved, Snackbar.LENGTH_LONG).show() }
                dismiss()
            } else render(state)
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            selectedPhoto?.let { model.saveAvatar(it) }
        }
        render(model.state.value ?: ProfileState())
    }

    private fun render(state: ProfileState) {
        val ui = binding ?: return
        ui.choosePhoto.isEnabled = !state.saving
        ui.uploadProgress.visibility = if (state.saving) View.VISIBLE else View.GONE
        ui.avatarStatus.setText(when {
            state.saving -> R.string.avatar_saving
            selectedPhoto != null -> R.string.avatar_selected
            else -> R.string.avatar_hint
        })
        ui.uploadError.visibility = if (state.saveError == null) View.GONE else View.VISIBLE
        state.saveError?.let { ui.uploadError.setText(it) }
        Glide.with(this).load(selectedPhoto ?: MediaUrl.resolve(state.profile?.avatarUrl))
            .circleCrop().placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(ui.avatarPreview)
        isCancelable = !state.saving
        (dialog as? AlertDialog)?.apply {
            getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !state.saving && selectedPhoto != null && state.profile?.isOwner == true
            getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !state.saving
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("version", initialVersion)
        outState.putString("photo", selectedPhoto?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
