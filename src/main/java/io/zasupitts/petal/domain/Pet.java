package io.zasupitts.petal.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pet implements Comparable {
    private int orgID;
    private String animalID;
    private String status;
    private String lastUpdated;
    private String rescueID;
    private String name;
    private String summary;
    private String species;
    private String breed;
    private String primaryBreed;
    private String secondaryBreed;
    private String sex;
    private String mixed;
    private String dogs;
    private String cats;
    private String kids;
    private String declawed;
    private String housetrained;
    private String age;
    private String birthdate;
    private String specialNeeds;
    private String altered;
    private String size;
    private String sizeCurrent;
    private String sizePotential;
    private String sizeUOM;
    private String uptodate;
    private String color;
    private String coatLength;
    private String pattern;
    private String courtesy;
    private String found;
    private String foundDate;
    private String foundZipcode;
    private String animalLocation;
    private String description;
    private String descriptionPlain;
    private String trackerImageUrl;
    private String adoptionFee;
    private String adoptionPending;
    private String oKWithAdults;
    private String obedienceTraining;
    private String ownerExperience;
    private String exerciseNeeds;
    private String energyLevel;
    private String groomingNeeds;
    private String yardRequired;
    private String fence;
    private String shedding;
    private String newPeople;
    private String vocal;
    private String activityLevel;
    private String earType;
    private String eyeColor;
    private String tailType;
    private String olderKidsOnly;
    private String noSmallDogs;
    private String noLargeDogs;
    private String noFemaleDogs;
    private String noMaleDogs;
    private String oKForSeniors;
    private String hypoallergenic;
    private String goodInCar;
    private String leashtrained;
    private String cratetrained;
    private String fetches;
    private String playsToys;
    private String swims;
    private String lap;
    private String oKWithFarmAnimals;
    private String drools;
    private String apartment;
    private String noHeat;
    private String noCold;
    private String protective;
    private String escapes;
    private String predatory;
    private String hasAllergies;
    private String specialDiet;
    private String ongoingMedical;
    private String hearingImpaired;
    private String sightImpaired;
    private String obedient;
    private String playful;
    private String timid;
    private String skittish;
    private String independent;
    private String affectionate;
    private String eagerToPlease;
    private String intelligent;
    private String eventempered;
    private String gentle;
    private String goofy;
    private List<PetPicture> pictures;
    private List<Map> videos;
    private List<Map> videoUrls;
    private String mediaLastUpdated;
    private String contactName;
    private String contactEmail;
    private String contactCellPhone;
    private String contactHomePhone;
    private String petUrl;
    private String messagePet;
    private String needsFoster;
    private List<String> photos = new ArrayList<>();

    @Override
    public int compareTo(Object o) {
        return this.animalID.compareTo(((Pet)o).animalID);
    }

    public void addPhotoUrl(String photoUrl) {
        photos.add(photoUrl);
    }
}
