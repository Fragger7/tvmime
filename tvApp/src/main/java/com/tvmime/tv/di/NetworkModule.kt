package com.tvmime.tv.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CHROME_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    @Provides
    @Singleton
    fun provideEvasionInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
            
            // 1. Spoof User-Agent if not explicitly set
            if (originalRequest.header("User-Agent") == null) {
                requestBuilder.header("User-Agent", CHROME_USER_AGENT)
            }

            var response: Response? = null
            var attempt = 0
            var success = false

            // 2. Retry Loop & Enhanced Headers (Origin/Referer) on 403
            while (!success && attempt < 3) {
                try {
                    response?.close()
                    val requestToFire = requestBuilder.build()
                    response = chain.proceed(requestToFire)
                    
                    if (response.code == 403 || response.code == 401) {
                        // Inject origin headers dynamically to bypass deep-link blocks
                        val origin = "${originalRequest.url.scheme}://${originalRequest.url.host}"
                        requestBuilder.header("Origin", origin)
                        requestBuilder.header("Referer", "$origin/")
                    } else {
                        success = true
                    }
                } catch (e: Exception) {
                    if (attempt >= 2) throw e
                }
                attempt++
                if (!success) {
                    Thread.sleep(500L * attempt)
                }
            }
            response ?: chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(evasionInterceptor: Interceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(evasionInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            // Note: Custom DoH (DNS over HTTPS) can be injected here for ISP blocking
            .build()
    }
}
