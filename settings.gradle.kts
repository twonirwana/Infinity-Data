rootProject.name = "Infinity-Data"
include("data")
include("data:api")
include("data:core")
include("app")
include("web")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
