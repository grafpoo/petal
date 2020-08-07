package io.zasupitts.petal.domain;

import lombok.Data;

@Data
public class PetPicture {
    private String originalUrl;
    private String largeUrl;
    private String smallUrl;
    private String fullsizeUrl;
    private String thumbnailUrl;
    private String mediaID;
    private String mediaOrder;
    private String lastUpdated;
    private Metadata original;
    private Metadata large;
    private Metadata small;

    public static class Metadata {
        public Long fileSize;
        public Long resolutionX;
        public Long resolutionY;
        public String url;
    }
}