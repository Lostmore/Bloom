package app.bloom.android

import app.bloom.android.core.security.Session
import app.bloom.android.core.security.SessionManager
import app.bloom.android.core.security.SessionStore
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionManagerTest {
    private lateinit var server: MockWebServer
    private lateinit var store: MemoryStore
    private lateinit var manager: SessionManager

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        store = MemoryStore(Session("old-access", "old-refresh", 0))
        manager = SessionManager(store, OkHttpClient(), server.url("/api/v1/"))
    }

    @After fun tearDown() = server.shutdown()

    @Test
    fun concurrentUnauthorizedRequestsRotateRefreshOnlyOnce() {
        server.enqueue(tokens())
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results = executor.invokeAll(List(4) { Callable { manager.freshToken("old-access") } })
            results.forEach { assertEquals("new-access", it.get()) }
            assertEquals(1, server.requestCount)
            val request = server.takeRequest()
            assertEquals("/api/v1/auth/refresh", request.path)
            assertEquals("{\"refreshToken\":\"old-refresh\"}", request.body.readUtf8())
            assertNull(request.getHeader("X-Internal-Token"))
            assertEquals("new-refresh", store.value?.refreshToken)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun rejectedRefreshClearsStoredCredentials() {
        server.enqueue(MockResponse().setResponseCode(401))
        assertNull(manager.freshToken())
        assertNull(store.value)
        assertNull(manager.session.value)
    }

    @Test
    fun temporaryFailurePreservesSessionAndDoesNotRetryRotatingToken() {
        server.enqueue(MockResponse().setResponseCode(503))
        assertThrows(IOException::class.java) { manager.freshToken() }
        assertEquals("old-refresh", store.value?.refreshToken)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun malformedRefreshResponseCannotReplaceCredentials() {
        server.enqueue(MockResponse().setBody("{\"expiresIn\":900}"))
        assertThrows(IOException::class.java) { manager.freshToken() }
        assertEquals("old-access", store.value?.accessToken)
    }

    @Test
    fun authenticatorRetriesOriginalApiWithNewBearer() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(tokens())
        server.enqueue(MockResponse().setBody("{}"))
        val client = OkHttpClient.Builder().authenticator(manager).build()
        client
            .newCall(
                Request.Builder()
                    .url(server.url("/api/v1/users/me"))
                    .header("Authorization", "Bearer old-access")
                    .build()
            )
            .execute()
            .use {
                assertEquals(200, it.code)
            }
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("/api/v1/auth/refresh", server.takeRequest().path)
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
    }

    private fun tokens() =
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""{"accessToken":"new-access","refreshToken":"new-refresh","expiresIn":900}""")

    private class MemoryStore(var value: Session?) : SessionStore {
        override fun load() = value

        override fun save(session: Session?) {
            value = session
        }
    }
}
