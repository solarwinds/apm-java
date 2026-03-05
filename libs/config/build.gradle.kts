plugins {
  id("solarwinds.java-conventions")
}

description = "config"

dependencies {
  implementation(project(":libs:logging"))
  compileOnly("org.json:json")
  testImplementation("org.json:json")
}
