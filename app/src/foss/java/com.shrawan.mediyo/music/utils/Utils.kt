package com.shrawan.mediyo.music.utils

import com.shrawan.mediyo.music.MainActivity
import java.lang.Exception
import timber.log.Timber

fun reportException(throwable: Throwable) {
    Timber.e(throwable)
    throwable.printStackTrace()
}
