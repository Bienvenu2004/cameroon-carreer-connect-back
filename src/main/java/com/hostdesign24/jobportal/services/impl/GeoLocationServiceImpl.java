package com.hostdesign24.jobportal.services.impl;

import com.hostdesign24.jobportal.services.GeolocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoLocationServiceImpl implements GeolocationService {
    
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getLocationFromIP(String ipAddress) {
        // Skip localhost/private IPs
        if (ipAddress.startsWith("192.168.") || 
            ipAddress.startsWith("10.") || 
            ipAddress.equals("127.0.0.1") ||
            ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return "Local Network";
        }
        
        try {
            String url = "http://ip-api.com/json/" + ipAddress + "?fields=status,city,country";
            Map response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && "success".equals(response.get("status"))) {
                String city = (String) response.get("city");
                String country = (String) response.get("country");
                
                if (city != null && country != null) {
                    return city + ", " + country;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get location for IP: {}", ipAddress, e);
        }
        
        return "Unknown Location";
    }
}