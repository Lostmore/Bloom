package app.bloom.android.core.model

// Proposed Discovery wire contract; the service is not implemented yet.
data class FeedPage(val items: List<Profile>, val nextCursor: String?)
