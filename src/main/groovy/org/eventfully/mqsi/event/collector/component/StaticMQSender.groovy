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
@ConfigurationProperties(prefix = "mqSender")
@Log4j
class StaticMQSender {

    String qmgr
    String hostname
    int port
    String channel
    String queue

    public void resendToQueue(MQMessage mqMessage, String queueName) {

        Hashtable connProps = new Hashtable()
        connProps.put(MQC.HOST_NAME_PROPERTY, hostname)
        connProps.put(MQC.CHANNEL_PROPERTY, channel)
        connProps.put(MQC.PORT_PROPERTY, port)

        MQQueueManager queueManager = new MQQueueManager(qmgr, connProps)
        MQQueue mqQueue = queueManager.accessQueue(queueName, MQConstants.MQOO_OUTPUT);

        mqQueue.put(mqMessage)
        mqQueue.close();
        queueManager.disconnect()

    }

    public void resend(MQMessage mqMessage){
        resendToQueue(mqMessage, queue)
    }

}
