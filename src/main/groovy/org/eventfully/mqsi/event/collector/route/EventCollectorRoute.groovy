package org.eventfully.mqsi.event.collector.route

import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.Message
import org.apache.camel.builder.RouteBuilder
import org.eventfully.mqsi.event.collector.component.MqsiEventParser
import org.springframework.stereotype.Component

import java.text.SimpleDateFormat

@Component
class EventCollectorRoute extends RouteBuilder {

    private final static String EVENT_RFH_FILE_NAME = "eventRfhFileName"

    @Override
    void configure() throws Exception {

        from("{{eventRoute.from}}").routeId("{{eventRoute.id}}")
                .log(LoggingLevel.INFO, "Event received")
                .convertBodyTo(String.class, "UTF-8")
                .process { Exchange ex ->
            Message message = ex.in

            def event = new XmlParser(false, false).parseText(message.body)
            String uniqueFlowName = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:messageFlow"."@wmb:uniqueFlowName".text()
            String eventName = event."wmb:eventPointData"."wmb:eventData"."wmb:eventIdentity"."@wmb:eventName".text()
            String creationTime = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:creationTime".text()
            String creationDate = creationTime?.substring(0, 17).replaceAll('[-:T]', '')
            String fileName = "/${uniqueFlowName.replace('.', '/')}/${eventName}-${creationDate}-${ex.exchangeId}"
            message.setHeader(EVENT_RFH_FILE_NAME, fileName + ".rfh")
            message.setHeader(Exchange.FILE_NAME, fileName + ".xml")

        }.to("{{eventRoute.toXml}}")
                .process { Exchange ex ->

            Message message = ex.in

            message.body = MqsiEventParser.extractBitstreamPayload(message.body)
            String fileName = message.getHeader(EVENT_RFH_FILE_NAME)

            message.setHeader(Exchange.FILE_NAME, fileName)

        }
        .to("{{eventRoute.toRfh}}")
    }

}
