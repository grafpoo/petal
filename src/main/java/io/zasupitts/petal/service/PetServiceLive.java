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
@Profile("live")
public class PetServiceLive implements PetService {

    private static final int PAGE_SIZE=250;

    private Map<Integer, Org> orgsMap = new HashMap<>();
    private Map<Integer, String> breedMap = new HashMap<>();
    private Map<String, Pet> livePetMap = new HashMap<>();

    private Map<CacheKey, Map<String, Set<String>>> cache = new HashMap<>();

    @Value("${home.lat}")
    Double homeLat;

    @Value("${home.lon}")
    Double homeLon;

    @Value("${pets.animalUrl}")
    private String petsAnimalUrl;

    @Value("${pets.apiKey}")
    private String apiKey;

    @Value("${pets.url}")
    private String petsDumpUrl;

    @Value("${pets.orgUrl}")
    private String petsOrgUrl;

    @Value("${pets.orgsUrl}")
    private String petsOrgsUrl;

    @Value("${pets.breedUrl}")
    private String petsBreedUrl;

    @Value("${pets.locationUrl}")
    private String petsByLocationUrl;

    private final RestTemplate restTemplate = (new RestTemplateBuilder()).build();

    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", apiKey);
        return headers;
    }

    public void getBreedsLookup() {
        HttpHeaders headers = createHttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        ResponseEntity<Map> response = restTemplate.exchange(petsBreedUrl, HttpMethod.GET, entity, Map.class);
        List breedList = (List) response.getBody().get("data");
        breedMap = new HashMap<>();
        for (int i = 0; i < breedList.size(); i++) {
            Map breed = (Map) breedList.get(i);
            Map attr = (Map) breed.get("attributes");
            String breedName = (String) attr.get("name");
            int id = Integer.parseInt((String)breed.get("id"));
            breedMap.put(id, breedName);
        }
        return;
    }

    public void getOrgsLookup() {
        orgsMap = new HashMap<>();
        HttpHeaders headers = createHttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
        int page = 1;
        int datasetSize = Integer.MAX_VALUE;
        while (datasetSize > page*PAGE_SIZE) {
            ResponseEntity<Map> response = restTemplate.exchange(petsOrgsUrl + "&page=" + page, HttpMethod.GET, entity, Map.class);
            Map body = response.getBody();
            if (datasetSize == Integer.MAX_VALUE) {
                Map meta = (Map) body.get("meta");
                if (meta != null) {
                    Object count = meta.get("count");
                    if (count != null) {
                        if (count instanceof Integer) datasetSize = (Integer) count;
                        else if (count instanceof String) datasetSize = Integer.parseInt((String) count);
                        else log.error("org count is weird type: "+count.getClass().getName());
                    }
                    if (datasetSize < 0)
                        log.error("Misread dataset size");
                } else {
                    log.error("couldn't read org meta");
                }
            }
            List orgList = (List) body.get("data");
            Org whoops = Org.builder()
                    .id(-1)
                    .name("Org lookup failed")
                    .build();
            for (int i = 0; i < orgList.size(); i++) {
                Map orgMap = (Map) orgList.get(i);
                int id = Integer.parseInt((String) orgMap.get("id"));
                Map attr = (Map) orgMap.get("attributes");
                Org o = Org.builder()
                        .id(id)
                        .name(stringOrNull(attr, "name"))
                        .city(stringOrNull(attr, "city"))
                        .state(stringOrNull(attr, "state"))
                        .zip(stringOrNull(attr, "postalcode"))
                        .url(stringOrNull(attr, "url"))
                        .build();
                orgsMap.put(id, o);
            }
            page++;
        }
        return;
    }
    private String stringOrNull(Map m, String attr) {
        if (m.containsKey(attr)) return m.get(attr).toString();
        return "";
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
    public Map<String, Set<String>> getPetsWithinRadius(String miles, String zipcode) {
        CacheKey cacheKey = new CacheKey(miles, zipcode);
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);
//        log.info("Getting Pets in radius "+miles+" for zip "+zipcode+" [NO CACHE]");
        if (breedMap == null) getBreedsLookup();
        String dataParam = "{\n" +
                "    \"data\": {\n" +
                "        \"filterRadius\":\n" +
                "          {\n" +
                "            \"miles\": "+miles+",\n" +
                "            \"postalcode\": "+zipcode+"\n" +
                "          }\n" +
                "    }\n" +
                "}";
        HttpHeaders headers = createHttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(dataParam, headers);

        Map<String, Set<String>> retMap = new HashMap<>();
        livePetMap = new HashMap<>();
        int page = 1;
        int datasetSize = Integer.MAX_VALUE;
        while (datasetSize > page*PAGE_SIZE) {
            String result = restTemplate.postForObject(petsByLocationUrl+"&page="+page, request, String.class);
            try {
                JsonNode rootNode = (new ObjectMapper()).readValue(result, JsonNode.class);
                if (datasetSize == Integer.MAX_VALUE) {
                    datasetSize = getInt(rootNode, "meta.count");
                    if (datasetSize < 0)
                        log.error("Misread dataset size");
                }
                JsonNode dataNode = rootNode.get("data");
                for (Iterator<JsonNode> it = dataNode.elements(); it.hasNext(); ) {
                    JsonNode node = it.next();
                    //                int breedIx = getInt(node,"relationships.breeds.data.id");
                    JsonNode animalId = node.get("id");
                    JsonNode relNode = node.path("relationships");
                    JsonNode breedsNode = relNode.get("breeds");
                    JsonNode breedsDataNode = breedsNode.get("data");
                    for (Iterator<JsonNode> it2 = breedsDataNode.elements(); it2.hasNext(); ) {
                        JsonNode breedNode = it2.next().get("id");
                        String breed = breedMap.get(breedNode.asInt());
                        Set<String> breedAnimalList;
                        if (retMap.containsKey(breed)) {
                            breedAnimalList = retMap.get(breed);
                        } else {
                            breedAnimalList = new TreeSet<>();
                            retMap.put(breed, breedAnimalList);
                        }
                        breedAnimalList.add(animalId.textValue());
                    }
                }
                cache.put(cacheKey, retMap);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            page++;
        }
        return retMap;
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

    @SneakyThrows
    public Set<Pet> getPetsLive(Set<String> petIds) {
        Set<Pet> petList = new TreeSet<>();
        for (String id : petIds) {
            if (livePetMap.containsKey(id)) {
                petList.add(livePetMap.get(id));
            } else {
                String animalUrl = petsAnimalUrl + id;
                HttpHeaders headers = createHttpHeaders();
                HttpEntity<String> entity = new HttpEntity<String>(null, headers);
                ResponseEntity<String> response = restTemplate.exchange(animalUrl, HttpMethod.GET, entity, String.class);
                JsonNode rootNode = (new ObjectMapper()).readValue(response.getBody(), JsonNode.class);
                int countReturned = getInt(rootNode, "meta.countReturned");
                if (countReturned > 0) {
                    JsonNode dataNode = rootNode.get("data");
                    for (Iterator<JsonNode> it = dataNode.elements(); it.hasNext(); ) {
                        JsonNode node = it.next();
                        Pet p = new Pet();
                        p.setAnimalID(id);
                        p.setName(getString(node, "attributes.name"));
                        p.setBreed(getString(node, "attributes.breedString"));
                        p.setAdoptionFee(getString(node, "attributes.adoptionFeeString"));
                        p.setAdoptionPending(getString(node, "attributes.isAdoptionPending"));
                        p.setAge(getString(node, "attributes.ageGroup"));
                        p.setCats(getString(node, "attributes.isCatsOk"));
                        p.setDogs(getString(node, "attributes.isDogsOk"));
                        p.setKids(getString(node, "attributes.isKidsOk"));
                        p.setCoatLength(getString(node, "attributes.coatLength"));
                        p.setDescriptionPlain(getString(node, "attributes.descriptionText"));
                        p.setSex(getString(node, "attributes.sex"));
                        p.setSize(getString(node, "attributes.sizeGroup"));
                        p.setLastUpdated(getString(node, "attributes.updatedDate"));
                        p.setPetUrl(getString(node, "attributes.pictureThumbnailUrl"));
                        try {
                            JsonNode rels = node.get("relationships");
                            JsonNode orgs = rels.get("orgs");
                            JsonNode orgdata = orgs.get("data");
                            ArrayNode orgarray = (ArrayNode) orgdata;
                            p.setOrgID(getInt(orgarray.get(0), "id"));
//                        for (Iterator<JsonNode> it2 = orgdata.elements(); it.hasNext(); ) {
//                            JsonNode onode = it.next();
//                            p.setOrgID(getInt(onode, "id"));
//                        }
                        } catch (Exception e) {
                            log.error("Error getting org for pet: " + p.getAnimalID() + " :: " + e.getMessage());
                            p.setOrgID(-1);
                        }
                        livePetMap.put(id, p);
                        petList.add(p);
                    }
                    JsonNode includedNode = rootNode.get("included");
                    for (Iterator<JsonNode> it = includedNode.elements(); it.hasNext(); ) {
                        JsonNode node = it.next();
                        String type = node.get("type").asText();
                        if ("pictures".equals(type)) {
                            String url = getString(node, "attributes.original.url");
                            if (StringUtils.isEmptyOrWhitespace(url)) {
                                // skip
                            } else {
                                for (String id2 : petIds) {
                                    if (url.contains(id2)) {
                                        livePetMap.get(id2).addPhotoUrl(url);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return petList;
    }

    public Set<String> getDogBreedsForRadius(PetParams params) {
        Map<String, Set<String>> petsWithinRadius = getPetsWithinRadius(params.getRange(), params.getZipCode());
        Set<String> retval = new TreeSet<>();
        retval.addAll(petsWithinRadius.keySet());
        return retval;
    }

    @Override
    public Pet getPet(String animalId) {
        return livePetMap.get(animalId);
    }

    public Org getCachedOrg(int id) {
        if (orgsMap == null) getOrgsLookup();
        if (orgsMap.containsKey(id))
            return orgsMap.get(id);
        Org noOrg = Org.builder()
                .id(id)
                .name("-not supplied-")
                .build();
        return noOrg;
    }

    @EqualsAndHashCode
    @Data
    @AllArgsConstructor
    private static class CacheKey {
        String range;
        String zipCode;
    }
}
