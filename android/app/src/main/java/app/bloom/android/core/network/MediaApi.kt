package app.bloom.android.core.network

import app.bloom.android.core.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface MediaApi {
    @GET("media/capabilities") suspend fun capabilities(): MediaCapabilities

    @Multipart
    @POST("media/chat-attachments")
    suspend fun uploadChatPhoto(
        @Header("Idempotency-Key") key: String,
        @Part("roomId") roomId: RequestBody,
        @Part file: MultipartBody.Part,
    ): UploadedChatPhoto

    @Streaming @GET("media/{id}") suspend fun image(@Path("id") id: String): ResponseBody
}
