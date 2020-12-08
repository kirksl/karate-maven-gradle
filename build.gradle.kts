import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:1.5.22.RELEASE")
    }
}

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/epam/reportportal") }
}

plugins {
    java
    scala
    jacoco
    application
    id("org.springframework.boot") version "1.5.22.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    //kotlin("jvm") version "1.3.72"
    //kotlin("plugin.spring") version "1.3.72"
}

application {
    mainClassName = "org.company.OrderApplication"
}

group = group
version = version

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.jar {
    val artifactId: String by project
    baseName = "$artifactId"
}

val gatling by configurations.creating

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:1.5.22.RELEASE")
    implementation("com.epam.reportportal:commons-model:5.0.0")
    implementation("org.scala-lang:scala-library:2.12.8")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-access:1.2.3")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.7")
    implementation("net.logstash.logback:logstash-logback-encoder:4.9")
    implementation("org.slf4j:slf4j-api:1.7.18")

    testImplementation("junit:junit:4.12")
    testImplementation("org.springframework.boot:spring-boot-starter-test") { exclude(group = "org.junit.vintage", module = "junit-vintage-engine") }
    testImplementation("com.epam.reportportal:logger-java-logback:5.0.1")
    testImplementation("com.intuit.karate:karate-apache:0.9.6")
    testImplementation("com.intuit.karate:karate-mock-servlet:0.9.6")
    testImplementation("com.intuit.karate:karate-junit5:0.9.6")
    testImplementation("com.intuit.karate:karate-gatling:0.9.6")
    testImplementation("net.masterthought:cucumber-reporting:4.11.2")

    gatling("org.scala-lang:scala-library:2.12.8")
    gatling("io.gatling:gatling-app:3.0.2")
    gatling("io.gatling.highcharts:gatling-charts-highcharts:3.0.2")
    gatling("com.intuit.karate:karate-gatling:0.9.6")

    // Align versions of all Kotlin components, JDK8, Test Library, JUnit
    //implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    //implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    //testImplementation("org.jetbrains.kotlin:kotlin-test")
    //testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.bootRun {
	systemProperties(System.getProperties().mapKeys { it.key as String })
}

sourceSets.getByName("test") {
    resources.srcDir("src/test/resources")
    resources.srcDir("src/test/java")
    resources.srcDir("src/test/scala")
}

tasks.withType<Test> {
    if(!project.gradle.startParameter.taskNames.contains("gatlingRun")) {
        gradle.startParameter.excludedTaskNames += ":compileTestScala"
    }

    useJUnitPlatform()

    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
        events = mutableSetOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }

    systemProperties(System.getProperties().mapKeys { it.key as String })
    systemProperty("karate.options", System.getProperty("karate.options"))
    systemProperty("karate.env", System.getProperty("karate.env"))

    outputs.upToDateWhen { false }

    finalizedBy("jacocoTestReport")
}

tasks.register<JavaExec>("gatlingRun") {
    dependsOn("testClasses")

    description = "Run Gatling Tests"

    val gatling = File("${buildDir}/reports/gatling")
    gatling.mkdirs()

    classpath = sourceSets.test.get().runtimeClasspath

    main = "io.gatling.app.Gatling"

    val simulation = System.getProperty("simulation")
    args = mutableListOf<String>("-s", "$simulation", "-rf", "${buildDir}/reports/gatling")

    systemProperties(System.getProperties().mapKeys { it.key as String })
}

tasks.register<JavaExec>("gatlingReport") {
    dependsOn("testClasses")

    description = "Run Gatling Report"

    classpath = sourceSets.test.get().runtimeClasspath

    main = "io.gatling.app.Gatling"

    val simulationFolder = System.getProperty("simulationFolder")
    args = mutableListOf<String>("-ro", "$simulationFolder")
    
    systemProperties(System.getProperties().mapKeys { it.key as String })
}

tasks.register<JavaExec>("karateExecute") {
    classpath = sourceSets.test.get().runtimeClasspath
    main = System.getProperty("mainClass")
}

tasks.withType<ScalaCompile>().configureEach {
    scalaCompileOptions.apply {
        targetCompatibility = "1.8"
    }
}