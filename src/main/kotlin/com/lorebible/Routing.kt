package com.lorebible

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import exception.BadRequestException
import exception.UnauthorizedException
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.jvm.javaio.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.responsewrapper.ApiResponse
import model.character.Archetype
import model.character.Character
import model.dtos.CharacterUpdateRequest
import model.dtos.FeatRequest
import model.dtos.RelationAdd
import model.enums.FeatLevel
import model.enums.Role
import model.user.User
import service.AuditService
import service.CharacterService
import service.ExportService
import service.FeatServices
import service.RedisCacheManager
import service.RelationshipService
import service.UserService
import java.io.File
import java.util.Date

fun Application.configureRouting() {
    routing {

        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")

        staticFiles("/uploads", File("uploads"))


        val jwtSecret = environment.config.property("jwt.secret").getString()
        val jwtIssuer = environment.config.property("jwt.issuer").getString()
        val jwtAudience = environment.config.property("jwt.audience").getString()
        val jwtExpiration = environment.config.property("jwt.expiration").getString()

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
                val currentDate = System.currentTimeMillis()

                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .withClaim("username", loginRequest.username)
                    .withIssuedAt(Date(currentDate))
                    .withExpiresAt(Date(currentDate + jwtExpiration.toInt()))
                    .sign(Algorithm.HMAC256(jwtSecret))

                call.respond(status = HttpStatusCode.OK, message = ApiResponse(success = true, data = mapOf("token" to token)))
            }else{
                call.respond(status = HttpStatusCode.Unauthorized, message = ApiResponse<Unit>(success = false, message = "Invalid Credentials. Begone Peasant!"))
            }
        }

        authenticate ("auth-jwt"){

            //Get All Characters
            get("/characters"){
                //Extracting parameter with default here.
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val size = call.parameters["size"]?.toIntOrNull() ?: 10
                val search = call.parameters["search"]

                //Calculate offset: (Page 1 starts at 0, page 2 starts at 10, etc...)
                val offset = getOffset(page, size)

                val userId = call.requireUserId()

                val characters = CharacterService.getAll(limit = size, offset = offset, search = search, userId = userId)

                call.respond(ApiResponse(success = true, data = characters))
            }

            //Get Character by its id
            get("/character/{id}"){
                val charId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException()

                val userId = call.requireUserId()

                val character = CharacterService.getCharacterById(charId, userId)

                if(character != null)
                    call.respond(ApiResponse(success = true, data = character))
                else
                    call.respond(status = HttpStatusCode.NotFound, message = ApiResponse<Unit>(success = false, message = "Character '$charId' does not exist in the lore Bible! Get your brain checked first!"))
            }

            //Get Characters by their role
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

                val userId = call.requireUserId()

                if(role != null){
                    val characters = CharacterService.getByRole(role, limit = size, offset = offset, userId = userId)
                    call.respond(ApiResponse(success = true, data = characters))
                } else{
                    call.respond(status = HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, message = "Invalid Role! You are going to the shadow realm!"))
                }

            }

            //Get total counts of each ArchType
            get("/stats/archetype"){
                val userId = call.requireUserId()
                val stats = CharacterService.getByArchetypeCounts(userId)
                call.respond(status = HttpStatusCode.OK, message = ApiResponse(success = true, data = stats))
            }

            //Get Relationship for the specified character
            get("relationships/{id}"){
                val charId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException()

                val userId = call.requireUserId()

                val relations = RelationshipService.getRelationships(charId, userId)
                call.respond(ApiResponse(success = true, data = relations))
            }

            //Get the audit logs for the user
            get("/audit-logs"){
                val userId = call.requireUserId()

                call.respond(
                    status = HttpStatusCode.OK,
                    message = ApiResponse(success = true, data = AuditService.getLogsForUser(userId)
                    )
                )
            }

            get("/character/{id}/feats"){
                val charId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)

                val userId = call.requireUserId()

                val feats = FeatServices.getFeatsByCharacterId(charId, userId)

                if(feats.isEmpty()){
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, message = "No feats found for character $charId")
                    )
                }

                call.respond(
                    ApiResponse(
                        success = true,
                        data = feats
                    )
                )
            }

            //Exporting the character for users
            get("/character/{id}/export") {
                val charId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException()
                val userId = call.requireUserId()

                // 1. Try to get from Redis first (Super Fast since it's caching!)
                var character = try {
                    RedisCacheManager.getCharacter(charId = charId, userId = userId)
                } catch (e: Exception){
                    println("⚠ Redis is down, skipping cache...")
                    null
                }


                // 2. If Redis is empty, go to the Database (Slower)
                if (character == null) {
                    character = CharacterService.getCharacterById(charId,userId)

                    // 3. Save it to Redis so next time is instant
                    if (character != null) {
                        RedisCacheManager.setCharacter(character)
                    }
                }

                // 4. Handle Not Found
                if (character == null) {
                    return@get call.respond(HttpStatusCode.NotFound, "Character Not Found")
                }

                // 5. Build and Send the File
                val markdown = ExportService.characterToMarkdown(character, userId)
                    ?: "# Error\nCharacter data could not be generated, Beep Boop!"

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        "${(character.name).replace(" ", "_")}.md"
                    ).toString()
                )

                AuditService.logAction(userId,"EXPORTED CHARACTER LORE", charId, character.name)

                call.respondText(markdown, ContentType.parse("text/markdown"))
            }


            post("/relationships"){
                val userId = call.requireUserId()
                val request = call.receive<RelationAdd>()
                val success = RelationshipService.addRelationship(request, userId)

                val charName = CharacterService.getCharacterNameById(userId, request.sourceId)

                if(success) {
                    AuditService.logAction(userId,"RELATIONSHIP TO CHARACTER ID:${request.targetId} ADDED" ,request.sourceId, charName)

                    call.respond(ApiResponse<Unit>(success = true, message = "Fate has been updated!"))
                }

                else
                    call.respond(status = HttpStatusCode.NotFound, message = ApiResponse<Unit>(success = false, message = "Fated to be unfated lmao 🤷"))
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
                            val contentType = part.contentType?.contentType + "/" + part.contentType?.contentSubtype

                            if(part.contentType == null || contentType !in listOf("image/jpeg", "image/png", "image/webp"))
                            {
                                part.dispose()
                                return@forEachPart
                            }
                            //Here a new file is created for each new character added to avoid overwriting
                            fileName = "portrait_${System.currentTimeMillis()}.jpg"
                            val fileBytes = part.provider().toInputStream().use { input ->
                                File("uploads/$fileName").outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                        }
                        else -> {}
                    }
                    part.dispose()
                }

                //We are getting the file name here to add in DB
                val imageUrl = if( fileName.isNotEmpty()) "/uploads/$fileName" else null

                val userId = call.requireUserId()

                val charId = CharacterService.addCharacter(Character(name = name, role = role, powerLevel = powerLevel, abilities = abilities, imageUrl = imageUrl, race = race, age = age, archetype = archetype, lore = lore), userId)

                AuditService.logAction(userId,"CHARACTER CREATED", charId, name)

                call.respond(status = HttpStatusCode.Created, message = ApiResponse<Unit>(success = true, message = "The portrait of $name is archived!"))
            }

            //Updating the Feats for the specified Character
            put("/character/{id}/feats"){
                val charId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException()

                val request = call.receive<FeatRequest>()
                val userId = call.requireUserId()
                val charName = CharacterService.getCharacterNameById(userId, charId)

                FeatServices.setFeat(charId, request, userId)

                AuditService.logAction(userId,"UPSERTED FEATS",charId, charName)

                call.respond(
                    status = HttpStatusCode.OK,
                    message = ApiResponse<Unit>(
                        success = true,
                        message = "Feat updated for $charId at ${request.category} level 🔥"
                    )
                )
            }

            //To update the new details given by the user about the specific character
           patch("/character/{id}"){
               val charId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException()

               val userId = call.getUserId() ?: return@patch call.respond(HttpStatusCode.Unauthorized)

               val request = call.receive<CharacterUpdateRequest>()

               val updated = CharacterService.updateById(charId, request, userId)

               if(updated){
                   val charName = CharacterService.getCharacterNameById(userId, charId)
                   AuditService.logAction(userId,"PATCHED CHARACTER",  charId, charName )
                   call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, message = "Character Ascended 🐦‍🔥"))
               } else{
                   call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, message = "Ascension Failed 🌠"))
               }
           }

            //Delete the feats of specified character ID
            delete("/character/{id}/feats/{category}"){
                val charId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException()

                val categoryParam = call.parameters["category"]?.uppercase()

                val userId = call.requireUserId()

                val category = try{
                    FeatLevel.valueOf(categoryParam ?: "")
                } catch(e: Exception){
                    null
                }

                if(category == null){
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, message = "Invalid feat Category my guy 😵"))
                }

                val charName = CharacterService.getCharacterNameById(userId, charId)
                FeatServices.deleteFeat(charId, category, userId)

                AuditService.logAction(userId,"DELETED FEATS",charId, charName)

                call.respond(
                    ApiResponse<Unit>(
                        success = true,
                        message = "Feat removed for character id:$charId\nat $category level 🧹"
                    )
                )

            }

            //Delete the character with specified it
            delete("/character/{id}") {
                val charId = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException()
                val userId = call.requireUserId()

                val removed = CharacterService.deleteById(charId, userId)

                if(removed.first) {
                    AuditService.logAction(userId,"DELETED CHARACTER", charId,removed.second!!)

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = ApiResponse<Unit>(
                            success = true,
                            message = "Character has been erased from Bible 😶‍🌫️!"
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

//For extracting the username from the token.
fun ApplicationCall.getUsername(): String? {
    return this.principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("username")
        ?.asString()
}

fun ApplicationCall.getUserId(): Int? =
    getUsername()?.let(UserService::getUserId)

fun ApplicationCall.requireUserId(): Int =
    getUserId() ?: throw UnauthorizedException("Invalid or expired Amulet, Begone Witch 🧙‍♂️!")
