package com.hostdesign24.jobportal.dto.jobActivityPost;

import com.hostdesign24.jobportal.model.enums.Region;
import lombok.Getter;
import lombok.Setter;

/**
 * Wire shape matches the frontend's AddressDto closely.
 * `region` is the Cameroon-specific enum used by the UI for translation
 * keys and filtering; `stateRegion` is kept for non-Cameroon addresses.
 */
@Getter
@Setter
public class JobLocationDto {
    private String street;
    private String city;
    private Region region;
    private String stateRegion;
    private String country;
}
