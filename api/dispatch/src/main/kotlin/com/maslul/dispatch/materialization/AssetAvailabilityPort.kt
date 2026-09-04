package com.maslul.dispatch.materialization

import java.util.UUID

// dispatch is the calling module for asset data during materialization, so dispatch owns
// this interface; assets implements it (spec §4: modules talk through interfaces defined in
// the calling module, no cross-module entity imports). Body types cross this boundary as
// their enum name (String) rather than assets.entity.BodyType, for the same reason.
interface AssetAvailabilityPort {

    // False for a removed/inactive ServicePoint, or one that no longer exists.
    fun isServicePointActive(tenantId: UUID, servicePointId: UUID): Boolean

    // Body type names required by containers at this ServicePoint. Empty for a SEGMENT point
    // with no containers, or a point with none registered yet - spec §3, required_body_type
    // is mandatory per container, not per point.
    fun requiredBodyTypeNames(tenantId: UUID, servicePointId: UUID): Set<String>

    // The assigned vehicle's body type name, or null if the vehicle no longer exists.
    fun vehicleBodyTypeName(tenantId: UUID, vehicleId: UUID): String?
}
