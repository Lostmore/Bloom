package app.bloom.android.core.network

import app.bloom.android.core.model.*
import retrofit2.http.*

interface UsersApi {
    @GET("users/feed") suspend fun feed(@Query("cursor") cursor: String? = null): FeedPage

    @GET("users/me") suspend fun me(): Profile

    @POST("users") suspend fun create(@Body request: CreateProfile): Profile

    @PATCH("users/me") suspend fun update(@Body request: ProfilePatch): Profile

    @GET("users/{id}") suspend fun profile(@Path("id") id: String): Profile

    @GET("interests") suspend fun interests(): List<Interest>

    @PUT("users/me/interests")
    suspend fun interests(@Body body: Map<String, @JvmSuppressWildcards List<String>>): List<String>

    @POST("users/{id}/block") suspend fun block(@Path("id") id: String)

    @POST("users/me/heartbeat") suspend fun heartbeat()

    @DELETE("users/me") suspend fun delete()

    @GET("users/me/privacy") suspend fun privacy(): Privacy

    @PUT("users/me/privacy") suspend fun privacy(@Body body: Map<String, Privacy>): Privacy
}
