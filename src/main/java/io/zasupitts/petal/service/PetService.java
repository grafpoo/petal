package io.zasupitts.petal.service;

import io.zasupitts.petal.domain.Org;
import io.zasupitts.petal.domain.Pet;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Set;

public interface PetService {
    Map<String, Set<String>> getPetsWithinRadius(String miles, String zipcode);

    @SneakyThrows
    Org getOrg(int orgId);

    Pet getPet(String animalId);
}
