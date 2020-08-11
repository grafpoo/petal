package io.zasupitts.petal.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetOrg {
    private int orgID;
    private String status;
    private String name;
    private String city;
    private String address;
    private String state;
    private String country;
    private String zip;
    private String phone;
    private String fax;
    private String email;
    private String orgurl;
    private String facebookUrl;
    private String orgType;
    private String orgSpecies;
    private String serveAreas;
    private String adoptionProcess;
    private String about;
    private String meetPets;
    private String services;
    private String allowAppSubmissions;
    private String messageOrg;
    private Double lat;
    private Double lon;
}
/*
          "serveAreas": "",
          "adoptionProcess": "",
          "about": "",
          "meetPets": "",
          "services": "Adoption",
          "allowAppSubmissions": "No",
          "messageOrg": "Yes"
*/