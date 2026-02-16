package com.hostdesign24.jobportal.repository;

import com.hostdesign24.jobportal.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID>, JpaSpecificationExecutor<File> {

  @Query("SELECT f FROM File f WHERE f.ownerId IN :ownerIds AND f.ownerType = :ownerType")
  List<File> findByOwnerIdInAndOwnerType(@Param("ownerIds") Collection<UUID> ownerIds,
      @Param("ownerType") String ownerType);

  @Query("SELECT f FROM File f WHERE f.ownerId IN :ownerIds")
  List<File> findByOwnerIdIn(@Param("ownerIds") Collection<UUID> ownerIds);

    List<File> findByCreatedBy(UUID ownerId);
}
