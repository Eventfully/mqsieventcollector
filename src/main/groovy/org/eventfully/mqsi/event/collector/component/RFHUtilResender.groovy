package org.eventfully.mqsi.event.collector.component

import com.ibm.mq.MQC
import com.ibm.mq.MQMessage
import com.ibm.mq.MQQueue
import com.ibm.mq.MQQueueManager
import com.ibm.mq.constants.CMQC
import com.ibm.mq.constants.MQConstants
import com.ibm.mq.headers.MQHeaderList
import com.ibm.mq.headers.MQMD
import com.ibm.mq.headers.MQRFH2
import groovy.util.logging.Log4j
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "resend")
@Log4j
class RFHUtilResender {

    String qmgr
    String queue
    String hostname
    int port
    String channel

    public boolean resend(byte[] rawData) {

        DataInput indata1 = new DataInputStream(new ByteArrayInputStream(rawData))

        MQMD mqmd = new MQMD(indata1, CMQC.MQENC_AS_PUBLISHED, CMQC.MQCCSI_DEFAULT)

        log.debug("MQMD format: '" + mqmd.format + "'")
        log.debug("MQMD ccsid: " + mqmd.getCodedCharSetId())

        MQMessage mqOutMessage = new MQMessage();
        mqOutMessage.characterSet = mqmd.codedCharSetId

        if (mqmd.format == MQConstants.MQFMT_RF_HEADER_2) {
            MQRFH2 mqrfh2 = new MQRFH2(indata1, mqmd.encoding, mqmd.codedCharSetId)
            log.debug("RFH2 format: '" + mqrfh2.format + "'")
            log.debug("RFH2 ccsid: " + mqrfh2.codedCharSetId)
            mqOutMessage.format = MQConstants.MQFMT_RF_HEADER_2;
            mqrfh2.write(mqOutMessage)
        }
        byte[] remaining = new byte[indata1.available()]
        indata1.readFully(remaining)
        mqOutMessage.write(remaining)

        Hashtable connProps = new Hashtable()
        connProps.put(MQC.HOST_NAME_PROPERTY, hostname)
        connProps.put(MQC.CHANNEL_PROPERTY, channel)
        connProps.put(MQC.PORT_PROPERTY, port)

        MQQueueManager queueManager = new MQQueueManager(qmgr, connProps)
        MQQueue queue = queueManager.accessQueue(queue, MQConstants.MQOO_OUTPUT);

        //  rfhFIS.close()
        queue.put(mqOutMessage)
        queue.close();
        queueManager.disconnect()

        return true;

    }

}
