package org.eventfully.mqsi.event.collector.route

import org.apache.camel.Exchange
import org.apache.camel.Message
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class EventConsumerRoute extends RouteBuilder {

    @Override
    void configure() throws Exception {

        from("{{eventRoute.from}}").routeId("{{eventRoute.id}}")
                .convertBodyTo(String.class, "UTF-8")
                .process { Exchange ex ->

            Message message = ex.in
            String xmlString = message.body
            def event = new XmlParser(false, false).parseText(xmlString)
            NodeList bitStream = event."wmb:bitstreamData"."wmb:bitstream"
            byte[] decodedBitStream = bitStream.text().decodeBase64()
            message.setBody(decodedBitStream)

            String uniqueFlowName = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:messageFlow"."@wmb:uniqueFlowName".text()

            String eventName = event."wmb:eventPointData"."wmb:eventData"."wmb:eventIdentity"."@wmb:eventName".text()
            String creationTime = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:creationTime".text()
            String creationDate = creationTime?.substring(0, 9).replace('-', '')

            message.setHeader("uniqueFlowName", uniqueFlowName.replace('.', '/'))
            message.setHeader("eventName", eventName)
            message.setHeader("creationDate", creationDate)
            message.setHeader("exchangeId", ex.exchangeId)

        }.setHeader(Exchange.FILE_NAME, simple('/${in.header.uniqueFlowName}/${in.header.eventName}-${in.header.creationDate}-${in.header.exchangeId}.rfh'))
                .to("{{eventRoute.to}}")
    }
}
