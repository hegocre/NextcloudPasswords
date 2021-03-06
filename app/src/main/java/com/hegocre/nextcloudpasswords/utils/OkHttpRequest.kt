package com.hegocre.nextcloudpasswords.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Class to manage the [OkHttpRequest] requests, and make them using always the same client, as suggested
 * [here](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared).
 *
 */
class OkHttpRequest private constructor() {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Throws(
        MalformedURLException::class,
        IllegalArgumentException::class,
        IOException::class,
        IllegalStateException::class
    )
    fun get(
        sUrl: String, sessionCode: String? = null,
        username: String? = null, password: String? = null
    ): Response {
        val url = URL(sUrl)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("OCS-APIRequest", "true")

        if (username != null && password != null) {
            requestBuilder.addHeader("Authorization", Credentials.basic(username, password))
        }

        if (sessionCode != null) {
            requestBuilder.addHeader("x-api-session", sessionCode)
        }

        val request = requestBuilder.build()

        return client.newCall(request).execute()
    }

    @Throws(
        MalformedURLException::class,
        IllegalArgumentException::class,
        IOException::class,
        IllegalStateException::class
    )
    fun post(
        sUrl: String, sessionCode: String? = null,
        body: String, mediaType: MediaType?,
        username: String? = null, password: String? = null
    ): Response {
        val formBody = body.toRequestBody(mediaType)

        val url = URL(sUrl)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("OCS-APIRequest", "true")
            .post(formBody)

        if (username != null && password != null) {
            requestBuilder.addHeader("Authorization", Credentials.basic(username, password))
        }

        if (sessionCode != null) {
            requestBuilder.addHeader("x-api-session", sessionCode)
        }

        val request = requestBuilder.build()

        return client.newCall(request).execute()
    }

    companion object {
        private var instance: OkHttpRequest? = null

        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

        fun getInstance(): OkHttpRequest {
            if (instance == null) instance = OkHttpRequest()
            return instance as OkHttpRequest
        }
    }
}