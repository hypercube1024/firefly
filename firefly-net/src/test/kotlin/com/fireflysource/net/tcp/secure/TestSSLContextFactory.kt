package com.fireflysource.net.tcp.secure

import com.fireflysource.net.tcp.secure.common.AbstractSecureEngineFactory
import com.fireflysource.net.tcp.secure.conscrypt.FileConscryptSSLContextFactory
import com.fireflysource.net.tcp.secure.conscrypt.SelfSignedCredentialConscryptSSLContextFactory
import com.fireflysource.net.tcp.secure.jdk.FileOpenJdkSSLContextFactory
import com.fireflysource.net.tcp.secure.jdk.SelfSignedCredentialOpenJdkSSLContextFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class TestSSLContextFactory {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments("jsse", FileOpenJdkSSLContextFactory("/fireflyKeystore.jks", "123456", null)),
                arguments("jsse", SelfSignedCredentialOpenJdkSSLContextFactory()),
                arguments("Conscrypt", FileConscryptSSLContextFactory("/fireflyKeystore.jks", "123456", null)),
                arguments("Conscrypt", SelfSignedCredentialConscryptSSLContextFactory())
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should create secure engine factory successfully.")
    fun test(name: String, factory: AbstractSecureEngineFactory) {
        val sslContext = factory.sslContext
        println(sslContext.provider.name)
        println(sslContext.provider.info)
        assertTrue(sslContext.provider.name.contains(name, true))
    }
}