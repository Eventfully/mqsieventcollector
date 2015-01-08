package org.eventfully.mqsi.event.collector.route

import com.ibm.mq.jms.MQConnectionFactory
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EventConsumerRoute extends RouteBuilder {

    @Autowired
    MQConnectionFactory wmqConnectionFactory

    @Override
    void configure() throws Exception {

        from("{{eventRoute.from}}").routeId("{{eventRoute.id}}").process { Exchange ex ->
            ex.in.body = "JMS says hi"
        }.to("{{eventRoute.to}}");

    }
}
