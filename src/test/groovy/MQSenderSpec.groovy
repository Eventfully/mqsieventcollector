package org.eventfully.mqsi.event.collector.test

import org.eventfully.mqsi.event.collector.component.RFHUtilResender
import spock.lang.Specification
import spock.lang.Unroll

class MQSenderSpec extends Specification {

    @Unroll
    def "Store articleNumber #articleNumber"() {

        given: "A RFHutil file"
        def testPayload = new File("src/test/resources/data", inputFile)
        and: "A resender"
        RFHUtilResender resender = new RFHUtilResender()
        resender.with {
            queue = "TEST.IN"
            port = 2414
            hostname = "localhost"
            channel = "JMS"

        }

        when: "The request is sent"
        boolean result = resender.resend(testPayload.getBytes())

        then: "A reply is received"
        result

        where:
        inputFile | _
        'InputOrder.rfh' | _
        'InputOrderWithRFH2.rfh' | _

    }

}

