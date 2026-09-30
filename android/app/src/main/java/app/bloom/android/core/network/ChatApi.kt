package app.bloom.android.core.network

import app.bloom.android.core.model.*
import retrofit2.http.*

interface ChatApi {
    @GET("conversations") suspend fun rooms(): List<ChatRoom>?

    @POST("conversations") suspend fun create(@Body body: Map<String, String>): ChatRoom

    @GET("conversations/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30,
    ): MessagePage

    @GET("conversations/{id}/messages")
    suspend fun context(
        @Path("id") roomId: Long,
        @Query("around") messageId: Long,
        @Query("limit") limit: Int = 50,
    ): MessagePage

    @GET("conversations/capabilities") suspend fun capabilities(): ChatCapabilities
}
