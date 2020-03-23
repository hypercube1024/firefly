package com.fireflysource.net.tcp.secure

import com.fireflysource.net.tcp.secure.common.AbstractSecureEngineFactory
import com.fireflysource.net.tcp.secure.conscrypt.FileConscryptSSLContextFactory
import com.fireflysource.net.tcp.secure.conscrypt.SelfSignedCertificateConscryptSSLContextFactory
import com.fireflysource.net.tcp.secure.jdk.FileOpenJdkSSLContextFactory
import com.fireflysource.net.tcp.secure.jdk.SelfSignedCertificateOpenJdkSSLContextFactory
import com.fireflysource.net.tcp.secure.utils.SecureUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.security.KeyStore
import java.util.stream.Stream
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory

class TestSSLContextFactory {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments(
                    "jsse",
                    FileOpenJdkSSLContextFactory("fireflyKeystore.jks", "123456", "654321", "JKS")
                ),
                arguments(
                    "jsse",
                    SelfSignedCertificateOpenJdkSSLContextFactory()
                ),
                arguments(
                    "Conscrypt",
                    FileConscryptSSLContextFactory("fireflyKeystore.jks", "123456", "654321", "JKS")
                ),
                arguments(
                    "Conscrypt",
                    SelfSignedCertificateConscryptSSLContextFactory()
                )
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

    @Test
    @DisplayName("should load self signed certificate successfully.")
    fun testKeyStore() {
        val ks = KeyStore.getInstance("JKS")
        SecureUtils.getSelfSignedCertificate().use {
            ks.load(it, "123456".toCharArray())
            println("size: ${ks.size()}")
            assertTrue(ks.size() > 0)
            val certificate = ks.getCertificate("fireflyselfcert")
            println(certificate.type)
            assertEquals("X.509", certificate.type)

            val km = KeyManagerFactory.getInstance("SunX509")
            km.init(ks, "654321".toCharArray())
            assertTrue(km.keyManagers.isNotEmpty())
            val tmf = TrustManagerFactory.getInstance("SunX509")
            tmf.init(ks)
            assertTrue(tmf.trustManagers.isNotEmpty())
        }
    }
}