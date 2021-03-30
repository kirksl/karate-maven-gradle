package org.company

import java.lang.Math._
import java.util.Map
import com.intuit.karate.Runner._
import com.intuit.karate.gatling.PreDef._
import io.gatling.core.Predef._
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.core.controller.inject.closed.ClosedInjectionStep
import scala.Enumeration
import scala.concurrent.duration._
import scala.collection.JavaConverters._


class Order extends BaseSimulation {
    val protocol = karateProtocol(
        "/order" -> Nil
    )

    val action = karateFeature("classpath:org/company/order.feature@test=order3")

    setUp(
        scenario("get-order")
            .exec(action)
            .inject(super.buildOpenModel())
            .protocols(protocol)
    )
}