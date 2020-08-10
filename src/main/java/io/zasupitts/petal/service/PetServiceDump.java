package io.zasupitts.petal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import io.zasupitts.petal.domain.Org;
import io.zasupitts.petal.domain.Pet;
import io.zasupitts.petal.web.PetParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.util.StringUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
@Slf4j
@Profile("dailydump")
public class PetServiceDump implements PetService {

    private static final int PAGE_SIZE=250;

    private Map<Integer, Org> orgsMap = new HashMap<>();
    private Map<Integer, String> breedMap = new HashMap<>();
    private Map<String, Pet> livePetMap = new HashMap<>();
    private Map<String, Pet> dailyPetMap = new HashMap<>();

    private Map<CacheKey, Map<String, Set<String>>> cache = new HashMap<>();

    @Value("${home.lat}")
    Double homeLat;

    @Value("${home.lon}")
    Double homeLon;

    @Value("${use.daily}")
    Boolean useDaily;

    @Value("${pets.url}")
    private String petsDumpUrl;

    @Value("${pets.orgUrl}")
    private String orgsDumpUrl;

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
            Org org = getOrg(p.getOrgID());
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

    private String getString(JsonNode root, String path) {
        JsonNode curNode = root;
        String[] split = path.split("\\.");
        for (int i = 0; i < split.length; i++) {
            curNode = curNode.get(split[i]);
            if (curNode == null || curNode.isMissingNode()) {
                log.error("unretrievable node: "+path);
                return "-error-";
            }
        }
        return curNode.textValue();
    }

    private int getInt(JsonNode root, String path) {
        JsonNode curNode = root;
        String[] split = path.split("\\.");
        for (int i = 0; i < split.length; i++) {
            curNode = curNode.get(split[i]);
            if (curNode.isMissingNode()) {
                log.error("unretrievable node: "+path);
                return -1;
            }
        }
        if (curNode instanceof IntNode) {
            return ((IntNode)curNode).intValue();
        }
        if (curNode.isNull()) return 0;
        String intString = curNode.textValue();
        return Integer.parseInt(intString);
    }

    private double getDouble(JsonNode root, String path) {
        JsonNode curNode = root;
        String[] split = path.split("\\.");
        for (int i = 0; i < split.length; i++) {
            curNode = curNode.get(split[i]);
            if (curNode.isMissingNode()) {
                log.error("unretrievable node: "+path);
                return -1;
            }
        }
        if (curNode instanceof DoubleNode) {
            return ((DoubleNode)curNode).doubleValue();
        }
        if (curNode.isNull()) return 0.0;
        String d = curNode.textValue();
        return Double.parseDouble(d);
    }

    @Override
    @SneakyThrows
    public Org getOrg(int orgId) {
        if (orgsMap.containsKey(orgId)) return orgsMap.get(orgId);
        String animalUrl = petsOrgUrl + orgId;
        HttpHeaders headers = createHttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(animalUrl, HttpMethod.GET, entity, String.class);
        JsonNode rootNode = (new ObjectMapper()).readValue(response.getBody(), JsonNode.class);
        int count = getInt(rootNode, "meta.countReturned");
        if (count > 0) {
            JsonNode dataNode = rootNode.get("data");
            for (Iterator<JsonNode> it = dataNode.elements(); it.hasNext(); ) {
                JsonNode node = it.next();
                Org p = Org.builder()
                        .id(orgId)
                        .name(getString(node, "attributes.name"))
                        .city(getString(node, "attributes.city"))
                        .state(getString(node, "attributes.state"))
                        .zip(getString(node, "attributes.postalcode"))
                        .url(getString(node, "attributes.url"))
                        .lat(getDouble(node, "attributes.lat"))
                        .lon(getDouble(node, "attributes.lon"))
                        .build();
                orgsMap.put(orgId, p);
                return p;
            }
        }
        log.error("Didn't find org: "+orgId);
        return Org.builder()
                .name("Something went wrong, sorry")
                .id(orgId)
                .lat(0.)
                .lon(0.)
                .build();
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

    private Double[] getLatLon(String zip) {
        String
    }
    @EqualsAndHashCode
    @Data
    @AllArgsConstructor
    private static class CacheKey {
        String range;
        String zipCode;
    }
}
