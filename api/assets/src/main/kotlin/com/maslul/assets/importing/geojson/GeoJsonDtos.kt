package com.maslul.assets.importing.geojson

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

// Mirrors only the shape this importer reads - properties keys match the CSV import's
// column names (see CsvServicePointImportParser) so the two formats describe the same
// entity fields under the same names.
@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoJsonFeatureCollectionDto(
    val features: List<GeoJsonFeatureDto> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoJsonFeatureDto(
    val geometry: GeoJsonGeometryDto? = null,
    val properties: GeoJsonPropertiesDto? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoJsonGeometryDto(
    val type: String? = null,
    val coordinates: JsonNode? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoJsonPropertiesDto(
    @JsonProperty("external_ref") val externalRef: String? = null,
    val name: String? = null,
    val address: String? = null,
    // Only meaningful when geometry.type is LineString - see spec §3 (ServicePoint side).
    val side: String? = null,
    val containers: List<GeoJsonContainerDto> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeoJsonContainerDto(
    @JsonProperty("external_ref") val externalRef: String? = null,
    @JsonProperty("waste_stream") val wasteStream: String? = null,
    @JsonProperty("volume_liters") val volumeLiters: Int? = null,
    @JsonProperty("container_type") val containerType: String? = null,
    @JsonProperty("required_body_type") val requiredBodyType: String? = null,
    @JsonProperty("access_notes") val accessNotes: String? = null,
)
