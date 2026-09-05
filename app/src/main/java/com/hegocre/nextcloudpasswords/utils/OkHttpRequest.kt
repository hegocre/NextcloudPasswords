package com.hegocre.nextcloudpasswords.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.security.KeyChain
import androidx.core.content.ContextCompat
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.Interceptor
import java.net.InetAddress

/**
 * Class to manage the [OkHttpRequest] requests, and make them using always the same client, as suggested
 * [here](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared).
 *
 */
class OkHttpRequest private constructor() {
    var allowInsecureRequests = false
    private val initLock = ReentrantLock()
    private val initCondition = initLock.newCondition()
    @Volatile private var initializing = false

    private class LocalNetworkDns(private val context: Context) : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val ips = Dns.SYSTEM.lookup(hostname)

            if (Build.VERSION.SDK_INT >= 37) {
                val isLocal = ips.any { it.isDeviceLocalAddress() || it.isLinkLocalAddress }
                if (isLocal && ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_LOCAL_NETWORK") != PackageManager.PERMISSION_GRANTED) {
                    throw LocalNetworkAccessPermissionRequiredException()
                }
            }

            return ips
        }
    }

    private var secureClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private var insecureClient: OkHttpClient

    val client: OkHttpClient
        get() {
             if (allowInsecureRequests) return insecureClient

             if (initializing) {
                 initLock.withLock {
                     while (initializing) {
                         try { initCondition.await() } catch (_: InterruptedException) {}
                     }
                 }
             }
             return secureClient
        }

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

    fun initClient(context: Context) {
        val localIpInterceptor = Interceptor { chain ->
            val request = chain.request()
            if (Build.VERSION.SDK_INT < 37) {
                return@Interceptor chain.proceed(request)
            }

            val host = request.url.host
            Log.d("IP", host)

            val isIpAddress = android.net.InetAddresses.isNumericAddress(host)

            if (isIpAddress) {
                val ip = InetAddress.getByName(host)
                val isLocal = ip.isDeviceLocalAddress() || ip.isLinkLocalAddress
                Log.d("IP", "IsLocal: $isLocal")
                if (isLocal && ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_LOCAL_NETWORK") != PackageManager.PERMISSION_GRANTED) {
                    throw LocalNetworkAccessPermissionRequiredException()
                }
            }
            chain.proceed(request)
        }

        insecureClient = insecureClient.newBuilder()
            .addInterceptor(localIpInterceptor)
            .dns(LocalNetworkDns(context))
            .build()

        var newSecureClient = secureClient.newBuilder()
            .addInterceptor(localIpInterceptor)
            .dns(LocalNetworkDns(context))

        val alias = PreferencesManager.getInstance(context).getClientCertAlias()

        if (alias == null) {
            initLock.withLock {
                secureClient = newSecureClient.build()
                initializing = false
                initCondition.signalAll()
            }
            return
        }

        initializing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val privateKey = KeyChain.getPrivateKey(context, alias)
                val chain = KeyChain.getCertificateChain(context, alias)

                if (privateKey != null && chain != null) {
                    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    keyStore.load(null, null)
                    keyStore.setKeyEntry(alias, privateKey, null, chain)

                    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                    kmf.init(keyStore, null)

                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(kmf.keyManagers, null, null)

                    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    tmf.init(null as KeyStore?)
                    val trustManagers = tmf.trustManagers
                    val x509TrustManager = trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager

                    if (x509TrustManager != null) {
                        newSecureClient = newSecureClient.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
                    }
                }
            } catch (e: Exception) {
                Log.e("OkHttpRequest", "Failed to initialize SSL context with client certificate alias: $alias", e)
                PreferencesManager.getInstance(context).setClientCertAlias(null)
            } finally {
                initLock.withLock {
                    secureClient = newSecureClient.build()
                    initializing = false
                    initCondition.signalAll()
                }
            }
        }
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

        class LocalNetworkAccessPermissionRequiredException : Exception()

        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

        fun getInstance(): OkHttpRequest {
            synchronized(this) {
                if (instance == null) instance = OkHttpRequest()

                return instance as OkHttpRequest
            }
        }
    }
}