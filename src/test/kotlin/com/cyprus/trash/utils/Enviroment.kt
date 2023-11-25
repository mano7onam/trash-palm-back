package com.cyprus.trash.utils

import org.slf4j.LoggerFactory
import org.testcontainers.containers.ContainerState
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Environment {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private const val MONGODB_IMAGE_NAME = "mongo:6.0.1"
    private const val MONGODB_DEFAULT_PORT = 27017

    @JvmStatic
    val mongo: KGenericContainer by lazy {
        KGenericContainer(MONGODB_IMAGE_NAME)
            .withExposedPorts(MONGODB_DEFAULT_PORT)
            .withLogConsumer(Slf4jLogConsumer(logger))
            .apply { start() }
    }

    class KGenericContainer(imageName: String) :
        GenericContainer<KGenericContainer>(DockerImageName.parse(imageName))

    val ContainerState.address: String get() = "$host:$port"
    val ContainerState.port: Int get() = firstMappedPort
}

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
