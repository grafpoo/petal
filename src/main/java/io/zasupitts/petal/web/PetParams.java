package io.zasupitts.petal.web;

import lombok.Data;

@Data
public class PetParams {
    private String petType;
    private String zipCode;
    private String range;
    private String dogBreed;
    private String[] dogBreeds;
}
