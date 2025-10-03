rootProject.name = "Infinity-Data"
include("data")
include("data:api")
include("data:core")
include("app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}