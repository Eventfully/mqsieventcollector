package org.eventfully.mqsi.event.collector.component

class MqsiEventParser {

    public static byte[] extractBitstreamPayload(String eventXml) {
        def event = new XmlParser(false, false).parseText(eventXml)
        NodeList bitStream = event."wmb:bitstreamData"."wmb:bitstream"
        byte[] decodedBitStream = bitStream.text().decodeBase64()
        return decodedBitStream
    }
}
