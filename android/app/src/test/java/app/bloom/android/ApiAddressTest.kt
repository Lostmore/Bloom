package app.bloom.android

import app.bloom.android.core.network.ApiAddress
import org.junit.Assert.*
import org.junit.Test

class ApiAddressTest {
    @Test
    fun debugAllowsLanGatewayAndNormalizesTrailingSlash() {
        assertEquals(
            "http://192.168.1.5:8080/api/v1/",
            ApiAddress.parse("http://192.168.1.5:8080/api/v1", true).toString(),
        )
    }

    @Test
    fun releaseRejectsPlaintextAndEmbeddedCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiAddress.parse("http://example.com/api/v1/", false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ApiAddress.parse("https://secret:password@example.com/api/v1/", false)
        }
        assertEquals(
            "https://example.com/api/v1/",
            ApiAddress.parse("https://example.com/api/v1/", false).toString(),
        )
    }

    @Test
    fun rejectsWrongPrefixAndQuerySoRelativeCallsStayOnGateway() {
        assertThrows(IllegalArgumentException::class.java) {
            ApiAddress.parse("https://example.com/", false)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ApiAddress.parse("https://example.com/api/v1/?token=secret", false)
        }
    }
}
