package app.bloom.android.core.network

import app.bloom.android.core.model.*
import retrofit2.http.*

interface IdentityApi {
    @POST("auth/login") suspend fun login(@Body request: Credentials): Tokens

    @POST("auth/register") suspend fun register(@Body request: Credentials): Tokens

    @POST("auth/logout") suspend fun logout(@Body request: RefreshRequest)

    @POST("auth/logout-all") suspend fun logoutAll()
}
