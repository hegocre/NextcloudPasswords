package com.hegocre.nextcloudpasswords.utils

import android.annotation.SuppressLint
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Class to manage the [OkHttpRequest] requests, and make them using always the same client, as suggested
 * [here](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared).
 *
 */
class OkHttpRequest private constructor() {
    var allowInsecureRequests = false

    private val secureClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val insecureClient: OkHttpClient

    val client: OkHttpClient
        get() = if (allowInsecureRequests) insecureClient else secureClient

    init {
        val insecureTrustManager = @SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {
            }

            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(insecureTrustManager), java.security.SecureRandom())
        insecureClient = OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, insecureTrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }


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

    @Throws(
        MalformedURLException::class,
        IllegalArgumentException::class,
        IOException::class,
        IllegalStateException::class
    )
    fun patch(
        sUrl: String, sessionCode: String? = null,
        body: String, mediaType: MediaType?,
        username: String? = null, password: String? = null
    ): Response {
        val formBody = body.toRequestBody(mediaType)

        val url = URL(sUrl)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("OCS-APIRequest", "true")
            .patch(formBody)

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
    fun delete(
        sUrl: String, sessionCode: String? = null,
        body: String, mediaType: MediaType?,
        username: String? = null, password: String? = null
    ): Response {
        val formBody = body.toRequestBody(mediaType)

        val url = URL(sUrl)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("OCS-APIRequest", "true")
            .delete(formBody)

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
            synchronized(this) {
                if (instance == null) instance = OkHttpRequest()

                return instance as OkHttpRequest
            }
        }
    }
}