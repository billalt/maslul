package com.maslul.assets.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Geometry/Point fields travel over JSON as WKT, e.g. "POINT (34.78 32.08)" -
// jts-core ships WKT support for free, avoiding a GeoJSON dependency for one field type.
@Configuration
class GeometryJacksonConfig {

    @Bean
    fun geometryModule(): Module {
        val module = SimpleModule("GeometryModule")
        module.addSerializer(Geometry::class.java, GeometrySerializer())
        module.addDeserializer(Geometry::class.java, GeometryDeserializer(Geometry::class.java))
        module.addDeserializer(Point::class.java, GeometryDeserializer(Point::class.java))
        return module
    }
}

private class GeometrySerializer : StdSerializer<Geometry>(Geometry::class.java) {
    override fun serialize(value: Geometry, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(WKTWriter().write(value))
    }
}

private class GeometryDeserializer<T : Geometry>(type: Class<T>) : StdDeserializer<T>(type) {
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T =
        WKTReader().read(p.valueAsString) as T
}
