package org.eventfully.mqsi.event.collector.config

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.jms.internal.JMSComponent
import com.ibm.msg.client.wmq.WMQConstants
import org.apache.camel.CamelContext
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsComponent
import org.apache.camel.spring.boot.CamelContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.jms.JMSException

@Configuration
//@EnableConfigurationProperties(WMQConnectionFactoryProperties.class)
public class BaseConfiguration {

    @Autowired
    CamelContext camelContext;

   // @Bean(name = "wmq")
   // public JmsComponent wmq() throws JMSException {
   //     JMSCon
   //     JMSComponent wmq = JmsComponent.jmsComponent(createWMQ())
//
  //  }

    @Bean
    public MQConnectionFactory wmq() throws JMSException {
        return new MQConnectionFactory(hostName: WmqConfig.hostName,
                port: WmqConfig.port, queueManager: WmqConfig.queueManager,
                channel: WmqConfig.channel, transportType: WmqConfig.transportType)

    }

    //@ConfigurationProperties("wmq")
    public static class WmqConfig {

        static String hostName = "localhost"

        static int port = 2414

        static String queueManager = "MB7QMGR"

        static  String channel = "JMS"

        static int transportType = WMQConstants.WMQ_CM_CLIENT

    }

/*
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            void beforeApplicationStart(CamelContext context) {
                context.addComponent("wmq", wmq())
            }
        };
    }
*/
}
