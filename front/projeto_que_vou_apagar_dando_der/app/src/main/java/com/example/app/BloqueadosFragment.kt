package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.auth.openLogin
import com.example.app.databinding.FragmentBloqueadosBinding
import com.example.app.settings.BlockedUsersAdapter
import com.example.app.settings.BlockUserDialogFragment
import com.example.app.settings.SettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BloqueadosFragment : Fragment() {
    private var binding: FragmentBloqueadosBinding? = null
    private var adapter: BlockedUsersAdapter? = null
    private lateinit var model: SettingsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentBloqueadosBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        model = ViewModelProvider(this)[SettingsViewModel::class.java]
        val ui = binding ?: return
        adapter = BlockedUsersAdapter { block ->
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.settings_unblock)
                .setMessage(getString(R.string.settings_unblock_confirmation,
                    block.user.name ?: getString(R.string.settings_unnamed_user)))
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.settings_unblock) { _, _ -> model.unblock(block.id) }.show()
        }
        ui.blockedList.layoutManager = LinearLayoutManager(requireContext())
        ui.blockedList.adapter = adapter
        ui.blockedBack.setOnClickListener { findNavController().navigateUp() }
        ui.blockUserButton.setOnClickListener {
            if (childFragmentManager.findFragmentByTag("block_user") == null)
                BlockUserDialogFragment().show(childFragmentManager, "block_user")
        }
        ui.blockedRetry.setOnClickListener { model.load() }
        model.state.observe(viewLifecycleOwner) { status ->
            if (status.authRequired) { openLogin(); return@observe }
            adapter?.update(status)
            ui.blockedProgress.visibility = if (status.busy) View.VISIBLE else View.GONE
            ui.blockUserButton.isEnabled = !status.busy
            ui.blockedList.visibility = if (status.items.isEmpty()) View.GONE else View.VISIBLE
            val message = status.error ?: if (status.loaded && status.items.isEmpty()) R.string.settings_blocked_empty else null
            ui.blockedMessage.visibility = if (message != null) View.VISIBLE else View.GONE
            message?.let { ui.blockedMessage.setText(it) }
            ui.blockedRetry.visibility = if (status.error != null && !status.busy) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() { super.onResume(); model.load() }
    override fun onDestroyView() {
        binding?.blockedList?.adapter = null
        adapter = null
        binding = null
        super.onDestroyView()
    }
}
