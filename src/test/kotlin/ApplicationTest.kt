package LoreBible

import LoreBible.com.lorebible.configureMonitoring
import LoreBible.com.lorebible.configureSerialization
import com.lorebible.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import org.junit.BeforeClass
import plugins.DatabaseFactory
import plugins.configureSecurity
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.*
import kotlin.test.assertTrue

class ApplicationTest {

    // Runs once before all tests — sets up the in-memory DB
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupDatabase() {
            DatabaseFactory.init(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                user = "sa",
                password = "",
                driver = "org.h2.Driver"
            )
        }
    }

    // Shared config so you don't repeat it in every test
    //The app is setup here.
    private fun ApplicationTestBuilder.setupApp() {
        environment {
            config = MapApplicationConfig(
                "jwt.secret" to "my-super-secret-lore-key",
                "jwt.issuer" to "http://0.0.0.0:8080/",
                "jwt.audience" to "lore-users",
                "jwt.expiration" to "21600000"
            )
        }
        application {
            // Call everything EXCEPT DatabaseFactory.init() — already done above
            configureSerialization()
            configureMonitoring()
            configureSecurity()
            configureRouting()
        }
    }

    @Test
    fun `unauthenticated request to characters returns 401`() = testApplication {
        setupApp()

        client.get("/characters").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun `register then login returns a token`() = testApplication {
        setupApp()

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        // Register
        val registerResponse = jsonClient.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"testuser","password":"testpass"}""")
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        // Login
        val loginResponse = jsonClient.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"testuser","password":"testpass"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        // Check the token is actually in the response
        val body = loginResponse.bodyAsText()
        assertTrue(body.contains("token"))
    }

    @Test
    fun `authenticated request to characters returns 200`() = testApplication {
        setupApp()

        val jsonClient = createClient {
            install(ContentNegotiation) { json() }
        }

        // Register + login to get a real token
        jsonClient.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"authuser","password":"authpass"}""")
        }
        val loginResponse = jsonClient.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"authuser","password":"authpass"}""")
        }

        // Pull the token out of the response
        val token = Json.parseToJsonElement(loginResponse.bodyAsText())
            .jsonObject["data"]
            ?.jsonObject?.get("token")
            ?.jsonPrimitive?.content!!
        //jsonPrimitive = "This is a plain value, not another object."

        // Hit the protected endpoint with the token
        val response = jsonClient.get("/characters") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}