package org.eventfully.mqsi.event.collector.route

import com.ibm.mq.MQMessage
import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.Message
import org.apache.camel.builder.RouteBuilder
import org.eventfully.mqsi.event.collector.component.MqsiEventParser
import org.eventfully.mqsi.event.collector.component.RFHUtilHelper
import org.eventfully.mqsi.event.collector.component.StaticMQSender
import org.eventfully.mqsi.event.collector.config.ResendConfiguration
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

    @Autowired
    ResendConfiguration resendConfiguration

    @Override
    public void configure() throws Exception {

        from("{{xmlResenderRoute.from}}").routeId("{{xmlResenderRoute.id}}")
                .autoStartup("{{xmlResenderRoute.enabled}}")
                .log(LoggingLevel.INFO, "Resend request received")
                .convertBodyTo(String.class, "UTF-8")
                .process { Exchange ex ->

            Message message = ex.in
            def event = new XmlParser(false, false).parseText(message.body)
            String flowName = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:messageFlow"."@wmb:name".text()
            String eventSrc = event."wmb:eventPointData"."wmb:eventData"."@wmb:eventSourceAddress".text()
            // Add error handling if it is not an ComIbmMQInputNode
            String nodeDetail = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:node"."@wmb:detail".text()
            String resendQueue = resendConfiguration.findResendQueueForEventSource(flowName, eventSrc) ?: nodeDetail

            log.info("Found resend queue ${resendQueue} for flow: ${flowName} and event source ${eventSrc}")

            NodeList bitStream = event."wmb:bitstreamData"."wmb:bitstream"
            byte[] decodedBitStream = bitStream.text().decodeBase64()

            MQMessage mqMessage = rfhUtilHelper.extract(decodedBitStream)
            mqSender.resendToQueue(mqMessage, resendQueue)

        }
        .log(LoggingLevel.INFO, "Message resent")

    }
}
