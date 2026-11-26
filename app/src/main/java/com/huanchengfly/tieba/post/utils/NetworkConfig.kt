package com.huanchengfly.tieba.post.utils

import android.content.Context
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 全站网络与图片加载并发/超时/连接池统一配置中枢。
 *
 * OkHttp 默认的 `maxRequestsPerHost = 5` 会让同域名（`c.tieba.baidu.com` / `imgsa.baidu.com` /
 * `tiebapic.baidu.com` / `himg.bdimg.com`）上的多图与接口请求严重排队。
 * 这里集中维护共享 `Dispatcher` 与 `ConnectionPool`，并在设置页修改后立即实时生效（无需重启）。
 */
object NetworkConfig {
    const val PREF_KEY_MAX_REQUESTS = "max_concurrent_requests"
    const val PREF_KEY_MAX_REQUESTS_PER_HOST = "max_concurrent_requests_per_host"
    const val PREF_KEY_TIMEOUT_SECONDS = "network_timeout_seconds"

    const val DEFAULT_MAX_REQUESTS = 64
    const val DEFAULT_MAX_REQUESTS_PER_HOST = 16
    const val DEFAULT_TIMEOUT_SECONDS = 15

    /** API 请求共享连接池与调度器（Retrofit 各 API 客户端共用，避免创建多套线程池）。 */
    val apiConnectionPool = ConnectionPool(32, 5, TimeUnit.MINUTES)
    val apiDispatcher = Dispatcher().apply {
        maxRequests = DEFAULT_MAX_REQUESTS
        maxRequestsPerHost = DEFAULT_MAX_REQUESTS_PER_HOST
    }

    /** 图片加载共享连接池与调度器（Sketch 与 Glide 共用）。 */
    val imageConnectionPool = ConnectionPool(48, 5, TimeUnit.MINUTES)
    val imageDispatcher = Dispatcher().apply {
        maxRequests = DEFAULT_MAX_REQUESTS
        maxRequestsPerHost = DEFAULT_MAX_REQUESTS_PER_HOST
    }

    @Volatile
    var timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS
        private set

    /**
     * 按每次请求动态应用超时配置，使设置页更改超时后对新请求立即生效。
     */
    val dynamicTimeoutInterceptor = Interceptor { chain ->
        val timeout = timeoutSeconds.coerceIn(5, 120)
        chain
            .withConnectTimeout(timeout, TimeUnit.SECONDS)
            .withReadTimeout(timeout, TimeUnit.SECONDS)
            .withWriteTimeout(timeout, TimeUnit.SECONDS)
            .proceed(chain.request())
    }

    fun init(context: Context) {
        val prefs = context.appPreferences
        applyConfig(
            maxRequests = prefs.maxConcurrentRequests?.toIntOrNull() ?: DEFAULT_MAX_REQUESTS,
            maxRequestsPerHost = prefs.maxConcurrentRequestsPerHost?.toIntOrNull() ?: DEFAULT_MAX_REQUESTS_PER_HOST,
            timeoutSec = prefs.networkTimeoutSeconds?.toIntOrNull() ?: DEFAULT_TIMEOUT_SECONDS,
        )
    }

    /**
     * 实时更新全站网络与图片调度器并发数与超时（无需重启 App）。
     */
    @Synchronized
    fun applyConfig(
        maxRequests: Int? = null,
        maxRequestsPerHost: Int? = null,
        timeoutSec: Int? = null,
    ) {
        val safeMax = (maxRequests ?: apiDispatcher.maxRequests).coerceIn(8, 256)
        val safePerHost = (maxRequestsPerHost ?: apiDispatcher.maxRequestsPerHost).coerceIn(2, safeMax)
        apiDispatcher.maxRequests = safeMax
        apiDispatcher.maxRequestsPerHost = safePerHost
        imageDispatcher.maxRequests = safeMax
        imageDispatcher.maxRequestsPerHost = safePerHost
        if (timeoutSec != null) {
            timeoutSeconds = timeoutSec.coerceIn(5, 120)
        }
    }

    /** 构建用于 Sketch 图片加载的高并发 OkHttpClient。 */
    fun createImageOkHttpClient(userAgent: String? = null): OkHttpClient {
        return OkHttpClient.Builder()
            .dispatcher(imageDispatcher)
            .connectionPool(imageConnectionPool)
            .retryOnConnectionFailure(true)
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            .addInterceptor(dynamicTimeoutInterceptor)
            .apply {
                if (!userAgent.isNullOrBlank()) {
                    addInterceptor { chain ->
                        val req = chain.request()
                        if (req.header("User-Agent") == null) {
                            chain.proceed(req.newBuilder().header("User-Agent", userAgent).build())
                        } else {
                            chain.proceed(req)
                        }
                    }
                }
            }
            .build()
    }
}
