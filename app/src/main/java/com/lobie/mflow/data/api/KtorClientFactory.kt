package com.lobie.mflow.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.conscrypt.Conscrypt
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object KtorClientFactory {

    fun create(): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                preconfigured = createOkHttpClient()
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                    prettyPrint = false
                })
            }
        }
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)

        // Custom Conscrypt SSL Socket Factory & TrustManager for Android 7 TLS 1.3
        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            val x509TrustManager = trustManagers.first { it is X509TrustManager } as X509TrustManager

            val sslContext = SSLContext.getInstance("TLSv1.3", Conscrypt.newProvider())
            sslContext.init(null, arrayOf(x509TrustManager), null)

            builder.sslSocketFactory(sslContext.socketFactory, x509TrustManager)
        } catch (e: Exception) {
            // Fallback to default OkHttp SSL
            e.printStackTrace()
        }

        return builder.build()
    }
}
