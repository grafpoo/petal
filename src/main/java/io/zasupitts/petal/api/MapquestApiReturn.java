package io.zasupitts.petal.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapquestApiReturn {
    List<MapquestResult> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MapquestResult {
        List<MapquestLocation> locations;
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MapquestLocation {
        MapquestLatLong latLng;
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MapquestLatLong {
        Double lat;
        Double lng;
    }
}
