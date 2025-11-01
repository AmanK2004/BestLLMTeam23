package com.example.csci310team23

import android.app.Application

class CSCI310Team23App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.provide(this)
    }
}
