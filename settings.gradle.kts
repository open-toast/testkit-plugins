enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

plugins {
    id("org.ajoberstar.reckon.settings") version "2.0.0"
}

extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
    setDefaultInferredScope("patch")
    snapshots()
    setScopeCalc(calcScopeFromProp())
    setStageCalc(calcStageFromProp())
}

rootProject.name = "testkit-plugins"

include(
    ":jacoco-reflect", ":junit5", ":testkit-plugin", ":coverage-plugin", ":integration-tests"
)
