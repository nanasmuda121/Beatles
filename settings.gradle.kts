pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases/") }
    }
}

rootProject.name = "Beatles"

include(":app")
include(":innertube")
include(":auravideo")
include(":kugou")
include(":lrclib")
include(":lastfm")
include(":betterlyrics")
include(":simpmusic")
include(":shazamkit")
include(":rush")
include(":paxsenix")
include(":musixmatch")
