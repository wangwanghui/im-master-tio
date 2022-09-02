package com.octv.im.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
/*
* mongoDB 连接池可后期设置分片
* */
@Configuration
public class MongoConfig {
    @Value("${spring.data.mongodb.uri}")
    private String url;

    @Bean
    public MongoTemplate mongoTemplate(SimpleMongoClientDatabaseFactory factory, MappingMongoConverter converter) {
        return new MongoTemplate(factory, converter);
    }
    /**
     * 转换器
     */
    @Bean
    public MappingMongoConverter mappingMongoConverter(SimpleMongoClientDatabaseFactory factory, MongoMappingContext context,
            MongoCustomConversions conversions) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
        MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
        mappingConverter.setCustomConversions(conversions);
        mappingConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return mappingConverter;
    }

    @Bean
    public SimpleMongoClientDatabaseFactory simpleMongoClientDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory(url);
    }
}
