package io.zasupitts.petal.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Breeds {
    private Metadata meta;

    @JsonProperty("data")
    private List<Breed> breeds;

    public static final class Metadata {
        int count;
        int countReturned;
        int pageReturned;
        int pages;
        int limit;
        String transactionId;
    }
    public static final class Breed {
        String id;
        @JsonUnwrapped(prefix="attributes.")
        String name;
    }
}
