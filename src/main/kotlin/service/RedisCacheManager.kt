package service

import model.character.Character
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import redis.clients.jedis.JedisPool

object RedisCacheManager {
    //Connects to local Redis
    private val pool = JedisPool("localhost", 6379)

    fun setCharacter(character: Character){
        pool.resource.use {jedis ->
            val json = Json.encodeToString(character)
            jedis.set("char:${character.name}", json)

            jedis.expire("char:${character.name}", 3600)
        }
    }

    fun getCharacter(name: String): Character? {
        pool.resource.use { jedis ->
            val json = jedis.get("char:$name") ?: return null
            return Json.decodeFromString<Character>(json)
        }
    }

    fun evict(name: String){
        pool.resource.use {
            it.del("char:$name")
        }
    }

}