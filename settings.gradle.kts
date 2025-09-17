pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      name = "savaGithubPackages"
      url = uri("https://maven.pkg.github.com/sava-software/sava-build")
      credentials(PasswordCredentials::class)
    }
  }
}

plugins {
  id("software.sava.build") version "0.2.2"
}

rootProject.name = "ix-proxy"

javaModules {
  directory(".") {
    group = "systems.glam"
    plugin("software.sava.build.java-module")
  }
}
