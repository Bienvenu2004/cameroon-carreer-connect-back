package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "files")
public class File extends BaseEntity {
  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String url;
  private String type;
  private UUID ownerId;
  private String ownerType;
  private String publicId;
  private Long size;
  private String relatedEntity;
}