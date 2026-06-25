package com.hostdesign24.jobportal.dto.company;

import com.hostdesign24.jobportal.model.enums.Region;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyAddressDto {

    private String street;

    private String unitApt;

    private String city;

    private String stateRegion;

    /** Cameroonian administrative region (one of 10). */
    private Region region;

    private String zip;

    private String country;

    private String longitude;

    private String latitude;

    private String phone;
}
