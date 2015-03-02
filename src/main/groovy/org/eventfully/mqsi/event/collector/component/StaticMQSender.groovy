package org.eventfully.mqsi.event.collector.component

import com.ibm.mq.MQC
import com.ibm.mq.MQMessage
import com.ibm.mq.MQPutMessageOptions
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

        int openOptions = MQConstants.MQOO_OUTPUT + MQConstants.MQOO_SET_IDENTITY_CONTEXT;

        MQQueueManager queueManager = new MQQueueManager(qmgr, connProps)
        MQQueue mqQueue = queueManager.accessQueue(queueName,openOptions)
        MQPutMessageOptions pmo = new MQPutMessageOptions()
        pmo.options = MQConstants.MQPMO_FAIL_IF_QUIESCING | MQConstants.MQPMO_SET_IDENTITY_CONTEXT | MQConstants.MQPMO_SYNCPOINT;

        mqQueue.put(mqMessage,pmo)
        log.debug "Sending msg with applicationidentidata: ${mqMessage.applicationIdData}"
        mqQueue.close();
        queueManager.disconnect()

    }

    public void resend(MQMessage mqMessage){
        resendToQueue(mqMessage, queue)
    }

}
