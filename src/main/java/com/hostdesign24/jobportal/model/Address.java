package com.hostdesign24.jobportal.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Address {
  private String street;
  private String unitApt;
  private String city;
  private String stateRegion;
  private String zip;
  private String country;
  private String longitude;
  private String latitude;
  private String phone;
}