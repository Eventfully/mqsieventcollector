package org.eventfully.mqsi.event.collector.test

import org.eventfully.mqsi.event.collector.component.RFHUtilResender
import spock.lang.Specification
import spock.lang.Unroll

class MQSenderSpec extends Specification {

    RFHUtilResender resender

    def setup() {
        Properties applicationProperties = new Properties()
        applicationProperties.load(new FileReader("src/test/resources/application.properties"))
        resender = new RFHUtilResender()
        resender.with {
            queue = applicationProperties.getProperty("resend.queue", "TEST.IN")
            port = applicationProperties.getProperty("resend.port", "2414") as int
            hostname = applicationProperties.getProperty("resend.hostname", "localhost")
            channel = applicationProperties.getProperty("resend.channel", "JMS")
        }
    }

    @Unroll
    def "Store articleNumber #articleNumber"() {

        given: "A RFHutil file"
        def testPayload = new File("src/test/resources/data", inputFile)

        when: "The request is sent"
        boolean result = resender.resend(testPayload.getBytes())

        then: "A reply is received"
        result

        where:
        inputFile                | _
        'InputOrder.rfh'         | _
        'InputOrderWithRFH2.rfh' | _

    }

}

