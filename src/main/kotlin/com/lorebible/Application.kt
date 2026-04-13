package LoreBible.com.lorebible

import io.ktor.server.application.*
import io.ktor.server.cio.EngineMain
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import model.Character
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import plugins.DatabaseFactory
import plugins.configureSecurity

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting()

    install(RequestValidation){
        validate <Character> { character ->
            if(character.name.isBlank()){
                ValidationResult.Invalid("A character must have a name to exist in the Lore Bible!")
            } else if ((character.powerLevel ?: 0) < 0){
                ValidationResult.Invalid("Power levels cannot be negative. Even a peasant has a 0 you fool!")
            } else{
                ValidationResult.Valid
            }

        }
    }

    install(StatusPages){
        exception<RequestValidationException> {call, cause ->
            //Catches the "Invalid" result that we wrote and sends it back to the client
            call.respondText (
                text = cause.reasons.joinToString(),
                status = HttpStatusCode.BadRequest
                )
        }
    }
}
