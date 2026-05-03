package com.shrawan.mediyo.music.utils

import com.shrawan.mediyo.music.db.entities.LyricsEntity

object TranslationHelper {
    suspend fun translate(lyrics: LyricsEntity): LyricsEntity = lyrics
    suspend fun clearModels() {}
}