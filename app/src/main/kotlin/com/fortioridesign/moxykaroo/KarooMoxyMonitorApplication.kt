package com.fortioridesign.moxykaroo

import android.app.Application
import com.fortioridesign.moxykaroo.ble.BleManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::KarooSystemServiceProvider) { createdAtStart() }
    singleOf(::BleManager) { createdAtStart() }
}

class KarooMoxyMonitorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@KarooMoxyMonitorApplication)
            modules(appModule)
        }
    }
}