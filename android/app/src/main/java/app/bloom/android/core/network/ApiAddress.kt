package app.bloom.android.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object ApiAddress {
    fun parse(value: String, allowHttp: Boolean): HttpUrl {
        val url = value.trim().trimEnd('/').plus('/').toHttpUrl()
        require(allowHttp || url.isHttps) { "Для этой сборки требуется HTTPS." }
        require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null)
        require(url.encodedPath == "/api/v1/") { "Адрес должен заканчиваться на /api/v1/" }
        return url
    }
}
