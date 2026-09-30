package app.bloom.android.core.model

data class Attachment(
    val id: Long = 0,
    val url: String? = null,
    @com.google.gson.annotations.SerializedName("media_type") val mediaType: String,
    @com.google.gson.annotations.SerializedName("media_id") val mediaId: String? = null,
)
