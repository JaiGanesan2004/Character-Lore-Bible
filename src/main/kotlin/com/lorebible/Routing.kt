package LoreBible.com.lorebible

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.responsewrapper.ApiResponse
import model.character.Archetype
import model.character.Character
import model.dtos.RelationAdd
import model.enums.Role
import model.user.User
import service.AuditService
import service.CharacterService
import service.UserService
import java.io.File

fun Application.configureRouting() {
    routing {

        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")

        static ("/uploads"){
            files("uploads")
        }

        val jwtSecret = environment.config.property("jwt.secret").getString()
        val jwtIssuer = environment.config.property("jwt.issuer").getString()
        val jwtAudience = environment.config.property("jwt.audience").getString()

        post("/register"){
            val userRequest = call.receive<User>()
            val success = UserService.register(userRequest)

            if(success)
                call.respond(status = HttpStatusCode.Created, message = ApiResponse<Unit>(success = true, message = "Registration Successful! Welcome to the Lore Bible!"))
            else
                call.respond(status = HttpStatusCode.Conflict, message = ApiResponse<Unit>(success = false, message = "Username already exists! Please try another one."))
        }

        post("/login"){
            val loginRequest = call.receive<User>()
            val isValid = UserService.login(loginRequest)

            if(isValid){
                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .withClaim("username", loginRequest.username)
                    .sign(Algorithm.HMAC256(jwtSecret))


                call.respond(status = HttpStatusCode.OK, message = ApiResponse(success = true, data = mapOf("token" to token)))
            }else{
                call.respond(status = HttpStatusCode.Unauthorized, message = ApiResponse<Unit>(success = false, message = "Invalid Credentials. Begone Peasant!"))
            }
        }


        get("/characters"){
            //Extracting parameter with default here.
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val size = call.parameters["size"]?.toIntOrNull() ?: 10
            val search = call.parameters["search"]

            //Calculate offset: (Page 1 starts at 0, page 2 starts at 10, etc...)
            val offset = getOffset(page, size)

            val characters = CharacterService.getAll(limit = size, offset = offset, search = search)

            call.respond(ApiResponse(success = true, data = characters))
        }

        get("/character/{name}"){
            val name = call.parameters["name"] ?: return@get call.respondText("Missing name parameter.", status = HttpStatusCode.BadRequest)

            val character = CharacterService.getByName(name)

            if(character != null)
                call.respond(ApiResponse(success = true, data = character))
            else
                call.respond(status = HttpStatusCode.NotFound, message = ApiResponse<Unit>(success = false, message = "Character '$name' does not exist in the lore Bible! Get your brain checked first!"))
        }

        get("/characters/role/{roleName}"){
            val roleName = call.parameters["roleName"]?.uppercase()

            val role = try{
                Role.valueOf(roleName ?: "")
            } catch (e: IllegalArgumentException){
                null
            }

            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val size = call.parameters["size"]?.toIntOrNull() ?: 10
            val offset = getOffset(page, size)

            if(role != null){
                val characters = CharacterService.getByRole(role, limit = size, offset = offset)
                call.respond(ApiResponse(success = true, data = characters))
            } else{
                call.respond(status = HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, message = "Invalid Role! You are going to the shadow realm!"))
            }

        }

        get("/stats/archetype"){
            val stats = CharacterService.getByArchetypeCounts()
            call.respond(status = HttpStatusCode.OK, message = ApiResponse(success = true, data = stats))
        }

        get("relationships/{name}"){
            val charName = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)

            val relations = RelationshipService.getRelationships(charName)
            call.respond(ApiResponse(success = true, data = relations))
        }


        authenticate ("auth-jwt"){

            get("/audit-logs"){
                val username = call.principal<JWTPrincipal>()
                    ?.payload
                    ?.getClaim("username")
                    ?.asString()

                call.respond(
                    status = HttpStatusCode.OK,
                    message = ApiResponse(success = true, data = username?.let{AuditService.getLogsForUser(it)}
                    )
                )

            }

            post("/relationships"){
                val request = call.receive<RelationAdd>()
                val success = RelationshipService.addRelationship(request)

                if(success)
                    call.respond(ApiResponse<Unit>(success = true, message = "Fate has been updated!" ))

                else
                    call.respond(status = HttpStatusCode.NotFound, message = ApiResponse<Unit>(success = false, message = "Fated to be unfated lmao🤷"))
            }

            post ("/character"){
                val multipart = call.receiveMultipart()
                var name = ""
                var role = Role.MAGE
                var powerLevel = 0
                var abilities = listOf<String>()
                var age = 0
                var race: String? =  null
                var archetype = Archetype.THE_GUARDIAN
                var lore: String? =  null
                var fileName = ""


                val uploadDir = File("uploads")
                if(!uploadDir.exists()) uploadDir.mkdirs()

                multipart.forEachPart { part ->
                    when(part){
                        is PartData.FormItem -> {
                            //Read the text fields (name, role, powerlevel, etc...)
                            when(part.name){
                                "name" -> name = part.value
                                "role" -> role = try {
                                    Role.valueOf(part.value.uppercase())
                                } catch (e: Exception){
                                    Role.MAGE
                                }
                                "powerLevel" -> powerLevel = part.value.toIntOrNull() ?: 0
                                "abilities" -> abilities = part.value.split(",")
                                "race" -> race = part.value
                                "age" -> age = part.value.toIntOrNull() ?: 0
                                "archetype" ->  archetype = try{
                                    Archetype.valueOf(part.value.uppercase())
                                } catch(e: Exception){
                                    Archetype.THE_ANTI_HERO
                                }
                                "lore" -> lore = part.value
                            }
                        }

                        is PartData.FileItem -> {
                            //Here a new file is created for each new character added to avoid overwriting
                            fileName = "portrait_${System.currentTimeMillis()}.jpg"
                            val fileBytes = part.streamProvider().use {
                                input -> File("uploads/${fileName}").outputStream().use {
                                    output -> input.copyTo(output)
                            }
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                //We are getting the file name here to add in DB
                val imageUrl = if( fileName.isNotEmpty()) "/uploads/$fileName" else null

                CharacterService.addCharacter(Character(name = name, role = role, powerLevel = powerLevel, abilities = abilities, imageUrl = imageUrl, race = race, age = age, archetype = archetype, lore = lore))

                val userName = call.getUsername()

                if(userName != null) AuditService.logAction(userName, "CREATED", name)

                call.respond(status = HttpStatusCode.Created, message = ApiResponse<Unit>(success = true, message = "The portrait of $name is archived!"))
            }

            put("/character/{name}") {
                val name = call.parameters["name"].toString()
                val updatedData = call.receive<Character>()

                val result = CharacterService.update (name, updatedData)

                if (result) {
                    val username = call.getUsername()

                    username?.let {
                        AuditService.logAction(it, "UPDATED", name)
                    }
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = ApiResponse<Unit>(success = true, message = "Updated model.character.Character Into Da Bible!",)
                    )
                }
                else
                    call.respond(status = HttpStatusCode.NotFound, message = ApiResponse<Unit>(success = false, message = "Character $name Sheet does not exist to update! Create first you moron!", ))
            }

            delete("/character/{name}") {
                val name = call.parameters["name"].toString()
                val removed = CharacterService.delete(name)

                if(removed) {
                    val username = call.getUsername()

                    username?.let {
                        AuditService.logAction(it, "DELETED", name)
                    }

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = ApiResponse<Unit>(
                            success = true,
                            message = "Character $name has been erased from history!"
                        )
                    )
                }
                else
                    call.respond (status = HttpStatusCode.NotFound, message = ApiResponse<Unit>(success = false, message = "Must've been the wind..."))
            }
        }
    }
}

fun getOffset(page: Int, size: Int): Long = ((page - 1) * size).toLong()

fun ApplicationCall.getUsername(): String? {
    return this.principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("username")
        ?.asString()
}
