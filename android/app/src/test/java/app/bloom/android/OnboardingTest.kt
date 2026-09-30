package app.bloom.android

import app.bloom.android.core.network.UsersApi
import app.bloom.android.feature.profile.OnboardingDraft
import app.bloom.android.feature.profile.saveOnboarding
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OnboardingTest {
    @Test
    fun ageAndInterestsAreRequiredBeforeContinuing() {
        assertNotNull(OnboardingDraft(step = 1, birthday = LocalDate.now().minusYears(17).toString()).error())
        assertNull(OnboardingDraft(step = 1, birthday = LocalDate.now().minusYears(18).toString()).error())
        assertNotNull(
            OnboardingDraft(step = 1, birthday = LocalDate.now().minusYears(120).minusDays(1).toString()).error()
        )
        assertNotNull(OnboardingDraft(step = 4).error())
        assertNull(OnboardingDraft(step = 4, interests = listOf("travel")).error())
    }

    @Test
    fun retryAfterInterestFailureResumesExistingProfile() = runBlocking {
        MockWebServer().use { server ->
            val api =
                Retrofit.Builder()
                    .baseUrl(server.url("/api/v1/"))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(UsersApi::class.java)
            val profile = """{"id":"person","nickname":"Anna","version":1,"interests":[],"searchModes":["FRIENDS"]}"""
            fun enqueue(code: Int, body: String = "{}") {
                server.enqueue(
                    MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)
                )
            }
            val draft =
                OnboardingDraft(
                    name = "Anna",
                    birthday = "2000-05-21",
                    gender = "FEMALE",
                    goals = listOf("FRIENDS"),
                    interests = listOf("travel"),
                    bio = "Hello",
                    city = "Moscow",
                )
            enqueue(404)
            enqueue(200, profile)
            enqueue(200, profile)
            enqueue(503)
            try {
                saveOnboarding(api, draft)
                fail("Must keep onboarding incomplete on failure")
            } catch (error: HttpException) {
                assertEquals(503, error.code())
            }
            enqueue(200, profile)
            enqueue(200, profile)
            enqueue(200, "[\"travel\"]")
            enqueue(200, profile.replace("\"interests\":[]", "\"interests\":[\"travel\"]"))
            assertEquals(listOf("travel"), saveOnboarding(api, draft).interests)
            val requests = (1..8).map { server.takeRequest() }
            assertEquals(1, requests.count { it.method == "PUT" && it.path == "/api/v1/users/me" })
            assertEquals("{\"interests\":[\"travel\"]}", requests[6].body.readUtf8())
            assertTrue(requests[5].body.readUtf8().contains("Moscow"))
        }
    }
}
