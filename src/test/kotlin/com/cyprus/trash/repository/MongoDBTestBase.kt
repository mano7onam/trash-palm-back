package com.cyprus.trash.repository

import com.cyprus.trash.utils.Environment.mongo
import com.cyprus.trash.utils.Environment.port
import com.mongodb.ConnectionString
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver
import org.springframework.data.mongodb.core.mapping.MongoMappingContext

open class MongodbTestBase {

    private val mappingMongoConverter = MappingMongoConverter(NoOpDbRefResolver.INSTANCE, MongoMappingContext()).apply {
        setTypeMapper(DefaultMongoTypeMapper(null))
        afterPropertiesSet()
    }

    private val mainConnectionString = ConnectionString("mongodb://${mongo.host}:${mongo.port}/store")

    internal val template = ReactiveMongoTemplate(
        SimpleReactiveMongoDatabaseFactory(
            MongoClients.create(mainConnectionString),
            mainConnectionString.database ?: throw IllegalStateException()
        ),
        mappingMongoConverter
    )
}
