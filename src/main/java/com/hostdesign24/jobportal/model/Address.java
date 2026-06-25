package com.hostdesign24.jobportal.model;

import com.hostdesign24.jobportal.model.enums.Region;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  /**
   * Cameroon-specific administrative region.
   * Optional and orthogonal to {@link #stateRegion} which remains for non-Cameroon addresses.
   */
  @Enumerated(EnumType.STRING)
  private Region region;

  private String zip;
  private String country;
  private String longitude;
  private String latitude;
  private String phone;
}
