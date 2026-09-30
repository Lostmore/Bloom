package app.bloom.android.core.model

data class MediaCapabilities(
    val privateChatAttachments: Boolean = false,
    val maxUploadBytes: Long = 10_485_760,
    val maxAttachments: Int = 6,
)

data class UploadedChatPhoto(val mediaId: String, val mimeType: String)
