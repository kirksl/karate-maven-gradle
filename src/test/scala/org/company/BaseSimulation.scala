package org.company

import java.lang.Math._
import java.util.Map
import com.intuit.karate.Runner._
import com.intuit.karate.gatling.PreDef._
import io.gatling.core.Predef._
import io.gatling.core.controller.inject.InjectionProfile
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.core.controller.inject.closed.ClosedInjectionStep
import scala.Enumeration
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayDeque
import scala.language.postfixOps

// testing
// for expected load ensure operation within spec (traffic SLA, resource usage, etc)
// for unexpected load measure degradation of artifacts

// model types
// open   => guarantees arrival rate of users, not concurrent users in system
// closed => guarantees concurrent users in system, not arrival rate of users

// perf types
// load   => linear ramp to expected max load and run at max load for minutes/hours
// stress => staircase ramp to unexpected load to find performance limits
// soak   => linear ramp to expected normal load and run at normal load for 24hrs+
// spike  => one-shot to unexpected load and done to monitor system recovery

class BaseSimulation extends Simulation {
    before {
    }
    
    after {
    }

    object Perf extends Enumeration {
        val Load = Value("load")
        val Stress = Value("stress")
        val Soak = Value("soak")
        val Spike = Value("spike")
    }

    def perHour(rate : Double): Double = rate / 3600
    def perMin(rate : Double): Double = rate / 60

    def buildOpenModel(perf: String = System.getProperty("perf")) = {
        var model = ArrayDeque[OpenInjectionStep]()

        Perf.withName(perf) match {
            case Perf.Load =>
                model += (rampUsersPerSec(1) to (5) during (5 seconds))
                model += (constantUsersPerSec(5) during (10 seconds))
            case Perf.Stress =>
                ((1 to 5).map(i => constantUsersPerSec(i) during (5 seconds)))
            case Perf.Soak =>
                model += (rampUsersPerSec(1) to (5) during (10 seconds))
                model += (constantUsersPerSec(5) during (100 seconds))
            case Perf.Spike =>
                model += (rampUsersPerSec(1) to (5) during (5 seconds))
                model += (atOnceUsers(100))
        }
    }

    def buildClosedModel(perf: String = System.getProperty("perf")) = {
        var model = ArrayDeque[ClosedInjectionStep]()

        Perf.withName(perf) match {
            case Perf.Load =>
                model += (rampConcurrentUsers(1) to (5) during (5 seconds))
                model += (constantConcurrentUsers(5) during (10 seconds))
            case Perf.Stress =>
                ((1 to 5).map(i => constantConcurrentUsers(i) during (5 seconds)))
            case Perf.Soak =>
                model += (rampConcurrentUsers(1) to (5) during (10 seconds))
                model += (constantConcurrentUsers(5) during (100 seconds))
        }
    }
}