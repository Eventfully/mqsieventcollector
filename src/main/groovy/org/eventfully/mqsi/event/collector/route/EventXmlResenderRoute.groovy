package org.eventfully.mqsi.event.collector.route

import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.eventfully.mqsi.event.collector.component.RFHUtilHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "xmlResenderRoute")
class EventXmlResenderRoute extends RouteBuilder {

    @Autowired
    RFHUtilHelper rfhUtilHelper


    @Override
    public void configure() throws Exception {

        from("{{xmlResenderRoute.from}}").routeId("{{xmlResenderRoute.id}}")
                .autoStartup("{{xmlResenderRoute.enabled}}")
                .log(LoggingLevel.INFO, "Resend request received")
        //      .bean(rfhUtilHelper, "extract")
        //    .bean(simpleMQSender, "resend")
        //  .log(LoggingLevel.INFO, "Message resent")

    }
}
