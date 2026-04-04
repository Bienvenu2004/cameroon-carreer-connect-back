package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company extends BaseEntity {

    private String name;

    @ManyToOne
    @JoinColumn(name = "company_logo")
    private File logo;

    @Embedded
    private Address address;
}