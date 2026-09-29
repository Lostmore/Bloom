package app.bloom.android.core.model

data class Attachment(
    val id: Long,
    val url: String,
    @com.google.gson.annotations.SerializedName("media_type") val mediaType: String,
)
