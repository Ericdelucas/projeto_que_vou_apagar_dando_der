package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.app.auth.openLogin
import com.example.app.databinding.FragmentConfiguracoesBinding
import com.example.app.settings.SettingsViewModel
import com.example.app.settings.ThemeSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class ConfiguracoesFragment : Fragment() {
    private var binding: FragmentConfiguracoesBinding? = null
    private lateinit var model: SettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentConfiguracoesBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        model = ViewModelProvider(this)[SettingsViewModel::class.java]
        val ui = binding ?: return
        ui.btnSalvos.setOnClickListener { findNavController().navigate(R.id.action_nav_configuracoes_to_favoritosFragment) }
        ui.btnBloqueados.setOnClickListener { findNavController().navigate(R.id.nav_bloqueados) }
        ui.btnLoja.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.loja)
                .setMessage(R.string.settings_store_info)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.settings_open_store) { _, _ ->
                    findNavController().navigate(R.id.nav_marketplace)
                }.show()
        }
        ui.btnTemas.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.temas)
                .setSingleChoiceItems(R.array.settings_themes, ThemeSettings.selected(requireContext())) { dialog, index ->
                    dialog.dismiss()
                    ThemeSettings.select(requireContext(), index)
                }.setNegativeButton(R.string.settings_cancel, null).show()
        }
        ui.btnExcluirConta.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.excluir_conta)
                .setMessage(R.string.settings_delete_confirmation)
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.excluir_conta) { _, _ -> model.deleteAccount() }
                .show().getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.red_primary))
        }
        model.state.observe(viewLifecycleOwner) { status ->
            if (status.accountDeleted || status.authRequired) { openLogin(); return@observe }
            listOf(ui.btnSalvos, ui.btnBloqueados, ui.btnLoja, ui.btnTemas, ui.btnExcluirConta)
                .forEach { it.isEnabled = !status.busy }
            ui.settingsProgress.visibility = if (status.deleting) View.VISIBLE else View.GONE
            status.error?.let {
                Snackbar.make(ui.root, it, Snackbar.LENGTH_LONG).show()
                model.clearError()
            }
        }
    }

    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
