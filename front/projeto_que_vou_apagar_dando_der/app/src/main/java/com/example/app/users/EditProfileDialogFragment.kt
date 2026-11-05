package com.example.app.users

import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.databinding.DialogEditProfileBinding
import com.example.app.network.UpdateProfileRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditProfileDialogFragment : DialogFragment() {
    private var binding: DialogEditProfileBinding? = null
    private lateinit var model: ProfileViewModel
    private var initialVersion = 0
    private var photo: android.net.Uri? = null
    private val picker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { requireContext().contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photo = uri
            binding?.let { com.bumptech.glide.Glide.with(this).load(uri).circleCrop().into(it.avatarPreview) }
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        model = ViewModelProvider(requireParentFragment())[ProfileViewModel::class.java]
        initialVersion = savedInstanceState?.getInt("version") ?: (model.state.value?.savedVersion ?: 0)
        model.clearSaveError()
        val ui = DialogEditProfileBinding.inflate(layoutInflater)
        binding = ui
        photo = savedInstanceState?.getString("photo")?.let { android.net.Uri.parse(it) }
        ui.selectAvatar.setOnClickListener { picker.launch(arrayOf("image/*")) }
        com.bumptech.glide.Glide.with(this).load(photo ?: com.example.app.network.MediaUrl.resolve(model.state.value?.profile?.avatarUrl)).circleCrop().placeholder(R.drawable.ic_person).into(ui.avatarPreview)
        model.state.value?.profile?.let {
            ui.nameInput.setText(it.name)
            ui.emailInput.setText(it.email)
            ui.bioInput.setText(it.bio)
        }
        val dialog = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.btn_editar_perfil)
            .setView(ui.root).setPositiveButton(R.string.profile_save, null)
            .setNegativeButton(android.R.string.cancel, null).create()
        model.state.observe(this) { state ->
            if (state.savedVersion > initialVersion) { dismiss(); return@observe }
            binding?.apply {
                listOf(nameInput, emailInput, bioInput, selectAvatar).forEach { it.isEnabled = !state.saving }
                saveProgress.visibility = if (state.saving) View.VISIBLE else View.GONE
                saveError.visibility = if (state.saveError == null) View.GONE else View.VISIBLE
                state.saveError?.let { saveError.setText(it) }
            }
            isCancelable = !state.saving
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = !state.saving && state.profile?.isOwner == true
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = !state.saving
        }
        return dialog
    }
    override fun onStart() {
        super.onStart()
        val dialog = dialog as? AlertDialog ?: return
        val state = model.state.value ?: ProfileState()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !state.saving && state.profile?.isOwner == true
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !state.saving
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val ui = binding ?: return@setOnClickListener
            val name = ui.nameInput.text.toString().trim()
            val email = ui.emailInput.text.toString().trim()
            val avatar = model.state.value?.profile?.avatarUrl.orEmpty()
            if (name.isBlank()) { ui.nameInput.error = getString(R.string.seller_required); return@setOnClickListener }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ui.emailInput.error = getString(R.string.login_invalid_email); return@setOnClickListener
            }
            model.save(UpdateProfileRequest(name, email, avatar, ui.bioInput.text.toString().trim()), photo)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("version", initialVersion)
        outState.putString("photo", photo?.toString())
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
