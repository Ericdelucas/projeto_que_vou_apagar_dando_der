package com.example.app.settings

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.auth.TokenStore
import com.example.app.network.ApiClient
import com.example.app.network.UserSearchItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BlockUserDialogFragment : DialogFragment() {
    private var request: Call<List<UserSearchItem>>? = null
    private var results: List<UserSearchItem> = emptyList()
    private val model get() = ViewModelProvider(requireParentFragment())[SettingsViewModel::class.java]

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val input = EditText(context).apply {
            hint = getString(R.string.settings_search_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
        }
        val search = Button(context).apply { setText(R.string.settings_search) }
        val progress = ProgressBar(context).apply { visibility = View.GONE }
        val message = TextView(context).apply { setText(R.string.settings_search_hint) }
        val list = ListView(context)
        content.addView(input); content.addView(search); content.addView(progress); content.addView(message)
        content.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            (280 * resources.displayMetrics.density).toInt()))
        search.setOnClickListener {
            val tokens = TokenStore(context)
            val token = tokens.getToken()
            if (token.isNullOrBlank()) { model.expire(token); dismiss(); return@setOnClickListener }
            search.isEnabled = false; progress.visibility = View.VISIBLE; message.visibility = View.GONE
            results = emptyList(); list.adapter = null
            val call = ApiClient.users.search("Bearer $token", input.text.toString().trim())
            request = call
            fun fail() {
                search.isEnabled = true; progress.visibility = View.GONE
                message.setText(R.string.users_error); message.visibility = View.VISIBLE
            }
            call.enqueue(object : Callback<List<UserSearchItem>> {
                override fun onResponse(call: Call<List<UserSearchItem>>, response: Response<List<UserSearchItem>>) {
                    if (request !== call) return
                    request = null
                    if (tokens.getToken() != token) { dismiss(); return }
                    if (!response.isSuccessful || response.body() == null) {
                        response.errorBody()?.close()
                        if (response.code() == 401) { model.expire(token); dismiss() } else fail()
                        return
                    }
                    val blockedIds = model.state.value?.items.orEmpty().map { it.blockedUserId }.toSet()
                    results = response.body().orEmpty().filter { it.id !in blockedIds }
                    search.isEnabled = true; progress.visibility = View.GONE
                    message.setText(R.string.users_empty)
                    message.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                    list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1,
                        results.map { it.name ?: getString(R.string.settings_unnamed_user) })
                }
                override fun onFailure(call: Call<List<UserSearchItem>>, error: Throwable) {
                    if (request !== call) return
                    request = null
                    if (!call.isCanceled) fail()
                }
            })
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val user = results.getOrNull(position) ?: return@setOnItemClickListener
            MaterialAlertDialogBuilder(context).setTitle(R.string.settings_block)
                .setMessage(getString(R.string.settings_block_confirmation,
                    user.name ?: getString(R.string.settings_unnamed_user)))
                .setNegativeButton(R.string.settings_cancel, null)
                .setPositiveButton(R.string.settings_block) { _, _ -> model.block(user.id); dismiss() }.show()
        }
        return MaterialAlertDialogBuilder(context).setTitle(R.string.settings_block)
            .setView(content).setNegativeButton(R.string.settings_cancel, null).create()
    }

    override fun onDestroyView() {
        val call = request; request = null; call?.cancel()
        results = emptyList()
        super.onDestroyView()
    }
}
