package com.hostdesign24.jobportal.dto.company;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyAddressDto {

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

