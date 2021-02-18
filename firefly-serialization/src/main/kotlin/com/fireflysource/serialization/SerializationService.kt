package com.fireflysource.serialization

import com.fasterxml.jackson.core.type.TypeReference

interface SerializationService {

    fun <T : Any> read(content: String, clazz: Class<T>): T

    fun <T : Any> read(content: String, ref: TypeReference<T>): T

    fun write(obj: Any): String

}