package com.dn0ne.player.core.di

import com.dn0ne.player.core.data.Settings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single<Settings> {
        Settings(context = androidContext())
    }
}