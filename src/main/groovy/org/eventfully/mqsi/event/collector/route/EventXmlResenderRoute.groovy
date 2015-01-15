package org.eventfully.mqsi.event.collector.route

import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.Message
import org.apache.camel.builder.RouteBuilder
import org.eventfully.mqsi.event.collector.component.MqsiEventParser
import org.eventfully.mqsi.event.collector.component.RFHUtilHelper
import org.eventfully.mqsi.event.collector.component.StaticMQSender
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "xmlResenderRoute")
class EventXmlResenderRoute extends RouteBuilder {

    @Autowired
    RFHUtilHelper rfhUtilHelper

    @Autowired
    StaticMQSender mqSender

    @Override
    public void configure() throws Exception {

        from("{{xmlResenderRoute.from}}").routeId("{{xmlResenderRoute.id}}")
                .autoStartup("{{xmlResenderRoute.enabled}}")
                .log(LoggingLevel.INFO, "Resend request received")
                .convertBodyTo(String.class, "UTF-8")
                .process { Exchange ex ->

            Message message = ex.in
            message.body = MqsiEventParser.extractBitstreamPayload(message.body)

        }
        .bean(rfhUtilHelper, "extract")
                .bean(mqSender, "resend")
                .log(LoggingLevel.INFO, "Message resent")

    }
}
