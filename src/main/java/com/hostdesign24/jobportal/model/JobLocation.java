package com.hostdesign24.jobportal.model;

import jakarta.persistence.Entity;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
public class JobLocation extends BaseEntity {

    private String city;
    private String state;
    private String country;

    @Override
    public String toString() {
        return "JobLocation{" +
                "id=" + this.getId() +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}