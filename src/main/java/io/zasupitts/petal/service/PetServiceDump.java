package io.zasupitts.petal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zasupitts.petal.api.MapquestApiReturn;
import io.zasupitts.petal.domain.Pet;
import io.zasupitts.petal.domain.PetOrg;
import io.zasupitts.petal.web.PetParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@Slf4j
@Profile("dailydump")
public class PetServiceDump extends PetService {

    private static final int PAGE_SIZE=250;

    private Map<Integer, PetOrg> orgsMap = new HashMap<>();
    private Map<Integer, String> breedMap = new HashMap<>();
    private Map<String, Pet> dailyOrgMap = new HashMap<>();
    private Map<String, Pet> dailyPetMap = new HashMap<>();

    private Map<CacheKey, Map<String, Set<String>>> cache = new HashMap<>();

    private final PetOrg NO_ORG = PetOrg.builder()
            .orgID(0)
            .name("-not supplied-")
            .build();

    @Value("${home.lat}")
    Double homeLat;

    @Value("${home.lon}")
    Double homeLon;

    @Value("${use.daily}")
    Boolean useDaily;

    @Value("${pets.url}")
    private String petsDumpUrl;

    @Value("${orgs.url}")
    private String orgsDumpUrl;

    @Value("${mapquest.url}")
    private String latLonUrl;

    private RestTemplate restTemplate = (new RestTemplateBuilder()).build();

    @Override
    @SneakyThrows
    public Map<String, Set<String>> getPetsWithinRadius(String milesStr, String zipcode) {
        Double miles = Double.parseDouble(milesStr);
        Map<String, Set<String>> retMap = new HashMap<>();
        String result = restTemplate.getForObject(petsDumpUrl, String.class);
        Pet[] pets = (new ObjectMapper()).readValue(result, Pet[].class);
        dailyPetMap = new HashMap<>();
        for (Pet p : pets) {
            dailyPetMap.put(p.getAnimalID(), p);
            PetOrg org = getOrg(p.getOrgID());
            Double orgLat = org.getLat();
            Double orgLon = org.getLon();
            if (orgLat != null && orgLon != null) {
                Double metres = 6371e3; // metres
                double p1 = orgLat * Math.PI / 180;
                double p2 = homeLat * Math.PI / 180;
                double l1 = (homeLat - orgLat) * Math.PI / 180;
                double l2 = (homeLon - orgLon) * Math.PI / 180;
                double a = Math.sin(l1 / 2) * Math.sin(l1 / 2) +
                        Math.cos(p1) * Math.cos(p2) *
                                Math.sin(l2 / 2) * Math.sin(l2 / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                double inMeters = metres * c;
                double orgMiles = inMeters / 1609.34;
//                log.info("Miles to "+org.getName()+"/"+org.getZip()+" = "+orgMiles);
                if (orgMiles <= miles) {
                    if (!StringUtils.isEmptyOrWhitespace(p.getBreed()))
                        addToBreedMap(p.getBreed(), p.getAnimalID(), retMap);
                    if (!StringUtils.isEmptyOrWhitespace(p.getPrimaryBreed()))
                        addToBreedMap(p.getPrimaryBreed(), p.getAnimalID(), retMap);
                    if (!StringUtils.isEmptyOrWhitespace(p.getSecondaryBreed()))
                        addToBreedMap(p.getSecondaryBreed(), p.getAnimalID(), retMap);
                }
            }
        }
        return retMap;
    }

    private void addToBreedMap(String breed, String id, Map<String, Set<String>> map) {
        Set<String> breedAnimalList;
        if (map.containsKey(breed)) {
            breedAnimalList = map.get(breed);
        } else {
            breedAnimalList = new TreeSet<>();
            map.put(breed, breedAnimalList);
        }
        breedAnimalList.add(id);
        return;
    }

    @Override
    @SneakyThrows
    public PetOrg getOrg(int orgID) {
        if (orgsMap.containsKey(orgID)) return orgsMap.get(orgID);
        String result = restTemplate.getForObject(orgsDumpUrl+orgID, String.class);
        PetOrg[] orgs = (new ObjectMapper()).readValue(result, PetOrg[].class);
        PetOrg org = NO_ORG;
        if (orgs.length > 0) {
            org = orgs[0];
            if (!StringUtils.isEmptyOrWhitespace(org.getZip())) {
                Double[] latLon = getLatLon(org.getZip());
                org.setLat(latLon[0]);
                org.setLon(latLon[1]);
            }
        } else {
        }
        orgsMap.put(orgID, org);
        return org;
    }

    private Double[] getLatLon(String zip) {
        MapquestApiReturn result = restTemplate.getForObject(latLonUrl+zip, MapquestApiReturn.class);
        if (result != null) {
            if (result.getResults().size() > 0) {
                MapquestApiReturn.MapquestResult mrs = result.getResults().get(0);
                if (mrs.getLocations().size() > 0) {
                    MapquestApiReturn.MapquestLocation location = mrs.getLocations().get(0);
                    if (location.getLatLng() != null) {
                        return new Double[] {location.getLatLng().getLat(), location.getLatLng().getLng()};
                    }
                }
            }
        }
        return new Double[] {0., 0.};
    }

    public Set<String> getDogBreedsForRadius(PetParams params) {
        Map<String, Set<String>> petsWithinRadius = getPetsWithinRadius(params.getRange(), params.getZipCode());
        Set<String> retval = new TreeSet<>();
        retval.addAll(petsWithinRadius.keySet());
        return retval;
    }

    @Override
    public Pet getPet(String animalId) {
        return dailyPetMap.get(animalId);
    }

    public Set<Pet> getPets(Set<String> petIds) {
        Set<Pet> petList = new TreeSet<>();
        for (String id : petIds) {
            Pet p = getPet(id);
            if (p != null) petList.add(p);
        }
        return petList;
    }

    @EqualsAndHashCode
    @Data
    @AllArgsConstructor
    private static class CacheKey {
        String range;
        String zipCode;
    }
}
