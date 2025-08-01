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
  id("software.sava.build") version "0.1.27"
}

rootProject.name = "ix-proxy"

javaModules {
  directory(".") {
    group = "systems.glam"
    plugin("software.sava.build.java-module")
  }
}
