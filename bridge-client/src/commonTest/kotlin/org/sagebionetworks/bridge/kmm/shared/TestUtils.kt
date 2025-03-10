package org.sagebionetworks.bridge.kmm.shared

import com.squareup.sqldelight.db.SqlDriver
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.http.*

internal expect fun testDatabaseDriver() : SqlDriver

fun getTestClient(json: String): HttpClient {
    val mockEngine = MockEngine.config {
        addHandler (
            getJsonReponseHandler(json)
        )
    }
    return getTestClient(mockEngine)
}

fun getTestClient(mockEngine: HttpClientEngineFactory<MockEngineConfig>) : HttpClient {
    return HttpClient(mockEngine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
    }
}

fun getJsonReponseHandler(json: String) : suspend MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData {
    return {request ->
        respond(json, headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())))
    }
}