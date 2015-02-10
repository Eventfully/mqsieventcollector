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
@Log4j
class RFHUtilHelper {

    public MQMessage extract(byte[] rawData) {

        DataInput indata1 = new DataInputStream(new ByteArrayInputStream(rawData))

        MQMD mqmd = new MQMD(indata1, CMQC.MQENC_AS_PUBLISHED, CMQC.MQCCSI_DEFAULT)

        log.debug("MQMD format: '" + mqmd.format + "'")
        log.debug("MQMD ccsid: " + mqmd.getCodedCharSetId())

        MQMessage mqOutMessage = new MQMessage();
        mqOutMessage.characterSet = mqmd.codedCharSetId
        mqOutMessage.format = mqmd.format
        mqOutMessage.messageId = mqmd.msgId
        mqOutMessage.correlationId = mqmd.correlId
        mqOutMessage.encoding = mqmd.encoding
        mqOutMessage.accountingToken = mqmd.accountingToken
        mqOutMessage.expiry = mqmd.expiry
        mqOutMessage.applicationOriginData = mqmd.applIdentityData
        mqOutMessage.applicationOriginData = mqmd.applOriginData
        mqOutMessage.backoutCount = mqmd.backoutCount
        mqOutMessage.feedback = mqmd.feedback
        mqOutMessage.messageFlags = mqmd.msgFlags
        mqOutMessage.messageSequenceNumber = mqmd.msgSeqNumber
        mqOutMessage.messageType = mqmd.msgType
        mqOutMessage.offset = mqmd.offset
        mqOutMessage.originalLength = mqmd.originalLength
        mqOutMessage.persistence = mqmd.persistence
        mqOutMessage.priority = mqmd.priority
        mqOutMessage.putApplicationName = mqmd.putApplName
        mqOutMessage.putApplicationType = mqmd.putApplType
        //mqOutMessage.putDateTime = mqmd.putDateTime
        mqOutMessage.replyToQueueManagerName = mqmd.replyToQMgr
        mqOutMessage.replyToQueueName = mqmd.replyToQ
        mqOutMessage.report = mqmd.report
        mqOutMessage.userId = mqmd.userIdentifier
        mqOutMessage.version = mqmd.version

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

        return mqOutMessage;

    }

}
