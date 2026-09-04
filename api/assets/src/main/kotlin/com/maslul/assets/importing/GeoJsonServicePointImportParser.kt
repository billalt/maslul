package com.maslul.assets.importing

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.maslul.assets.entity.BodyType
import com.maslul.assets.entity.ContainerType
import com.maslul.assets.entity.ServicePointType
import com.maslul.assets.entity.Side
import com.maslul.assets.entity.WasteStream
import com.maslul.assets.importing.geojson.GeoJsonContainerDto
import com.maslul.assets.importing.geojson.GeoJsonFeatureDto
import com.maslul.assets.importing.geojson.GeoJsonGeometryDto
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.springframework.stereotype.Component
import java.io.InputStream

// One Feature = one ServicePoint; its containers travel together as properties.containers
// (unlike CSV, where each row is exactly one container). properties keys match the CSV
// import's column names - see CsvServicePointImportParser and GeoJsonDtos.
//
// Each feature is deserialized individually, inside the per-feature try/catch, rather than
// binding the whole document in one Jackson call - otherwise a type mismatch on one feature
// (e.g. volume_liters sent as an object) would throw for the entire file instead of just
// that row, breaking the same "report it, don't fail the batch" contract CSV import has.
@Component
class GeoJsonServicePointImportParser : ServicePointImportParser {

    override fun supports(format: ImportFormat): Boolean = format == ImportFormat.GEOJSON

    override fun parse(input: InputStream): List<ImportRow> {
        val root = objectMapper.readTree(input)
        val featuresNode = root.get("features")
        require(featuresNode != null && featuresNode.isArray) {
            "GeoJSON must be a FeatureCollection with a 'features' array"
        }
        return featuresNode.mapIndexed { index, node -> parseFeatureNode(index + 1, node) }
    }

    private fun parseFeatureNode(rowNumber: Int, node: JsonNode): ImportRow =
        try {
            val feature = objectMapper.treeToValue(node, GeoJsonFeatureDto::class.java)
            val servicePoint = parseServicePoint(feature)
            val containers = parseContainers(feature.properties?.containers ?: emptyList())
            ImportRow(rowNumber, servicePoint, containers, parseError = null)
        } catch (e: RowParseException) {
            ImportRow(rowNumber, null, emptyList(), parseError = e.message)
        } catch (e: JsonProcessingException) {
            ImportRow(rowNumber, null, emptyList(), parseError = "could not parse feature: ${e.originalMessage}")
        }

    private fun parseServicePoint(feature: GeoJsonFeatureDto): ImportedServicePoint {
        val properties = feature.properties ?: throw RowParseException("properties is required")
        val address = properties.address?.ifBlank { null }
            ?: throw RowParseException("address is required")
        val geometry = toGeometry(feature.geometry)
        val type = when (geometry) {
            is Point -> ServicePointType.POINT
            is LineString -> ServicePointType.SEGMENT
            else -> throw RowParseException("geometry must be Point or LineString")
        }
        val side = if (type == ServicePointType.SEGMENT) {
            parseImportEnum<Side>(properties.side.orEmpty(), "side")
        } else {
            null
        }
        return ImportedServicePoint(
            externalRef = properties.externalRef?.ifBlank { null },
            name = properties.name?.ifBlank { null },
            type = type,
            geometry = geometry,
            side = side,
            address = address,
        )
    }

    private fun parseContainers(containers: List<GeoJsonContainerDto>): List<ImportedContainer> =
        containers.map { container ->
            ImportedContainer(
                externalRef = container.externalRef?.ifBlank { null },
                wasteStream = parseImportEnum<WasteStream>(container.wasteStream.orEmpty(), "waste_stream"),
                volumeLiters = container.volumeLiters
                    ?: throw RowParseException("volume_liters is required for every container"),
                containerType = parseImportEnum<ContainerType>(container.containerType.orEmpty(), "container_type"),
                requiredBodyType = parseImportEnum<BodyType>(
                    container.requiredBodyType.orEmpty(),
                    "required_body_type",
                ),
                accessNotes = container.accessNotes?.ifBlank { null },
            )
        }

    private fun toGeometry(geometry: GeoJsonGeometryDto?): Geometry {
        val type = geometry?.type ?: throw RowParseException("geometry is required")
        val coordinates = geometry.coordinates ?: throw RowParseException("geometry.coordinates is required")
        return when (type) {
            "Point" -> geometryFactory.createPoint(toCoordinate(coordinates))
            "LineString" -> {
                if (coordinates.size() < 2) {
                    throw RowParseException("LineString geometry needs at least 2 coordinates")
                }
                geometryFactory.createLineString(coordinates.map { toCoordinate(it) }.toTypedArray())
            }
            else -> throw RowParseException("unsupported geometry type '$type' - expected Point or LineString")
        }
    }

    private fun toCoordinate(node: JsonNode): Coordinate {
        if (!node.isArray || node.size() < 2) {
            throw RowParseException("invalid coordinate: $node")
        }
        return Coordinate(node[0].asDouble(), node[1].asDouble())
    }

    companion object {
        private val geometryFactory = GeometryFactory()
        private val objectMapper = jacksonObjectMapper()
    }
}
