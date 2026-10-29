package com.example.app.users

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.app.R
import com.example.app.databinding.DialogProfileMediaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileMediaDialogFragment : DialogFragment() {
    private var binding: DialogProfileMediaBinding? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ui = DialogProfileMediaBinding.inflate(layoutInflater)
        binding = ui
        val video = arguments?.getString("video")
        ui.mediaImage.visibility = if (video.isNullOrBlank()) View.VISIBLE else View.GONE
        ui.mediaVideo.visibility = if (video.isNullOrBlank()) View.GONE else View.VISIBLE
        if (video.isNullOrBlank()) Glide.with(this).load(arguments?.getString("image"))
            .placeholder(R.drawable.ic_steering_wheel).into(ui.mediaImage)
        return MaterialAlertDialogBuilder(requireContext()).setTitle(arguments?.getString("title"))
            .setView(ui.root).setPositiveButton(android.R.string.ok, null).create()
    }
    override fun onResume() {
        super.onResume()
        val url = arguments?.getString("video")?.takeIf { it.isNotBlank() } ?: return
        binding?.apply {
            val controls = MediaController(requireContext())
            controls.setAnchorView(mediaVideo)
            mediaVideo.setMediaController(controls)
            mediaVideo.setOnPreparedListener { it.start() }
            mediaVideo.setOnErrorListener { _, _, _ ->
                mediaVideo.stopPlayback()
                mediaError.visibility = View.VISIBLE
                true
            }
            mediaVideo.setVideoURI(Uri.parse(url))
        }
    }
    override fun onPause() {
        binding?.mediaVideo?.apply {
            setOnPreparedListener(null); setOnErrorListener(null); stopPlayback()
        }
        super.onPause()
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
