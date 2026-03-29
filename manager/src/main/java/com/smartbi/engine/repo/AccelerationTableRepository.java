package com.smartbi.engine.repo;

import com.smartbi.engine.domain.AccelerationTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccelerationTableRepository extends JpaRepository<AccelerationTable, Long> {

    List<AccelerationTable> findAllByOrderByUpdatedAtDesc();

    Page<AccelerationTable> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    long countByStatus(com.smartbi.engine.domain.AccelerationStatus status);

    boolean existsByNameIgnoreCaseAndSchemaNameIgnoreCase(String name, String schemaName);

    boolean existsByNameIgnoreCaseAndSchemaNameIgnoreCaseAndIdNot(String name, String schemaName, Long id);
}
