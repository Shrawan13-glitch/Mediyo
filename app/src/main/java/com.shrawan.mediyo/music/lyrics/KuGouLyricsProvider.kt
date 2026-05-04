package com.shrawan.mediyo.music.lyrics

import android.content.Context
import com.shrawan.mediyo.kugou.KuGou
import com.shrawan.mediyo.music.constants.EnableKugouKey
import com.shrawan.mediyo.music.utils.dataStore
import com.shrawan.mediyo.music.utils.get

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(id: String, title: String, artist: String, duration: Int): Result<String> =
        KuGou.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(id: String, title: String, artist: String, duration: Int, callback: (String) -> Unit) {
        KuGou.getAllLyrics(title, artist, duration, callback)
    }
}
