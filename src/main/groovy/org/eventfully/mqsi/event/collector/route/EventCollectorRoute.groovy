package org.eventfully.mqsi.event.collector.route

import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.Message
import org.apache.camel.builder.RouteBuilder
import org.eventfully.mqsi.event.collector.component.MqsiEventParser
import org.springframework.stereotype.Component

import org.joda.time.*
import org.joda.time.format.*

@Component
class EventCollectorRoute extends RouteBuilder {

    private final static String EVENT_RFH_FILE_NAME = "eventRfhFileName"

    @Override
    void configure() throws Exception {

        onException().handled(true).logStackTrace(true)
                .to("log:org.eventfully.mqsi.event.collector?showAll=true&level=ERROR&showStackTrace=true&multiline=true")
                .to("{{eventRoute.toFailure}}")


        final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();

        from("{{eventRoute.from}}").routeId("{{eventRoute.id}}")
                .log(LoggingLevel.DEBUG, "Event received")
                .convertBodyTo(String.class, "UTF-8")
                .process { Exchange ex ->
            Message message = ex.in

            def event = new XmlParser(false, false).parseText(message.body)
            String uniqueFlowName = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:messageFlow"."@wmb:uniqueFlowName".text()
            String eventName = event."wmb:eventPointData"."wmb:eventData"."wmb:eventIdentity"."@wmb:eventName".text()
            String creationTime = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:creationTime".text()
            String counter = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:counter".text()
            String localTransactionId = event."wmb:eventPointData"."wmb:eventData"."wmb:eventCorrelation"."@wmb:localTransactionId".text()
            String creationDate = creationTime?.substring(0, 17).replaceAll('[-:T]', '')
            DateTime dt = fmt.parseDateTime(creationTime);

            String fileName = "/${uniqueFlowName.replace('.', '/')}/${dt.year}/${dt.monthOfYear}/${dt.dayOfMonth}/${localTransactionId}_Step-${counter}_Event-${eventName}"
            message.setHeader(EVENT_RFH_FILE_NAME, fileName + ".rfh")
            message.setHeader(Exchange.FILE_NAME, fileName + ".xml")
            log.info "Saving event: ${fileName}"

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
