package com.lorebible

import LoreBible.com.lorebible.configureMonitoring
import LoreBible.com.lorebible.configureSerialization
import exception.BadRequestException
import exception.ForbiddenException
import exception.NotFoundException
import exception.UnauthorizedException
import io.ktor.server.application.*
import io.ktor.server.cio.EngineMain
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import model.character.Character
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import model.dtos.FeatRequest
import model.dtos.RelationAdd
import model.responsewrapper.ApiResponse
import plugins.DatabaseFactory
import plugins.configureSecurity

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init() //Da Tables creator
    configureSerialization() //Da Translator
    configureMonitoring() //Da Logging team
    configureSecurity() //Da Bouncer


        install(RequestValidation) {

            validate<Character> { c ->

                val errors = mutableListOf<String>()

                // --- Name ---
                if (c.name.isBlank()) {
                    errors.add("Character must have a name.")
                } else if (c.name.length > 250) {
                    errors.add("Name too long (max 250 chars).")
                }

                // --- Power Level ---
                if ((c.powerLevel ?: 0) < 0) {
                    errors.add("Power level cannot be negative.")
                }

                // --- Age ---
                if (c.age != null && c.age < 0) {
                    errors.add("Age cannot be negative.")
                }

                // --- Abilities ---
                if (c.abilities.any { it.isBlank() }) {
                    errors.add("Abilities cannot contain empty values.")
                }

                if (c.abilities.size > 20) {
                    errors.add("Too many abilities (max 20).")
                }

                // --- Race ---
                if (c.race != null && c.race.length > 100) {
                    errors.add("Race too long (max 100 chars).")
                }

                // --- Lore ---
                if (c.lore != null && c.lore.length > 5000) {
                    errors.add("Lore too long (max 5000 chars).")
                }

                if (errors.isEmpty()) {
                    ValidationResult.Valid
                } else {
                    ValidationResult.Invalid(errors.joinToString(" | "))
                }
            }

            validate<RelationAdd> { r ->

                val errors = mutableListOf<String>()

                // --- ID sanity ---
                if (r.sourceId <= 0) {
                    errors.add("Invalid sourceId.")
                }

                if (r.targetId <= 0) {
                    errors.add("Invalid targetId.")
                }

                // --- Logical rule ---
                if (r.sourceId == r.targetId) {
                    errors.add("A character cannot form a relationship with itself.")
                }

                if (errors.isEmpty()) {
                    ValidationResult.Valid
                } else {
                    ValidationResult.Invalid(errors.joinToString(" | "))
                }
            }

            validate<FeatRequest> { f ->

                if (f.description.isBlank()) {
                    ValidationResult.Invalid("Feat description cannot be empty.")
                } else if (f.description.length > 2000) {
                    ValidationResult.Invalid("Feat description too long.")
                } else {
                    ValidationResult.Valid
                }
            }
        }



    //This one is an exception handler that determines what response
    //to be sent when the specific exception is met
    install(StatusPages){
        exception<RequestValidationException> {call, cause ->
            //Catches the "Invalid" result that we wrote and sends it back to the client
            call.respond (
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    message = cause.reasons.joinToString()
                )
            )
        }

        exception<UnauthorizedException> {call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(false, message = cause.message)
            )
        }

        exception<ForbiddenException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse<Unit>(false, message = cause.message ?: "Nope, You have no power here 🤏")
            )
        }

        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(false, message = cause.message ?: "Beep Boop, Not Found so dope.")
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(false, message = cause.message ?: "Beep Boop, Your request is Bad you doop")
            )
        }

        exception<Throwable> { call, cause ->
            cause.printStackTrace() // 🔥 for debugging

            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(false, message = "Something went wrong 😵")
            )
        }
    }

    configureRouting() //Da Menu
}
