package com.example.app

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.app.auth.TokenStore
import com.example.app.databinding.FragmentLoginBinding
import com.example.app.network.ApiClient
import com.example.app.network.LoginRequest
import com.example.app.network.LoginResponse
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {
    private var registering = false
    private var registerCall: Call<com.example.app.network.RegisterResponse>? = null
    private var binding: FragmentLoginBinding? = null
    private var loginCall: Call<LoginResponse>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentLoginBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registering = savedInstanceState?.getBoolean("registering") ?: false
        updateMode()
        binding?.registerButton?.setOnClickListener {
            registering = !registering
            updateMode()
        }
        binding?.loginButton?.setOnClickListener { login() }
        binding?.passwordInput?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login()
                true
            } else false
        }
    }

    private fun login() {
        val ui = binding ?: return
        if (loginCall != null || registerCall != null) return
        val email = ui.emailInput.text.toString().trim().lowercase(java.util.Locale.ROOT)
        val password = ui.passwordInput.text.toString()
        ui.emailLayout.error = null
        ui.passwordLayout.error = null
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ui.emailLayout.error = getString(R.string.login_invalid_email)
            return
        }
        if (password.isEmpty()) {
            ui.passwordLayout.error = getString(R.string.login_password_required)
            return
        }

        if (registering) {
            register(email, password)
            return
        }
        setLoading(true)
        val call = ApiClient.auth.login(LoginRequest(email, password))
        loginCall = call
        // enqueue executa a rede fora da thread principal e entrega o resultado na UI.
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (loginCall !== call) return
                loginCall = null
                if (binding == null) return
                setLoading(false)
                val token = response.body()?.token
                if (response.isSuccessful && !token.isNullOrBlank()) {
                    TokenStore(requireContext()).saveToken(token)
                    binding?.passwordInput?.text?.clear()
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    response.errorBody()?.close()
                    showError(when (response.code()) {
                        400 -> R.string.login_invalid_data
                        401 -> R.string.login_invalid_credentials
                        429 -> R.string.login_too_many_requests
                        else -> R.string.login_server_error
                    })
                }
            }

            override fun onFailure(call: Call<LoginResponse>, error: Throwable) {
                if (loginCall !== call) return
                loginCall = null
                if (binding == null || call.isCanceled) return
                setLoading(false)
                showConnectionFailure(error)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("registering", registering)
        super.onSaveInstanceState(outState)
    }

    private fun updateMode() {
        binding?.apply {
            loginHeading.setText(if (registering) R.string.register_heading else R.string.login_welcome)
            loginSubtitle.setText(if (registering) R.string.register_description else R.string.login_description)
            connectionMessage.visibility = View.GONE
            nameLayout.visibility = if (registering) View.VISIBLE else View.GONE
            sellerCheck.visibility = if (registering) View.VISIBLE else View.GONE
            loginButton.setText(if (registering) R.string.register_title else R.string.login_enter)
            registerButton.setText(if (registering) R.string.register_back else R.string.register_title)
            emailLayout.error = null
            passwordLayout.error = null
        }
    }

    private fun register(email: String, password: String) {
        val ui = binding ?: return
        val name = ui.nameInput.text.toString().trim()
        if (name.isEmpty() || name.length > 255 || password.length < 8 || password.toByteArray(Charsets.UTF_8).size > 72) {
            showError(R.string.register_invalid)
            return
        }
        setLoading(true)
        val call = ApiClient.auth.register(com.example.app.network.RegisterRequest(
            name, email, password, if (ui.sellerCheck.isChecked) "vendedor" else "comprador"))
        registerCall = call
        call.enqueue(object : Callback<com.example.app.network.RegisterResponse> {
            override fun onResponse(call: Call<com.example.app.network.RegisterResponse>, response: Response<com.example.app.network.RegisterResponse>) {
                if (registerCall !== call) return
                registerCall = null
                if (binding == null) return
                setLoading(false)
                if (response.isSuccessful && response.body()?.user != null) {
                    registering = false
                    binding?.passwordInput?.text?.clear()
                    updateMode()
                    showError(R.string.register_success)
                } else {
                    response.errorBody()?.close()
                    showError(when (response.code()) {
                        409 -> R.string.register_duplicate
                        400 -> R.string.register_invalid
                        else -> R.string.login_server_error
                    })
                }
            }
            override fun onFailure(call: Call<com.example.app.network.RegisterResponse>, error: Throwable) {
                if (registerCall !== call) return
                registerCall = null
                if (binding == null || call.isCanceled) return
                setLoading(false)
                showConnectionFailure(error)
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        binding?.apply {
            if (loading) connectionMessage.visibility = View.GONE
            registerButton.isEnabled = !loading
            nameInput.isEnabled = !loading
            sellerCheck.isEnabled = !loading
            loginButton.isEnabled = !loading
            emailInput.isEnabled = !loading
            passwordInput.isEnabled = !loading
            loginProgress.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun showConnectionFailure(error: Throwable) {
        if (error !is java.io.IOException) { showError(R.string.login_server_error); return }
        val message = R.string.connection_remote_error
        binding?.connectionMessage?.apply { setText(message); visibility = View.VISIBLE }
    }

    private fun showError(message: Int) {
        binding?.let { Snackbar.make(it.root, message, Snackbar.LENGTH_LONG).show() }
    }

    override fun onDestroyView() {
        // Evita callbacks acessando uma tela destruída ou navegando após sair dela.
        val registration = registerCall
        registerCall = null
        registration?.cancel()
        val pending = loginCall
        loginCall = null
        pending?.cancel()
        binding = null
        super.onDestroyView()
    }
}
