package org.eventfully.mqsi.event.collector.test

import org.eventfully.mqsi.event.collector.component.RFHUtilResender
import spock.lang.Specification

class MQSenderSpec extends Specification {

    def "Store articleNumber #articleNumber"() {

        given: "A RFHutil file"
        def testPayload = new File('src/test/resources/data/InputOrderWithRFH2.rfh')

        when: "The request is sent"
        boolean result = RFHUtilResender.resend(testPayload.getBytes())

        then: "A reply is received"
        result
    }

}

