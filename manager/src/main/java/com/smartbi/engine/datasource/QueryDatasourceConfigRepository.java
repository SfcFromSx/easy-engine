package com.smartbi.engine.datasource;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QueryDatasourceConfigRepository extends JpaRepository<QueryDatasourceConfig, Long> {

    Optional<QueryDatasourceConfig> findByName(String name);
}
