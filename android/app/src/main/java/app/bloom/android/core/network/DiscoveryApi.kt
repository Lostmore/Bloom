package app.bloom.android.core.network

import app.bloom.android.core.model.*
import retrofit2.http.*

interface DiscoveryApi {
    @GET("discovery/feed") suspend fun feed(@Query("cursor") cursor: String? = null): FeedPage
}
