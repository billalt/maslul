package com.maslul.app.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

// Convention: each feature module keeps its JPA entities under `<module>.entity` (spec §4).
class ModuleBoundaryArchTest {

    private val featureModules = listOf("identity", "assets", "dispatch", "sync", "telemetry", "reports")

    @Test
    fun `feature modules do not import each other's entities`() {
        val importedClasses = ClassFileImporter().importPackages("com.maslul")

        for (module in featureModules) {
            // com.maslul.app is the composition root (spec §4) - it wires and integration-tests
            // every module together, so it is exempt. The rule targets feature-to-feature imports.
            val rule: ArchRule = noClasses()
                .that().resideOutsideOfPackage("com.maslul.$module..")
                .and().resideOutsideOfPackage("com.maslul.app..")
                .should().dependOnClassesThat().resideInAPackage("com.maslul.$module.entity..")
                .because("modules must not import another module's entities directly - use its interface instead")

            rule.check(importedClasses)
        }
    }
}
