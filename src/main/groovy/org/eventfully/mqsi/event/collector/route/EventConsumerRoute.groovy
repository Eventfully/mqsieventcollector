package org.eventfully.mqsi.event.collector.route

import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsComponent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EventConsumerRoute extends RouteBuilder {

    @Override
    void configure() throws Exception {

        from("jms:TEST.IN?connectionFactory=wmq").process { Exchange ex ->
            ex.in.body = "Hej hopp"
        }.to("jms:TEST.OUT?connectionFactory=wmq");

    }
}
