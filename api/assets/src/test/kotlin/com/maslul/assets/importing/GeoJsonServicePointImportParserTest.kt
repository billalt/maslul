package com.maslul.assets.importing

import com.maslul.assets.entity.BodyType
import com.maslul.assets.entity.ContainerType
import com.maslul.assets.entity.ServicePointType
import com.maslul.assets.entity.Side
import com.maslul.assets.entity.WasteStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class GeoJsonServicePointImportParserTest {

    private val parser = GeoJsonServicePointImportParser()

    @Test
    fun `parses a point feature with two containers`() {
        val geoJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "Point", "coordinates": [35.3, 32.7] },
                  "properties": {
                    "external_ref": "SP-1",
                    "name": "Old City Bin",
                    "address": "Rehov HaGalil 4",
                    "containers": [
                      {
                        "external_ref": "C-1",
                        "waste_stream": "GENERAL",
                        "volume_liters": 1100,
                        "container_type": "COMMUNAL",
                        "required_body_type": "REAR_LOADER",
                        "access_notes": "gate code 4471"
                      },
                      {
                        "waste_stream": "PACKAGING_ORANGE",
                        "volume_liters": 360,
                        "container_type": "WHEELIE",
                        "required_body_type": "REAR_LOADER"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows).hasSize(1)
        val row = rows[0]
        assertThat(row.parseError).isNull()
        val servicePoint = row.servicePoint!!
        assertThat(servicePoint.externalRef).isEqualTo("SP-1")
        assertThat(servicePoint.name).isEqualTo("Old City Bin")
        assertThat(servicePoint.type).isEqualTo(ServicePointType.POINT)
        assertThat(servicePoint.geometry.coordinate.x).isEqualTo(35.3)
        assertThat(servicePoint.geometry.coordinate.y).isEqualTo(32.7)
        assertThat(row.containers).hasSize(2)
        assertThat(row.containers[0].externalRef).isEqualTo("C-1")
        assertThat(row.containers[0].wasteStream).isEqualTo(WasteStream.GENERAL)
        assertThat(row.containers[0].volumeLiters).isEqualTo(1100)
        assertThat(row.containers[0].containerType).isEqualTo(ContainerType.COMMUNAL)
        assertThat(row.containers[0].requiredBodyType).isEqualTo(BodyType.REAR_LOADER)
        assertThat(row.containers[1].externalRef).isNull()
        assertThat(row.containers[1].wasteStream).isEqualTo(WasteStream.PACKAGING_ORANGE)
    }

    @Test
    fun `parses a linestring feature as a segment with side`() {
        val geoJson = """
            {
              "features": [
                {
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [[34.9, 32.8], [34.91, 32.81]]
                  },
                  "properties": {
                    "address": "Old City Alley",
                    "side": "LEFT",
                    "containers": []
                  }
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows).hasSize(1)
        val servicePoint = rows[0].servicePoint!!
        assertThat(servicePoint.type).isEqualTo(ServicePointType.SEGMENT)
        assertThat(servicePoint.side).isEqualTo(Side.LEFT)
        assertThat(servicePoint.geometry.geometryType).isEqualTo("LineString")
        assertThat(rows[0].containers).isEmpty()
    }

    @Test
    fun `a feature with no containers array produces a service point with no containers`() {
        val geoJson = """
            {
              "features": [
                {
                  "geometry": { "type": "Point", "coordinates": [34.9, 32.8] },
                  "properties": { "address": "Derech Yafo 12" }
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows[0].parseError).isNull()
        assertThat(rows[0].containers).isEmpty()
    }

    @Test
    fun `missing address is reported on the feature, not thrown`() {
        val geoJson = """
            {
              "features": [
                {
                  "geometry": { "type": "Point", "coordinates": [34.9, 32.8] },
                  "properties": {}
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("address")
    }

    @Test
    fun `linestring without side is reported on the feature, not thrown`() {
        val geoJson = """
            {
              "features": [
                {
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [[34.9, 32.8], [34.91, 32.81]]
                  },
                  "properties": { "address": "Old City Alley" }
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("side")
    }

    @Test
    fun `unsupported geometry type is reported on the feature, not thrown`() {
        val geoJson = """
            {
              "features": [
                {
                  "geometry": { "type": "Polygon", "coordinates": [[[34.9, 32.8]]] },
                  "properties": { "address": "Somewhere" }
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("Polygon")
    }

    @Test
    fun `a bad field type on one feature does not affect other features`() {
        val geoJson = """
            {
              "features": [
                {
                  "geometry": { "type": "Point", "coordinates": [34.9, 32.8] },
                  "properties": {
                    "address": "Rehov Herzl 1",
                    "containers": [ { "volume_liters": "not-a-number", "waste_stream": "GENERAL",
                      "container_type": "WHEELIE", "required_body_type": "REAR_LOADER" } ]
                  }
                },
                {
                  "geometry": { "type": "Point", "coordinates": [34.8, 32.9] },
                  "properties": { "address": "Rehov Allenby 9" }
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows).hasSize(2)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).isNotNull()
        assertThat(rows[1].servicePoint).isNotNull
    }

    @Test
    fun `hebrew and arabic addresses round-trip untouched`() {
        val geoJson = """
            {
              "features": [
                {
                  "geometry": { "type": "Point", "coordinates": [35.3, 32.7] },
                  "properties": { "address": "شارع الجليل 4، الناصرة" }
                }
              ]
            }
        """.trimIndent()

        val rows = parser.parse(geoJson.toStream())

        assertThat(rows[0].servicePoint!!.address).isEqualTo("شارع الجليل 4، الناصرة")
    }

    @Test
    fun `missing features array fails the whole file, not a single feature`() {
        val geoJson = """{ "type": "FeatureCollection" }"""

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            parser.parse(geoJson.toStream())
        }
    }

    private fun String.toStream() = ByteArrayInputStream(this.toByteArray(StandardCharsets.UTF_8))
}
