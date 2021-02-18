package com.fireflysource.serialization.impl.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fireflysource.serialization.SerializationService

class JsonSerializationService(val mapper: ObjectMapper = ObjectMapper()) : SerializationService {

    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    override fun <T : Any> read(content: String, clazz: Class<T>): T = mapper.readValue(content, clazz)

    override fun <T : Any> read(content: String, ref: TypeReference<T>): T = mapper.readValue(content, ref)

    override fun write(obj: Any): String = mapper.writeValueAsString(obj)

}

inline fun <reified T : Any> SerializationService.read(content: String): T {
    return this.read(content, object : TypeReference<T>() {})
}

