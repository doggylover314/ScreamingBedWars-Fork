plugins {
    alias(libs.plugins.buildconfig)
}

dependencies {
    /* PROVIDED */
    compileOnly(libs.multiverse)
    compileOnly(libs.multiverse5.core)

    /* SHADED */
    api("org.screamingsandals.language.bedwars:BedWarsLanguage:${Regex("^\\d+\\.\\d+").find(project.version.toString())?.value}-SNAPSHOT")
    implementation(libs.hikari)
    implementation(libs.mclogs.api) {
        exclude(group="*", module="*")
    }

    api(libs.configurate.gson) {
        exclude(group="*", module="*")
    }
    api(libs.configurate.yaml)
    api(project(":BedWars-protocol"))

    /* TEST */
    // configurate-gson is declared with exclude(group="*", module="*") because servers provide Gson;
    // tests that load JSON (language overlay) need it explicitly.
    testRuntimeOnly(libs.gson)
}

buildConfig {
    className("VersionInfo")
    packageName("org.screamingsandals.bedwars")

    buildConfigField("String", "NAME", "\"${project.name}\"")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "BUILD_NUMBER", "\"${System.getenv("BUILD_NUMBER") ?: "custom"}\"")
}