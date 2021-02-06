## Purpose
The purpose of this project is to provide a reusable template showcasing how to execute Karate and Gatling utilizing both Maven and Gradle build systems.  Additionally how to execute across varying environments is demonstrated.  Eg.  In 'dev' we run without a web server, use mocks and obtain code coverage.  In 'stg' or staging we run without mocks and use a live web server.

## Setup
Install Java (tested with Java(TM) SE Runtime Environment 18.9 (build 11.0.8+10-LTS))

## Execution

### Functionally test without web server (with mock, with code coverage)
```bash
mvn clean test
gradlew clean test
```

### Functionally test with local web server (without mock, without code coverage)
```bash
mvn clean spring-boot:run
mvn test -Dkarate.env=stg

gradlew clean bootrun
gradlew test -Dkarate.env=stg
```

### Performance test with local web server
```bash
mvn clean spring-boot:run
mvn test-compile gatling:test -Dperf=load -Dgatling.simulationClass=org.company.Order -Dkarate.env=stg
mvn test-compile gatling:test -Dperf=stress -Dgatling.simulationClass=org.company.Order -Dkarate.env=stg
mvn test-compile gatling:test -Dperf=soak -Dgatling.simulationClass=org.company.Order -Dkarate.env=stg
mvn test-compile gatling:test -Dperf=spike -Dgatling.simulationClass=org.company.Order -Dkarate.env=stg

gradlew clean bootrun
gradlew gatlingRun -Dperf=load -Dsimulation=org.company.Order -Dkarate.env=stg
gradlew gatlingRun -Dperf=stress -Dsimulation=org.company.Order -Dkarate.env=stg
gradlew gatlingRun -Dperf=soak -Dsimulation=org.company.Order -Dkarate.env=stg
gradlew gatlingRun -Dperf=spike -Dsimulation=org.company.Order -Dkarate.env=stg

# optional if gatling fails to create html report from simulation.log
gradlew gatlingReport -DsimulationFolder=<folder path to **/gatling/[simulationFileName-YYYYMMDDHHMMSSmmm]>
```

## View Reports
```bash
# report portal
# this project was last tested with report portal version 5.3.3
#
# to enable:
# - install report portal from https://reportportal.io/
# - update /src/test/resources/reportportal.properties
# - specify jvm arg "-Dreportportal=true" in above commands

# karate report showing feature/scenario/step detail
**/karate-reports/karate-summary.html

# karate report showing execution of scenarios across threads and time
**/karate-reports/karate-timeline.html

# java code coverage report
**/jacoco/test/html/index.html
**/jacoco/index.html

# gatling performance test report
**/gatling/[simulationFileName-YYYYMMDDHHMMSSmmm]/index.html
```