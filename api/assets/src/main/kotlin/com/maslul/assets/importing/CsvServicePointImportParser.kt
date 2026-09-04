package com.maslul.assets.importing

import com.maslul.assets.entity.BodyType
import com.maslul.assets.entity.ContainerType
import com.maslul.assets.entity.ServicePointType
import com.maslul.assets.entity.Side
import com.maslul.assets.entity.WasteStream
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.WKTReader
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.nio.charset.StandardCharsets

// One CSV row = one Container, with ServicePoint fields repeated on every row that shares
// the same service_point_ref. Grouping rows into a single persisted ServicePoint happens in
// the persistence layer, not here - the parser's only job is turning one spreadsheet row
// into one ImportRow, tolerating bad data in that row without touching any other row.
@Component
class CsvServicePointImportParser : ServicePointImportParser {

    override fun supports(format: ImportFormat): Boolean = format == ImportFormat.CSV

    override fun parse(input: InputStream): List<ImportRow> {
        val reader = InputStreamReader(stripUtf8Bom(input), StandardCharsets.UTF_8)
        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreSurroundingSpaces(true)
            .build()

        CSVParser.parse(reader, csvFormat).use { parser ->
            val missingColumns = REQUIRED_COLUMNS.filterNot { parser.headerNames.contains(it) }
            require(missingColumns.isEmpty()) {
                "CSV is missing required column(s): ${missingColumns.joinToString()}"
            }
            return parser.map { record -> parseRecord(record) }
        }
    }

    private fun parseRecord(record: CSVRecord): ImportRow {
        // recordNumber is 1-indexed and excludes the header record.
        val rowNumber = record.recordNumber.toInt()
        return try {
            val servicePoint = parseServicePoint(record)
            val containers = parseContainers(record)
            ImportRow(rowNumber, servicePoint, containers, parseError = null)
        } catch (e: RowParseException) {
            ImportRow(rowNumber, null, emptyList(), parseError = e.message)
        }
    }

    private fun parseServicePoint(record: CSVRecord): ImportedServicePoint {
        val address = record.field(COL_ADDRESS).ifBlank {
            throw RowParseException("$COL_ADDRESS is required")
        }
        val type = parseImportEnum<ServicePointType>(record.field(COL_POINT_TYPE), COL_POINT_TYPE)
        val geometry: Geometry
        val side: Side?
        when (type) {
            ServicePointType.POINT -> {
                geometry = parseLatLng(record)
                side = null
            }
            ServicePointType.SEGMENT -> {
                geometry = parseSegmentGeometry(record)
                side = parseImportEnum<Side>(record.field(COL_SIDE), COL_SIDE)
            }
        }
        return ImportedServicePoint(
            externalRef = record.field(COL_SERVICE_POINT_REF).ifBlank { null },
            name = record.field(COL_SERVICE_POINT_NAME).ifBlank { null },
            type = type,
            geometry = geometry,
            side = side,
            address = address,
        )
    }

    private fun parseLatLng(record: CSVRecord): Point {
        val lat = record.field(COL_LAT).toDoubleOrNull()
            ?: throw RowParseException("$COL_LAT is required and must be numeric for POINT rows")
        val lng = record.field(COL_LNG).toDoubleOrNull()
            ?: throw RowParseException("$COL_LNG is required and must be numeric for POINT rows")
        return geometryFactory.createPoint(Coordinate(lng, lat))
    }

    // wkt_geometry only ever comes from a GIS export, never hand-typed - so a parse failure
    // here is a real data problem worth reporting, not something to guess around.
    private fun parseSegmentGeometry(record: CSVRecord): LineString {
        val wkt = record.field(COL_WKT_GEOMETRY).ifBlank {
            throw RowParseException("$COL_WKT_GEOMETRY is required for SEGMENT rows")
        }
        val geometry = try {
            WKTReader(geometryFactory).read(wkt)
        } catch (e: org.locationtech.jts.io.ParseException) {
            throw RowParseException("$COL_WKT_GEOMETRY could not be parsed: ${e.message}")
        }
        return geometry as? LineString
            ?: throw RowParseException("$COL_WKT_GEOMETRY must be a LINESTRING for SEGMENT rows")
    }

