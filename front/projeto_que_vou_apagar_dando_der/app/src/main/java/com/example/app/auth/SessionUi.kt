package com.example.app.auth

import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.app.R

fun Fragment.openLogin() {
    findNavController().navigate(R.id.nav_login, null,
        NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build())
}
