package org.eventfully.mqsi.event.collector.component

import com.ibm.mq.MQC
import com.ibm.mq.MQMessage
import com.ibm.mq.MQQueue
import com.ibm.mq.MQQueueManager
import com.ibm.mq.constants.MQConstants
import groovy.util.logging.Log4j
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "staticMQSender")
@Log4j
class StaticMQSender {

    String qmgr
    String hostname
    int port
    String channel
    String queue

    public void resend(MQMessage mqMessage) {

        Hashtable connProps = new Hashtable()
        connProps.put(MQC.HOST_NAME_PROPERTY, hostname)
        connProps.put(MQC.CHANNEL_PROPERTY, channel)
        connProps.put(MQC.PORT_PROPERTY, port)

        MQQueueManager queueManager = new MQQueueManager(qmgr, connProps)
        MQQueue mqQueue = queueManager.accessQueue(queue, MQConstants.MQOO_OUTPUT);

        mqQueue.put(mqMessage)
        mqQueue.close();
        queueManager.disconnect()

    }

}
