package app.bloom.android

import app.bloom.android.core.model.ChatRoom
import app.bloom.android.core.network.UsersApi
import app.bloom.android.feature.chat.loadChatPartner
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatPartnerTest {
    @Test
    fun resolvesEitherParticipantWithoutGuessingForOutsiders() {
        val room = ChatRoom(1, "USER-A", "user-b", true)
        assertEquals("user-b", room.partnerOrNull("user-a"))
        assertEquals("USER-A", room.partnerOrNull("user-b"))
        assertNull(room.partnerOrNull("outsider"))
        assertNull(room.partnerOrNull(""))
        assertNull(room.copy(user2Id = "USER-A").partnerOrNull("user-a"))
    }

    @Test
    fun loadsPublicProfilesAndPhotosAndClearsDeniedDataOnRefresh() = runBlocking {
        MockWebServer().use { server ->
            val api =
                Retrofit.Builder()
                    .baseUrl(server.url("/api/v1/"))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(UsersApi::class.java)
            val photo = "8cc89777-f073-419b-8d05-d99c2576297a"
            server.enqueue(MockResponse().setBody("""{"id":"a","nickname":"Anna","photos":["$photo"]}"""))
            server.enqueue(MockResponse().setBody("""{"id":"b","nickname":"Bob","photos":[]}"""))
            assertEquals(photo, loadChatPartner(api, "a").photoId)
            assertEquals("Bob", loadChatPartner(api, "b").name)
            assertEquals("/api/v1/users/a", server.takeRequest().path)
            assertEquals("/api/v1/users/b", server.takeRequest().path)
            for (code in listOf(403, 404, 410)) {
                server.enqueue(MockResponse().setResponseCode(code))
                val denied = loadChatPartner(api, "a")
                assertTrue(denied.unavailable)
                assertNull(denied.profile)
                assertNull(denied.photoId)
            }
        }
    }

    @Test
    fun failuresCanBeRetriedAndMismatchedProfilesAreRejected() = runBlocking {
        MockWebServer().use { server ->
            val api =
                Retrofit.Builder()
                    .baseUrl(server.url("/api/v1/"))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(UsersApi::class.java)
            server.enqueue(MockResponse().setResponseCode(503))
            val failed = loadChatPartner(api, "a")
            assertNull(failed.profile)
            assertFalse(failed.unavailable)
            server.enqueue(MockResponse().setBody("""{"id":"a","nickname":"Updated","photos":[]}"""))
            assertEquals("Updated", loadChatPartner(api, "a").name)
            server.enqueue(MockResponse().setBody("""{"id":"other","nickname":"Wrong person"}"""))
            assertNull(loadChatPartner(api, "a").profile)
        }
    }
}
