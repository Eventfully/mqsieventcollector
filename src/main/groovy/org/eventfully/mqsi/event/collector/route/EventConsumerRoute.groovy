package org.eventfully.mqsi.event.collector.route

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component

@Component
class EventConsumerRoute extends RouteBuilder {

    @Override
    void configure() throws Exception {

        from("{{eventRoute.from}}").routeId("{{eventRoute.id}}").process { Exchange ex ->
            ex.in.body = "JMS says hi"
        }.to("{{eventRoute.to}}");

    }
}
