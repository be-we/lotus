package com.dn0ne.player.app.di

import com.dn0ne.player.app.data.TrackResolver
import com.dn0ne.player.app.data.SavedPlayerState
import com.dn0ne.player.app.presentation.PlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val playerModule = module {

    single<TrackResolver> {
        TrackResolver(
            context = androidContext()
        )
    }

    single<SavedPlayerState> {
        SavedPlayerState(
            context = androidContext()
        )
    }

    viewModel<PlayerViewModel> {
        PlayerViewModel(
            savedPlayerState = get(),
            trackResolver = get()
        )
    }
}