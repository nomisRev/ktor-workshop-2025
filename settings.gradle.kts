rootProject.name = "ktor-workshop-2025"

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap") {
            mavenContent {
                includeGroupAndSubgroups("io.ktor")
            }
        }
        mavenCentral()
    }
}

include(":backend")
include(":app")