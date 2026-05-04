rootProject.name = "FlipVerse"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        google()
        mavenCentral()
    }
}
include(":dashboard")
include(":dashboard")
include(":dashboard")
include(":data")
include(":di")
include(":explore")
include(":feature:chat")

include(":feature:dashboard")
include(":feature:explore")
include(":feature:profile")
include(":feature:livebook")
include(":feature:profile")
include(":feature:notification")
include(":feature:profile")
include(":feature:userprofile")
include(":feature:auth")
include(":navigation")
include(":composeApp")
include(":shared")
