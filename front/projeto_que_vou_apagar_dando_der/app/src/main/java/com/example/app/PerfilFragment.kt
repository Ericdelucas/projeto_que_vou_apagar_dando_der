package com.example.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.app.auth.openLogin
import com.example.app.databinding.FragmentPerfilBinding
import com.example.app.users.EditProfileDialogFragment
import com.example.app.users.ProfileMediaAdapter
import com.example.app.users.ProfileMediaDialogFragment
import com.example.app.users.ProfileState
import com.example.app.users.ProfileViewModel

class PerfilFragment : Fragment() {
    private var binding: FragmentPerfilBinding? = null
    private var mediaAdapter: ProfileMediaAdapter? = null
    private lateinit var model: ProfileViewModel
    private val requestedId: Int? get() = arguments?.takeIf { it.containsKey("userId") }?.getInt("userId")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentPerfilBinding.inflate(inflater, container, false).also { binding = it }.root
    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        model = ViewModelProvider(this)[ProfileViewModel::class.java]
        val ui = binding ?: return
        mediaAdapter = ProfileMediaAdapter { media ->
            if (!media.videoUrl.isNullOrBlank() || media.productId == null) {
                if (childFragmentManager.findFragmentByTag("profile_media") == null) {
                    ProfileMediaDialogFragment().apply {
                        arguments = Bundle().apply {
                            putString("title", media.title); putString("image", media.imageUrl); putString("video", media.videoUrl)
                        }
                    }.show(childFragmentManager, "profile_media")
                }
            } else findNavController().navigate(R.id.nav_product_details,
                Bundle().apply { putInt("product_id", media.productId) })
        }
        ui.mediaGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        ui.mediaGrid.adapter = mediaAdapter
        ui.editProfile.setOnClickListener {
            if (model.state.value?.profile?.isOwner == true && childFragmentManager.findFragmentByTag("edit_profile") == null)
                EditProfileDialogFragment().show(childFragmentManager, "edit_profile")
        }
        val share = View.OnClickListener {
            val user = model.state.value?.profile ?: return@OnClickListener
            val text = getString(R.string.profile_share_text, user.name.orEmpty(), "skippecas://users/${user.id}")
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
            }, getString(R.string.btn_compartilhar_perfil)))
        }
        ui.shareProfile.setOnClickListener(share)
        ui.headerShare.setOnClickListener(share)
        ui.findUsers.setOnClickListener { findNavController().navigate(R.id.nav_users) }
        ui.profileMenu.setOnClickListener { findNavController().navigate(R.id.nav_configuracoes) }
        model.state.observe(viewLifecycleOwner) { render(it) }
        childFragmentManager.setFragmentResultListener("post_created", viewLifecycleOwner) { _, _ -> model.load(requestedId) }
        ui.newPost.setOnClickListener {
            if (model.state.value?.profile?.isOwner == true && childFragmentManager.findFragmentByTag("new_post") == null)
                com.example.app.users.NewPostDialogFragment().show(childFragmentManager, "new_post")
        }
        model.load(requestedId)
    }
    private fun render(state: ProfileState) {
        val ui = binding ?: return
        ui.profileProgress.visibility = if (state.loading) View.VISIBLE else View.GONE
        ui.profileContent.visibility = if (state.profile == null || state.error != null) View.GONE else View.VISIBLE
        ui.profileMessage.visibility = if (state.error != null) View.VISIBLE else View.GONE
        state.error?.let { ui.profileMessage.setText(it) }
        ui.retryButton.visibility = if (state.error != null) View.VISIBLE else View.GONE
        val needsLogin = state.error == R.string.favorites_session_expired
        ui.retryButton.setText(if (needsLogin) R.string.login_enter else R.string.feed_retry)
        ui.retryButton.setOnClickListener { if (needsLogin) openLogin() else model.load(requestedId) }
        ui.headerShare.isEnabled = state.profile != null && state.error == null
        ui.newPost.visibility = if (state.profile?.isOwner == true && state.error == null && !state.loading) View.VISIBLE else View.GONE
        val user = state.profile ?: return
        ui.userName.text = user.name
        ui.userBio.text = user.bio.orEmpty()
        ui.userBio.visibility = if (user.bio.isNullOrBlank()) View.GONE else View.VISIBLE
        ui.postsCount.text = getString(R.string.profile_posts, user.postsCount)
        ui.followersCount.text = getString(R.string.profile_followers, user.followersCount)
        ui.followingCount.text = getString(R.string.profile_following, user.followingCount)
        ui.editProfile.visibility = if (user.isOwner) View.VISIBLE else View.GONE
        ui.editProfile.isEnabled = !state.loading && !state.saving
        Glide.with(this).load(com.example.app.network.MediaUrl.resolve(user.avatarUrl)).circleCrop().placeholder(R.drawable.ic_person)
            .error(R.drawable.ic_person).into(ui.userAvatar)
        mediaAdapter?.submitList(user.media)
        ui.mediaEmpty.visibility = if (user.media.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onDestroyView() {
        binding?.mediaGrid?.adapter = null
        mediaAdapter = null
        binding = null
        super.onDestroyView()
    }
}
