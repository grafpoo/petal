package io.zasupitts.petal.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@Builder
public class Org {
    private int id;
    private String name;
    private String city;
    private String state;
    private String zip;
    private String url;
    private Double lat;
    private Double lon;
}
