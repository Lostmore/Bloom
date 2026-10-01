package app.bloom.android.core.model

// Basic Users feed; compatible with the planned Discovery response.
data class FeedPage(val items: List<Profile>, val nextCursor: String?)
