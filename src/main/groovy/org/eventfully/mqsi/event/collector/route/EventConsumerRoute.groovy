package org.eventfully.mqsi.event.collector.route

import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.Message
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class EventConsumerRoute extends RouteBuilder {

    @Override
    void configure() throws Exception {

        from("{{eventRoute.from}}").routeId("{{eventRoute.id}}")
                .log( LoggingLevel.INFO, "Event received")
                .convertBodyTo(String.class, "UTF-8")
                .process { Exchange ex ->

            Message message = ex.in

            def event = new XmlParser(false, false).parseText(message.body)
            NodeList bitStream = event."wmb:bitstreamData"."wmb:bitstream"
            byte[] decodedBitStream = bitStream.text().decodeBase64()
            message.body = decodedBitStream

            String uniqueFlowName = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:messageFlow"."@wmb:uniqueFlowName".text()
            String eventName = event."wmb:eventPointData"."wmb:eventData"."wmb:eventIdentity"."@wmb:eventName".text()
            String creationTime = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:creationTime".text()
            String creationDate = creationTime?.substring(0, 9).replace('-', '')

            String fileName = "/${uniqueFlowName.replace('.', '/')}/${eventName}-${creationDate}-${ex.exchangeId}.rfh"
            message.setHeader(Exchange.FILE_NAME, fileName)

        }
        .to("{{eventRoute.to}}")
    }
}
