package com.vikadata.api.modular.space.repository;

import com.vikadata.schema.AuditSpaceSchema;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditSpaceRepository extends MongoRepository<AuditSpaceSchema, Long> {
}
