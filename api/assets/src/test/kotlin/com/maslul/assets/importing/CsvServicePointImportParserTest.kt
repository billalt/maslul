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

class CsvServicePointImportParserTest {

    private val parser = CsvServicePointImportParser()

    private val header = "service_point_ref,service_point_name,address,lat,lng,point_type," +
        "container_ref,waste_stream,volume_liters,container_type,required_body_type,access_notes," +
        "wkt_geometry,side"

    @Test
    fun `parses a valid point row with a container`() {
        val csv = header + "\n" +
            "SP-1,Old City Bin,Rehov HaGalil 4,32.7,35.3,POINT," +
            "C-1,GENERAL,1100,COMMUNAL,REAR_LOADER,gate code 4471"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        val row = rows[0]
        assertThat(row.parseError).isNull()
        assertThat(row.servicePoint).isNotNull
        assertThat(row.servicePoint!!.externalRef).isEqualTo("SP-1")
        assertThat(row.servicePoint!!.name).isEqualTo("Old City Bin")
        assertThat(row.servicePoint!!.type).isEqualTo(ServicePointType.POINT)
        assertThat(row.servicePoint!!.address).isEqualTo("Rehov HaGalil 4")
        assertThat(row.servicePoint!!.geometry.coordinate.x).isEqualTo(35.3)
        assertThat(row.servicePoint!!.geometry.coordinate.y).isEqualTo(32.7)
        assertThat(row.containers).hasSize(1)
        assertThat(row.containers[0].externalRef).isEqualTo("C-1")
        assertThat(row.containers[0].wasteStream).isEqualTo(WasteStream.GENERAL)
        assertThat(row.containers[0].volumeLiters).isEqualTo(1100)
        assertThat(row.containers[0].containerType).isEqualTo(ContainerType.COMMUNAL)
        assertThat(row.containers[0].requiredBodyType).isEqualTo(BodyType.REAR_LOADER)
        assertThat(row.containers[0].accessNotes).isEqualTo("gate code 4471")
    }

    @Test
    fun `a row with no container fields filled in produces a service point with no containers`() {
        val csv = header + "\n" + "SP-2,,Derech Yafo 12,32.8,34.9,POINT,,,,,"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].parseError).isNull()
        assertThat(rows[0].servicePoint).isNotNull
        assertThat(rows[0].containers).isEmpty()
    }

    @Test
    fun `blank address is reported on the row, not thrown`() {
        val csv = header + "\n" + "SP-3,,,32.8,34.9,POINT,,,,,"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("address")
    }

    @Test
    fun `non-numeric lat is reported on the row, not thrown`() {
        val csv = header + "\n" + "SP-4,,Rehov Herzl 1,not-a-number,34.9,POINT,,,,,"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("lat")
    }

    @Test
    fun `unknown enum value is reported on the row, not thrown`() {
        val csv = header + "\n" +
            "SP-5,,Rehov Herzl 1,32.8,34.9,POINT,C-1,PLUTONIUM,1100,COMMUNAL,REAR_LOADER,"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("waste_stream")
    }

    @Test
    fun `parses a segment row from wkt_geometry and side`() {
        val csv = header + "\n" +
            "SP-6,Old City Alley,Old City Alley,,,SEGMENT,,,,,,,\"LINESTRING (34.9 32.8, 34.91 32.81)\",LEFT"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        val servicePoint = rows[0].servicePoint
        assertThat(rows[0].parseError).isNull()
        assertThat(servicePoint).isNotNull
        assertThat(servicePoint!!.type).isEqualTo(ServicePointType.SEGMENT)
        assertThat(servicePoint.side).isEqualTo(Side.LEFT)
        assertThat(servicePoint.geometry.geometryType).isEqualTo("LineString")
    }

    @Test
    fun `segment row missing wkt_geometry is reported on the row, not thrown`() {
        val csv = header + "\n" + "SP-7,,Old City Alley,,,SEGMENT,,,,,,,,LEFT"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("wkt_geometry")
    }

    @Test
    fun `segment row missing side is reported on the row, not thrown`() {
        val csv = header + "\n" +
            "SP-8,,Old City Alley,,,SEGMENT,,,,,,,\"LINESTRING (34.9 32.8, 34.91 32.81)\","

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("side")
    }

    @Test
    fun `segment row with a point wkt geometry is reported on the row`() {
        val csv = header + "\n" +
            "SP-9,,Old City Alley,,,SEGMENT,,,,,,,\"POINT (34.9 32.8)\",LEFT"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(1)
        assertThat(rows[0].servicePoint).isNull()
        assertThat(rows[0].parseError).contains("LINESTRING")
    }

    @Test
    fun `one malformed row does not affect other rows in the same file`() {
        val csv = header + "\n" +
            "SP-7,,Rehov Herzl 1,32.8,34.9,POINT,,,,,\n" +
            "SP-8,,,32.8,34.9,POINT,,,,,\n" +
            "SP-9,,Rehov Allenby 9,32.9,34.8,POINT,,,,,"

        val rows = parser.parse(csv.toStream())

        assertThat(rows).hasSize(3)
        assertThat(rows[0].servicePoint).isNotNull
        assertThat(rows[1].servicePoint).isNull()
        assertThat(rows[2].servicePoint).isNotNull
    }

    @Test
    fun `strips a leading UTF-8 BOM`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val csv = header + "\n" + "SP-10,,Rehov Herzl 1,32.8,34.9,POINT,,,,,"
        val bytes = bom + csv.toByteArray(StandardCharsets.UTF_8)

        val rows = parser.parse(ByteArrayInputStream(bytes))

        assertThat(rows).hasSize(1)
        assertThat(rows[0].parseError).isNull()
        assertThat(rows[0].servicePoint!!.externalRef).isEqualTo("SP-10")
    }

    @Test
    fun `hebrew and arabic addresses round-trip untouched`() {
        val csv = header + "\n" + "SP-11,,\"רחוב הגליל 4, נצרת\",32.7,35.3,POINT,,,,,"

        val rows = parser.parse(csv.toStream())

        assertThat(rows[0].servicePoint!!.address).isEqualTo("רחוב הגליל 4, נצרת")
    }

    @Test
    fun `missing required column fails the whole file, not a single row`() {
        // point_type dropped entirely - a structural problem, unlike a single bad cell.
        val brokenHeader = "service_point_ref,service_point_name,address,lat,lng," +
            "container_ref,waste_stream,volume_liters,container_type,required_body_type,access_notes"
        val csv = brokenHeader + "\n" + "SP-12,,Rehov Herzl 1,32.8,34.9,,,,,"

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            parser.parse(csv.toStream())
        }
    }

    private fun String.toStream() = ByteArrayInputStream(this.toByteArray(StandardCharsets.UTF_8))
}
