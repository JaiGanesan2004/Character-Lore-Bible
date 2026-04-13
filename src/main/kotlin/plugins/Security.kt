package plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

    fun Application.configureSecurity() {
        val jwtSecret = environment.config.property("jwt.secret").getString()
        val jwtIssuer = environment.config.property("jwt.issuer").getString()
        val jwtAudience = environment.config.property("jwt.audience").getString()

        install(Authentication){
            jwt("auth-jwt"){
                realm = "Access to the Lore Bible"
                verifier(
                    JWT.require(Algorithm.HMAC256(jwtSecret))
                        .withAudience(jwtAudience)
                        .build()
                )

                validate{
                    credential ->
                        if(credential.payload.audience.contains(jwtAudience)){
                            JWTPrincipal(credential.payload)
                        } else null
                }
            }
        }
    }
