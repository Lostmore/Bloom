package app.bloom.android.core.network

import app.bloom.android.core.model.*
import retrofit2.http.*

interface ChatApi {
    @GET("conversations") suspend fun rooms(): List<ChatRoom>?

    @POST("conversations") suspend fun create(@Body body: Map<String, String>): ChatRoom
}
