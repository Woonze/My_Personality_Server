package com.example.mypersonality.server

import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.Test

class ApplicationTest {

    @Test
    fun `health endpoint returns ok`() = testApplication {
        application { module() }

        val response = client.get("/")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).contains("mypersonality-server")
    }

    @Test
    fun `register and login endpoints create session`() = testApplication {
        application { module() }

        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "demo@example.com",
                  "password": "secret123",
                  "fullName": "Иван Петров",
                  "role": "SEEKER"
                }
                """.trimIndent()
            )
        }

        assertThat(registerResponse.status).isEqualTo(HttpStatusCode.Created)
        assertThat(registerResponse.bodyAsText()).contains("demo@example.com")

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "demo@example.com",
                  "password": "secret123"
                }
                """.trimIndent()
            )
        }

        assertThat(loginResponse.status).isEqualTo(HttpStatusCode.OK)
        assertThat(loginResponse.bodyAsText()).contains("Иван Петров")
    }

    @Test
    fun `vacancy endpoint creates vacancy`() = testApplication {
        application { module() }

        val response = client.post("/vacancies?employerId=employer-demo") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "companyName": "Тестовая компания",
                  "title": "Backend Kotlin Developer",
                  "city": "Екатеринбург",
                  "salary": "от 150 000 ₽",
                  "employmentType": "Удаленно",
                  "description": "Разработка API на Ktor"
                }
                """.trimIndent()
            )
        }

        assertThat(response.status).isEqualTo(HttpStatusCode.Created)
        assertThat(response.bodyAsText()).contains("Backend Kotlin Developer")
    }

    @Test
    fun `deleting vacancy removes applications`() = testApplication {
        application { module() }

        val createVacancyResponse = client.post("/vacancies?employerId=employer-demo") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "companyName": "Каскад Тест",
                  "title": "Delete Me",
                  "city": "Москва",
                  "salary": "100000",
                  "employmentType": "Удаленно",
                  "description": "Проверка каскадного удаления"
                }
                """.trimIndent()
            )
        }
        assertThat(createVacancyResponse.status).isEqualTo(HttpStatusCode.Created)
        val vacancyId = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(createVacancyResponse.bodyAsText())!!.groupValues[1]

        val authResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "email": "cascade@example.com",
                  "password": "secret123",
                  "fullName": "Тестовый Соискатель",
                  "role": "SEEKER"
                }
                """.trimIndent()
            )
        }
        assertThat(authResponse.status).isEqualTo(HttpStatusCode.Created)
        val seekerId = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(authResponse.bodyAsText())!!.groupValues[1]

        val applyResponse = client.post("/vacancies/$vacancyId/apply/$seekerId") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "vacancyId": "$vacancyId",
                  "coverLetter": "Готов к интервью"
                }
                """.trimIndent()
            )
        }
        assertThat(applyResponse.status).isEqualTo(HttpStatusCode.Created)

        val deleteResponse = client.delete("/vacancies/$vacancyId?employerId=employer-demo")
        assertThat(deleteResponse.status).isEqualTo(HttpStatusCode.NoContent)

        val vacancyResponse = client.get("/vacancies/$vacancyId")
        assertThat(vacancyResponse.status).isEqualTo(HttpStatusCode.NotFound)

        val applicationsResponse = client.get("/vacancies/$vacancyId/applications")
        assertThat(applicationsResponse.status).isEqualTo(HttpStatusCode.OK)
        assertThat(applicationsResponse.bodyAsText()).isEqualTo("[]")
    }
}
