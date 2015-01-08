package org.eventfully.mqsi.event.collector.config

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import org.apache.camel.CamelContext
import org.apache.camel.component.jms.JmsComponent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.jms.JMSException

@Configuration
@ConfigurationProperties(prefix = "wmqConfig")
public class WmqConfiguration {

    String hostName = "localhost"
    String queueManager = ""
    String channel = "SYSTEM.AUTO.SVRCONN"
    int port = 1414
    int transportType = WMQConstants.WMQ_CM_CLIENT

    @Bean(name =  "wmq")
    JmsComponent wmq() {
        return JmsComponent.jmsComponent(wmqQcf())
    }

    @Bean
    public MQConnectionFactory wmqQcf() throws JMSException {
        return new MQConnectionFactory(hostName: hostName,
                port: port, queueManager: queueManager,
                channel: channel, transportType: transportType)

    }
}
