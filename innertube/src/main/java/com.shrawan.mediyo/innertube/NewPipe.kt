package com.shrawan.mediyo.innertube

import com.shrawan.mediyo.innertube.models.YouTubeClient
import com.shrawan.mediyo.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.Proxy

private object NewPipeDownloader : Downloader() {
    private val client = OkHttpClient()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.let { okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/octet-stream"), it) })
            .url(url)
            .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB_PUBLIC)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), response.body?.string())
    }
}

object NewPipeUtils {
    init {
        NewPipe.init(NewPipeDownloader)
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> = runCatching {
        val url = format.url ?: format.signatureCipher?.let { signatureCipher ->
            val params = parseQueryString(signatureCipher)
            val obfuscatedSignature = params["s"] ?: throw ParsingException("Could not parse cipher signature")
            val signatureParam = params["sp"] ?: throw ParsingException("Could not parse cipher signature parameter")
            val urlBuilder = params["url"]?.let { URLBuilder(it) } ?: throw ParsingException("Could not parse cipher url")
            urlBuilder.parameters[signatureParam] = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSignature)
            urlBuilder.toString()
        } ?: throw ParsingException("Could not find format url")

        YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
    }
}

object NewPipeExtractor {
    fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        return try {
            val streamInfo = StreamInfo.getInfo(NewPipe.getService(0), "https://www.youtube.com/watch?v=$videoId")
            val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
            streamsList.mapNotNull { (it.itagItem?.id ?: return@mapNotNull null) to it.content }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): String? = try {
        val url = format.url ?: format.signatureCipher?.let { signatureCipher ->
            val params = parseQueryString(signatureCipher)
            val obfuscatedSignature = params["s"] ?: throw ParsingException("Could not parse cipher signature")
            val signatureParam = params["sp"] ?: throw ParsingException("Could not parse cipher signature parameter")
            val urlBuilder = params["url"]?.let { URLBuilder(it) } ?: throw ParsingException("Could not parse cipher url")
            urlBuilder.parameters[signatureParam] = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSignature)
            urlBuilder.toString()
        } ?: throw ParsingException("Could not find format url")

        YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
    } catch (e: Exception) {
        null
    }

    fun getThrottlingDeobfuscatedUrl(videoId: String, url: String): String? = try {
        YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
    } catch (e: Exception) {
        null
    }
}