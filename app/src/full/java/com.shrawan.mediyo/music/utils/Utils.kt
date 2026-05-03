package com.shrawan.mediyo.music.utils

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

fun reportException(throwable: Throwable) {
    Firebase.crashlytics.recordException(throwable)
    Timber.e(throwable)
    throwable.printStackTrace()
}
