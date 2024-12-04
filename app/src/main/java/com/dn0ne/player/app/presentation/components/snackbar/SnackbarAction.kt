package com.dn0ne.player.app.presentation.components.snackbar

data class SnackbarAction(
    val name: String,
    val action: () -> Unit
)