package br.com.catech.fire_shield_localidades_ms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnderecoDto(

        @JsonProperty("place_id")
        Long placeId,

        @JsonProperty("addresstype")
        String addressType,

        String name,

        @JsonProperty("display_name")
        String displayName,

        AddressDto address

) {
    public record AddressDto(

            // tipo de via dinâmico (road, railway, etc.) — mapeados individualmente
            String road,
            String railway,
            String suburb,
            String city,
            String town,

            @JsonProperty("city_district")
            String cityDistrict,

            String state,

            @JsonProperty("ISO3166-2-lvl4")
            String iso3166Lvl4,

            String region,
            String postcode,
            String country,

            @JsonProperty("country_code")
            String countryCode
    ) {}
}
