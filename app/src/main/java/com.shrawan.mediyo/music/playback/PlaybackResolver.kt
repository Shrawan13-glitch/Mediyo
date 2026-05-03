package com.shrawan.mediyo.music.playback

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.shrawan.mediyo.innertube.NewPipeExtractor
import com.shrawan.mediyo.innertube.NewPipeUtils
import com.shrawan.mediyo.innertube.YouTube
import com.shrawan.mediyo.innertube.models.YouTubeClient
import com.shrawan.mediyo.innertube.models.response.PlayerResponse
import com.shrawan.mediyo.music.constants.AudioQuality
import com.shrawan.mediyo.music.utils.reportException
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object PlaybackResolver {
    private const val TAG = "PlaybackResolver"
    private const val UPLOADED_TRACKS_PLAYLIST_PREFIX = "MLPT"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private val MAIN_CLIENT = YouTubeClient.WEB_REMIX
    private val STREAM_FALLBACK_CLIENTS = arrayOf(
        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        YouTubeClient.TVHTML5,
        YouTubeClient.ANDROID_VR_1_43_32,
        YouTubeClient.ANDROID_VR_1_61_48,
        YouTubeClient.ANDROID_CREATOR,
        YouTubeClient.IPADOS,
        YouTubeClient.ANDROID_VR_NO_AUTH,
        YouTubeClient.MOBILE,
        YouTubeClient.IOS,
        YouTubeClient.WEB,
        YouTubeClient.WEB_CREATOR,
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        val playerResponse: PlayerResponse,
        val isPrivatelyOwned: Boolean = false,
    )

    suspend fun resolve(
        videoId: String,
        playlistId: String? = null,
        isUploadedHint: Boolean = false,
        preferredItag: Int? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(TAG).d("Resolving playback for videoId=$videoId playlistId=$playlistId preferredItag=$preferredItag")

        val isUploadedTrack = isUploadedHint || playlistId?.startsWith(UPLOADED_TRACKS_PLAYLIST_PREFIX) == true
        val isLoggedIn = YouTube.cookie != null
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)

        var mainPlayerResponse = YouTube.player(
            videoId = videoId,
            playlistId = playlistId,
            client = MAIN_CLIENT,
            signatureTimestamp = signatureTimestamp,
        ).getOrThrow()

        val mainStatus = mainPlayerResponse.playabilityStatus.status
        val isAgeRestrictedFromResponse = mainStatus in listOf("AGE_CHECK_REQUIRED", "AGE_VERIFICATION_REQUIRED", "CONTENT_CHECK_REQUIRED")
        val isLoginRequired = mainStatus == "LOGIN_REQUIRED"
        val wasOriginallyAgeRestricted = isAgeRestrictedFromResponse || (isLoginRequired && !isUploadedTrack)

        if ((isAgeRestrictedFromResponse || isLoginRequired) && isLoggedIn) {
            val creatorResponse = YouTube.player(
                videoId = videoId,
                playlistId = playlistId,
                client = YouTubeClient.WEB_CREATOR,
                signatureTimestamp = signatureTimestamp,
            ).getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                mainPlayerResponse = creatorResponse
            }
        }

        val isPrivateTrack = isUploadedTrack ||
            mainPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

        val startIndex = when {
            isPrivateTrack && mainPlayerResponse.playabilityStatus.status == "OK" -> -1
            isPrivateTrack -> 1
            wasOriginallyAgeRestricted -> 0
            else -> -1
        }

        var selectedFormat: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        var privateCandidateStreamUrl: String? = null
        var privateCandidateFormat: PlayerResponse.StreamingData.Format? = null
        var privateCandidateExpiry: Int? = null
        var privateCandidateResponse: PlayerResponse? = null
        var inferredAsPrivate = false

        for (clientIndex in startIndex until STREAM_FALLBACK_CLIENTS.size) {
            selectedFormat = null
            streamUrl = null
            streamExpiresInSeconds = null

            val client =
                if (clientIndex == -1) {
                    MAIN_CLIENT
                } else {
                    STREAM_FALLBACK_CLIENTS[clientIndex]
                }

            if (client.loginRequired && !isLoggedIn) {
                continue
            }

            streamPlayerResponse =
                if (clientIndex == -1) {
                    mainPlayerResponse
                } else {
                    YouTube.player(
                        videoId = videoId,
                        playlistId = playlistId,
                        client = client,
                        signatureTimestamp = if (client.useSignatureTimestamp) signatureTimestamp else null,
                    ).getOrNull()
                }

            if (streamPlayerResponse?.playabilityStatus?.status != "OK") {
                continue
            }

            val skipNewPipe = wasOriginallyAgeRestricted || isPrivateTrack
            val responseToUse = if (skipNewPipe) {
                streamPlayerResponse
            } else {
                streamPlayerResponse?.let { maybeApplyNewPipeStreams(videoId, it) } ?: streamPlayerResponse
            }

            selectedFormat = findFormat(
                responseToUse ?: continue,
                audioQuality,
                connectivityManager,
                preferredItag,
            ) ?: continue

            streamUrl = resolveStreamUrl(
                format = selectedFormat,
                videoId = videoId,
                allowStreamInfoFallback = !skipNewPipe,
            ) ?: continue

            streamExpiresInSeconds = streamPlayerResponse?.streamingData?.expiresInSeconds
            if (streamExpiresInSeconds == null) {
                continue
            }

            val isPrivate = isPrivateTrack || isPrivatelyOwnedTrack(streamPlayerResponse)
            val isLastClient = clientIndex == STREAM_FALLBACK_CLIENTS.size - 1

            if (isLastClient) {
                break
            }

            if (isPrivate) {
                if (privateCandidateStreamUrl == null) {
                    privateCandidateStreamUrl = streamUrl
                    privateCandidateFormat = selectedFormat
                    privateCandidateExpiry = streamExpiresInSeconds
                    privateCandidateResponse = streamPlayerResponse
                }
                continue
            }

            val httpStatus = validateStatus(streamUrl, includeAuth = isLoggedIn)
            if (httpStatus != null && httpStatus in 200..299) {
                break
            } else if (httpStatus == 403 && isLoggedIn && privateCandidateStreamUrl == null) {
                inferredAsPrivate = true
                privateCandidateStreamUrl = streamUrl
                privateCandidateFormat = selectedFormat
                privateCandidateExpiry = streamExpiresInSeconds
                privateCandidateResponse = streamPlayerResponse
                continue
            }
        }

        if (streamUrl == null && privateCandidateStreamUrl != null) {
            streamUrl = privateCandidateStreamUrl
            selectedFormat = privateCandidateFormat
            streamExpiresInSeconds = privateCandidateExpiry
            streamPlayerResponse = privateCandidateResponse
        }

        val finalResponse = streamPlayerResponse ?: throw PlaybackException(
            "Bad stream player response",
            null,
            PlaybackException.ERROR_CODE_REMOTE_ERROR,
        )

        if (finalResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                finalResponse.playabilityStatus.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR,
            )
        }

        val finalFormat = selectedFormat ?: throw PlaybackException(
            "Could not find format",
            null,
            PlaybackException.ERROR_CODE_NO_STREAM,
        )

        val finalStreamUrl = streamUrl ?: throw PlaybackException(
            "Could not find stream url",
            null,
            PlaybackException.ERROR_CODE_NO_STREAM,
        )

        val finalExpires = streamExpiresInSeconds ?: throw PlaybackException(
            "Missing stream expire time",
            null,
            PlaybackException.ERROR_CODE_REMOTE_ERROR,
        )

        PlaybackData(
            audioConfig = finalResponse.playerConfig?.audioConfig,
            videoDetails = finalResponse.videoDetails,
            format = finalFormat,
            streamUrl = finalStreamUrl,
            streamExpiresInSeconds = finalExpires,
            playerResponse = finalResponse,
            isPrivatelyOwned = isPrivateTrack || inferredAsPrivate || isPrivatelyOwnedTrack(finalResponse),
        )
    }

    private fun getSignatureTimestampOrNull(videoId: String): Int? {
        val result = NewPipeUtils.getSignatureTimestamp(videoId)
        return result.getOrElse { throwable ->
            val isAgeRestricted = throwable.message?.contains("age-restricted", ignoreCase = true) == true ||
                throwable.cause?.message?.contains("age-restricted", ignoreCase = true) == true
            if (!isAgeRestricted) {
                reportException(throwable)
            }
            null
        }
    }

    private fun maybeApplyNewPipeStreams(
        videoId: String,
        playerResponse: PlayerResponse,
    ): PlayerResponse {
        val audioStreams = NewPipeExtractor.newPipePlayer(videoId)
        if (audioStreams.isEmpty()) {
            return playerResponse
        }
        val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats.orEmpty().mapNotNull { adaptiveFormat ->
            audioStreams.find { it.first == adaptiveFormat.itag }?.let {
                adaptiveFormat.copy(url = it.second)
            }
        }
        return if (adaptiveFormats.isEmpty()) {
            playerResponse
        } else {
            playerResponse.copy(
                streamingData = playerResponse.streamingData?.copy(
                    adaptiveFormats = adaptiveFormats
                )
            )
        }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        preferredItag: Int?,
    ): PlayerResponse.StreamingData.Format? {
        val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats ?: return null
        val audioCapableFormats = adaptiveFormats.filter { it.isAudio }
        if (audioCapableFormats.isEmpty()) return null

        preferredItag?.let { itag ->
            audioCapableFormats.firstOrNull { it.itag == itag }?.let { return it }
        }

        val maxBitrate = audioCapableFormats.maxOfOrNull { it.bitrate } ?: return null
        val targetBitrate =
            when (audioQuality) {
                AudioQuality.HIGH -> minOf(maxBitrate.toDouble(), 256000.0)
                AudioQuality.LOW -> minOf(maxBitrate.toDouble(), 128000.0)
                AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) {
                    minOf(maxBitrate.toDouble(), 128000.0)
                } else {
                    maxBitrate.toDouble()
                }
            }

        val cappedFormats = audioCapableFormats.filter { it.bitrate <= targetBitrate }
        return cappedFormats.filter { it.mimeType.contains("webm", ignoreCase = true) }.maxByOrNull { it.bitrate }
            ?: cappedFormats.maxByOrNull { it.bitrate }
            ?: audioCapableFormats.filter { it.mimeType.contains("webm", ignoreCase = true) }.maxByOrNull { it.bitrate }
            ?: audioCapableFormats.maxByOrNull { it.bitrate }
    }

    private fun resolveStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        allowStreamInfoFallback: Boolean,
    ): String? {
        NewPipeUtils.getStreamUrl(format, videoId).getOrNull()?.let { return it }
        if (!allowStreamInfoFallback) {
            return null
        }
        val streamUrls = NewPipeExtractor.newPipePlayer(videoId)
        if (streamUrls.isEmpty()) return null
        streamUrls.find { it.first == format.itag }?.second?.let { return it }
        return streamUrls.firstOrNull { it.second.isNotBlank() }?.second
    }

    private fun validateStatus(url: String, includeAuth: Boolean = false): Int? {
        return try {
            val requestBuilder = Request.Builder()
                .head()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB_PUBLIC)
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")

            if (includeAuth) {
                YouTube.cookie?.let { cookie ->
                    requestBuilder.header("Cookie", cookie)
                }
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                response.code
            }
        } catch (e: Exception) {
            reportException(e)
            null
        }
    }

    private fun isPrivatelyOwnedTrack(playerResponse: PlayerResponse?): Boolean =
        playerResponse?.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"
}
