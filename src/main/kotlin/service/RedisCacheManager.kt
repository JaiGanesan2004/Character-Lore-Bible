package service

import model.character.Character
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool

object RedisCacheManager {
    //Connects to local Redis
    val redisHost = System.getenv("REDIS_HOST") ?: "localhost"
    val redisPort = System.getenv("REDIS_PORT")?.toInt() ?: 6379
    private val pool = JedisPool(redisHost, redisPort)

    private fun key(userId: Int, charId: Int) = "char:$userId:$charId"

    fun setCharacter(character: Character){
        pool.resource.use {jedis ->
            val json = Json.encodeToString(character)
            val chacheKey = key(character.userId, character.id)

            jedis.set(chacheKey, json)
            jedis.expire(chacheKey, 3600)
        }
    }

    fun getCharacter(charId: Int, userId: Int): Character? {
        pool.resource.use { jedis ->
            val json = jedis.get(key(userId, charId)) ?: return null
            return Json.decodeFromString<Character>(json)
        }
    }

    fun evict(charId: Int, userId: Int){
        pool.resource.use {
            it.del(key(userId = userId, charId = charId))
        }
    }

}