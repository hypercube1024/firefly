package com.fireflysource.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fireflysource.serialization.impl.json.JsonSerializationService

object SerializationServiceFactory {

    val json = json()

    @JvmOverloads
    fun json(mapper: ObjectMapper = ObjectMapper()): SerializationService = JsonSerializationService(mapper)

}