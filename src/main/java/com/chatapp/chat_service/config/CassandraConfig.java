package com.chatapp.chat_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.cassandra.config.SchemaAction;
@Configuration
@EnableCassandraRepositories(basePackages = "com.chatapp.chat_service.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Override
    protected String getKeyspaceName() {
        return "chat_app";
    }

    @Override
    protected String getContactPoints() {
        return "127.0.0.1";
    }

    @Override
    protected int getPort() {
        return 9042;
    }
    @Override
    protected String getLocalDataCenter() {
        return "datacenter1";
    }
    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.NONE;
    }
}