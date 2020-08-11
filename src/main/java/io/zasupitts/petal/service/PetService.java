package io.zasupitts.petal.service;

import io.zasupitts.petal.domain.PetOrg;
import io.zasupitts.petal.domain.Pet;
import io.zasupitts.petal.web.PetParams;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public abstract class PetService {
    public abstract Map<String, Set<String>> getPetsWithinRadius(String miles, String zipcode);

    public abstract PetOrg getOrg(int orgId);

    public abstract Pet getPet(String animalId);

    public abstract Set<Pet> getPets(Set<String> petIds);

    public Set<String> getDogBreedsForRadius(PetParams params) {
        Map<String, Set<String>> petsWithinRadius = getPetsWithinRadius(params.getRange(), params.getZipCode());
        Set<String> retval = new TreeSet<>();
        retval.addAll(petsWithinRadius.keySet());
        return retval;
    }

}
