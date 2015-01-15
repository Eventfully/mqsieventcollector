package org.eventfully.mqsi.event.collector.config

import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import org.apache.camel.component.jms.JmsComponent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.jms.JMSException

@Configuration
@ConfigurationProperties(prefix = "resendConfig")
public class ResendConfiguration {

    String configFileName = "resendConfig.xml"
    Node configurationNode

    @Bean
    ResendConfiguration resendConfig() {
        File eventXml = new File(configFileName)
        this.configurationNode = new XmlParser(false, false).parse(eventXml)
        return this
    }

    public String findResendQueueForEventSource(String flow, String eventSrc) {
        configurationNode?."${flow}"?.find { it.@eventSrc == eventSrc }?.@resendQueue
    }

    public String findResendQueueForEventName(String flow, String eventName) {
        configurationNode?."${flow}"?.find { it.@eventName == eventName }?.@resendQueue
    }

}
