package service

import model.character.Character
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.exposedLogger
import redis.clients.jedis.JedisPool

object RedisCacheManager {
    //Connects to local Redis
    val redisHost = System.getenv("REDIS_HOST") ?: "localhost"
    val redisPort = System.getenv("REDIS_PORT")?.toInt() ?: 6379
    private val pool = JedisPool(redisHost, redisPort)

    private fun key(userId: Int, charId: Int) = "char:$userId:$charId"

    fun setCharacter(character: Character){
        try {
            pool.resource.use { jedis ->
                val json = Json.encodeToString(Character.serializer(), character)
                val cacheKey = key(character.userId, character.id)

                jedis.set(cacheKey, json)
                jedis.expire(cacheKey, 3600)
            }
        } catch (e: Exception){
            exposedLogger.error(e.stackTraceToString())
        }
    }

    fun getCharacter(charId: Int, userId: Int): Character? {
        try {
            pool.resource.use { jedis ->
                val json = jedis.get(key(userId, charId)) ?: return null
                return Json.decodeFromString<Character>(json)
            }
        } catch (e: Exception){
            exposedLogger.error(e.stackTraceToString())
            return null
        }
    }

    fun evict(charId: Int, userId: Int){
        try {
            pool.resource.use {
                it.del(key(userId = userId, charId = charId))
            }
        }catch (e: Exception){
            exposedLogger.error(e.stackTraceToString())
        }
    }

}