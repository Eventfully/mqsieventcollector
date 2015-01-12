package org.eventfully.mqsi.event.collector.route

import com.ibm.mq.MQC
import com.ibm.mq.MQMessage
import com.ibm.mq.MQQueue
import com.ibm.mq.MQQueueManager
import com.ibm.mq.constants.CMQC
import com.ibm.mq.constants.MQConstants
import com.ibm.mq.headers.MQHeaderIterator
import com.ibm.mq.headers.MQHeaderList
import com.ibm.mq.headers.MQMD
import com.ibm.mq.headers.MQRFH2
import org.apache.camel.Exchange
import org.apache.camel.LoggingLevel
import org.apache.camel.Message
import org.apache.camel.builder.RouteBuilder
import org.eventfully.mqsi.event.collector.component.RFHUtilResender
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "resenderRoute")
class EventPayloadSenderRoute extends RouteBuilder {

    @Autowired
    RFHUtilResender rfhUtilResender

    @Override
    public void configure() throws Exception {

        from("{{resenderRoute.from}}").routeId("{{resenderRoute.id}}")
                .log(LoggingLevel.INFO, "Resend request received")
                .bean(rfhUtilResender, "resend")
                .log(LoggingLevel.INFO, "Message resent")

    }
}
