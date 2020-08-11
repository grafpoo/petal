package io.zasupitts.petal.web;

import io.zasupitts.petal.domain.Pet;
import io.zasupitts.petal.domain.PetOrg;
import io.zasupitts.petal.service.PetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.Set;

@Controller
@Slf4j
public class PetController {

    @Autowired
    private PetService petService;

    @GetMapping("/petdisplay/{animalId}")
    public String getPet(@PathVariable String animalId, Model model) {
        Pet pet = petService.getPet(animalId);
        model.addAttribute("pet", pet);
        PetOrg cachedOrg = petService.getOrg(pet.getOrgID());
        model.addAttribute("org", cachedOrg);
        model.addAttribute("title", "This is "+pet.getName());
        return "pet";
    }

    @GetMapping("/petfind")
    public String setupFind(Model model) {
        model.addAttribute("petParams", new PetParams());
        return "formOne";
    }

    @PostMapping("/petfindOne")
    public String setupFind(PetParams params, Model model) {
        model.addAttribute("petParams", params);
        Set<String> breedList = petService.getDogBreedsForRadius(params);
        model.addAttribute("allDogBreeds", breedList);
        return "formTwo";
    }

    @PostMapping("/petfindTwo")
    public String processRegistration (
            PetParams params,
            Errors errors, Model model) throws Exception {
        Map<String, Set<String>> result = petService.getPetsWithinRadius(params.getRange(), params.getZipCode()); // should be cached
        Set<Pet> pets = petService.getPets(result.get(params.getDogBreed()));
        model.addAttribute("pets", pets);
        model.addAttribute("title", "Search for \""+
                params.getDogBreed()+"\" within "+params.getRange()+" miles from "+params.getZipCode());
        return "petsList";
    }
}
