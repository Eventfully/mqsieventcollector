package org.eventfully.mqsi.event.collector.route

import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.eventfully.mqsi.event.collector.component.RFHUtilHelper
import org.eventfully.mqsi.event.collector.component.StaticMQSender
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "rfhResenderRoute")
class EventPayloadResenderRoute extends RouteBuilder {

    @Autowired
    RFHUtilHelper rfhUtilHelper

    @Autowired
    StaticMQSender mqSender

    @Override
    public void configure() throws Exception {

        from("{{rfhResenderRoute.from}}").routeId("{{rfhResenderRoute.id}}")
                .autoStartup("{{rfhResenderRoute.enabled}}")
                .log(LoggingLevel.INFO, "Resend request received")
                .bean(rfhUtilHelper, "extract")
                .bean(mqSender, "resend")
                .log(LoggingLevel.INFO, "Message resent")

    }
}
