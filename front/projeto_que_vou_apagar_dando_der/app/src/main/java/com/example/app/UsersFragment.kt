package com.example.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.auth.TokenStore
import com.example.app.auth.openLogin
import com.example.app.databinding.FragmentUsersBinding
import com.example.app.network.ApiClient
import com.example.app.network.UserSearchItem
import com.example.app.users.UsersAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UsersFragment : Fragment() {
    private var binding: FragmentUsersBinding? = null
    private var adapter: UsersAdapter? = null
    private var call: Call<List<UserSearchItem>>? = null
    private val handler = Handler(Looper.getMainLooper())
    private val searchTask = Runnable { search() }
    private var filter: String? = null
    private var query = ""

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        query = state?.getString("query").orEmpty()
        filter = if (state != null) state.getString("filter") else arguments?.getString("filter")
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        FragmentUsersBinding.inflate(inflater, container, false).also { binding = it }.root
    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        val ui = binding ?: return
        adapter = UsersAdapter { id -> findNavController().navigate(R.id.action_users_to_profile,
            Bundle().apply { putInt("userId", id) }) }
        ui.usersList.layoutManager = LinearLayoutManager(requireContext())
        ui.usersList.adapter = adapter
        ui.searchInput.setText(query)
        ui.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s.toString()
                cancelSearch()
                ui.usersList.visibility = View.GONE
                ui.usersMessage.visibility = View.GONE
                ui.retryButton.visibility = View.GONE
                ui.usersProgress.visibility = View.VISIBLE
                handler.postDelayed(searchTask, 350)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        ui.searchButton.setOnClickListener { cancelSearch(); search() }
        ui.filterButton.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                val values = listOf(null, "comprador", "vendedor")
                val labels = listOf(R.string.marketplace_all, R.string.users_buyer, R.string.users_seller)
                values.forEachIndexed { index, value ->
                    menu.add(0, index, index, labels[index]).apply { isCheckable = true; isChecked = value == filter }
                }
                menu.setGroupCheckable(0, true, true)
                setOnMenuItemClickListener { filter = values[it.itemId]; cancelSearch(); search(); true }
                show()
            }
        }
        search()
    }
    private fun cancelSearch() {
        handler.removeCallbacks(searchTask)
        val pending = call
        call = null
        pending?.cancel()
    }
    private fun search() {
        val ui = binding ?: return
        ui.filterLabel.setText(when (filter) {
            "comprador" -> R.string.users_buyer
            "vendedor" -> R.string.users_seller
            else -> R.string.filtragem
        })
        ui.usersProgress.visibility = View.VISIBLE
        ui.usersMessage.visibility = View.GONE
        ui.retryButton.visibility = View.GONE
        ui.usersList.visibility = View.GONE
        val token = TokenStore(requireContext()).getToken()
        if (token.isNullOrBlank()) { showError(R.string.favorites_session_expired, true); return }
        val pending = ApiClient.users.search("Bearer $token", query.trim(), filter)
        call = pending
        pending.enqueue(object : Callback<List<UserSearchItem>> {
            override fun onResponse(call: Call<List<UserSearchItem>>, response: Response<List<UserSearchItem>>) {
                if (this@UsersFragment.call !== call) return
                this@UsersFragment.call = null
                val ui = binding ?: return
                val users = response.body()
                if (!response.isSuccessful || users == null) {
                    response.errorBody()?.close()
                    if (response.code() == 401) {
                        val store = TokenStore(requireContext())
                        if (store.getToken() == token) store.clear()
                    }
                    showError(if (response.code() == 401) R.string.favorites_session_expired else R.string.users_error,
                        response.code() == 401)
                    return
                }
                ui.usersProgress.visibility = View.GONE
                adapter?.submitList(users)
                ui.usersList.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
                ui.usersMessage.setText(R.string.users_empty)
                ui.usersMessage.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onFailure(call: Call<List<UserSearchItem>>, error: Throwable) {
                if (this@UsersFragment.call !== call) return
                this@UsersFragment.call = null
                if (!call.isCanceled) showError(R.string.users_error)
            }
        })
    }
    private fun showError(message: Int, login: Boolean = false) {
        binding?.apply {
            usersProgress.visibility = View.GONE
            usersMessage.setText(message)
            usersMessage.visibility = View.VISIBLE
            usersList.visibility = View.GONE
            retryButton.visibility = View.VISIBLE
            retryButton.setText(if (login) R.string.login_enter else R.string.feed_retry)
            retryButton.setOnClickListener { if (login) openLogin() else { cancelSearch(); search() } }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("query", query)
        outState.putString("filter", filter)
    }
    override fun onDestroyView() {
        cancelSearch()
        binding?.usersList?.adapter = null
        adapter = null
        binding = null
        super.onDestroyView()
    }
}