    private fun parseContainers(record: CSVRecord): List<ImportedContainer> {
        val ref = record.field(COL_CONTAINER_REF).ifBlank { null }
        val wasteStreamRaw = record.field(COL_WASTE_STREAM)
        val volumeRaw = record.field(COL_VOLUME_LITERS)
        val containerTypeRaw = record.field(COL_CONTAINER_TYPE)
        val requiredBodyTypeRaw = record.field(COL_REQUIRED_BODY_TYPE)
        val accessNotes = record.field(COL_ACCESS_NOTES).ifBlank { null }

        // A row may describe a ServicePoint with no containers at all (e.g. a location
        // being pre-registered before its bins are). Only treat the row as malformed once
        // it commits to having a container by filling in at least one container field.
        val hasAnyContainerData = ref != null || wasteStreamRaw.isNotBlank() || volumeRaw.isNotBlank() ||
            containerTypeRaw.isNotBlank() || requiredBodyTypeRaw.isNotBlank() || accessNotes != null
        if (!hasAnyContainerData) return emptyList()

        val volume = volumeRaw.toIntOrNull()
            ?: throw RowParseException("$COL_VOLUME_LITERS must be a whole number")

        return listOf(
            ImportedContainer(
                externalRef = ref,
                wasteStream = parseImportEnum<WasteStream>(wasteStreamRaw, COL_WASTE_STREAM),
                volumeLiters = volume,
                containerType = parseImportEnum<ContainerType>(containerTypeRaw, COL_CONTAINER_TYPE),
                requiredBodyType = parseImportEnum<BodyType>(requiredBodyTypeRaw, COL_REQUIRED_BODY_TYPE),
                accessNotes = accessNotes,
            ),
        )
    }

    // record.get(name) throws when the column isn't in the header at all (wkt_geometry/side
    // are optional columns) or when a row is shorter than the header (a ragged spreadsheet
    // export) - both cases should read as blank, not crash the row.
    private fun CSVRecord.field(name: String): String = if (isMapped(name) && isSet(name)) get(name) else ""

    private fun stripUtf8Bom(input: InputStream): InputStream {
        val pushback = PushbackInputStream(input, UTF8_BOM.size)
        val head = ByteArray(UTF8_BOM.size)
        val bytesRead = pushback.read(head)
        if (bytesRead != UTF8_BOM.size || !head.contentEquals(UTF8_BOM)) {
            if (bytesRead > 0) pushback.unread(head, 0, bytesRead)
        }
        return pushback
    }

    companion object {
        private val geometryFactory = GeometryFactory()
        private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        const val COL_SERVICE_POINT_REF = "service_point_ref"
        const val COL_SERVICE_POINT_NAME = "service_point_name"
        const val COL_ADDRESS = "address"
        const val COL_LAT = "lat"
        const val COL_LNG = "lng"
        const val COL_WKT_GEOMETRY = "wkt_geometry"
        const val COL_SIDE = "side"
        const val COL_POINT_TYPE = "point_type"
        const val COL_CONTAINER_REF = "container_ref"
        const val COL_WASTE_STREAM = "waste_stream"
        const val COL_VOLUME_LITERS = "volume_liters"
        const val COL_CONTAINER_TYPE = "container_type"
        const val COL_REQUIRED_BODY_TYPE = "required_body_type"
        const val COL_ACCESS_NOTES = "access_notes"

        // wkt_geometry and side are conditionally required (SEGMENT rows only) rather than
        // structurally required - a POINT-only import legitimately has neither column.
        private val REQUIRED_COLUMNS = listOf(
            COL_SERVICE_POINT_REF, COL_ADDRESS, COL_LAT, COL_LNG, COL_POINT_TYPE,
            COL_CONTAINER_REF, COL_WASTE_STREAM, COL_VOLUME_LITERS, COL_CONTAINER_TYPE,
            COL_REQUIRED_BODY_TYPE, COL_ACCESS_NOTES,
        )
    }
}
