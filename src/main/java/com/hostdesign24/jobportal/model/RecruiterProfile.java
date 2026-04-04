package com.hostdesign24.jobportal.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recruiter_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterProfile extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "user_id")
    @MapsId
    private User user;

    private String firstName;
    private String lastName;
    private String city;
    private String state;
    private String country;
    private String company;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_photo_id", unique = true)
    private File profilePhoto;

    public RecruiterProfile(User user) {
        this.user = user;
    }
}