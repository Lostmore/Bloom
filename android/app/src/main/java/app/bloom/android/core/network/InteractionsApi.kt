package app.bloom.android.core.network

import app.bloom.android.core.model.*
import retrofit2.http.*

interface InteractionsApi {
    @POST("interactions/{id}/{action}")
    suspend fun react(
        @Path("id") id: String,
        @Path("action") action: String,
        @Header("Idempotency-Key") key: String,
    ): InteractionResult

    @GET("matches") suspend fun matches(@Query("after") after: String? = null): MatchPage

    @GET("matches/{id}") suspend fun match(@Path("id") id: String): Match

    @DELETE("matches/{id}") suspend fun unmatch(@Path("id") id: String)
}
