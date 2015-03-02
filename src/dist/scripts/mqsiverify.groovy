package scripts

import java.security.MessageDigest

import static groovy.io.FileType.*
import static groovy.io.FileVisitResult.*

def cli = new CliBuilder(
        usage: 'mqsiverify.groovy [options]',
        header: '\nAvailable options (use -h for help):\n',
        footer: '\nInformation provided via above options is used to generate printed string.\n')

cli.with
        {
            i(longOpt: 'input', 'The path to the directory of events from the original broker/bus', args: 1, argName: 'INPUT_DIR', required: true)
            o(longOpt: 'output', 'The path to the directory of events from the broker/bus under test', args: 1, argName: 'OUTPUT_DIR', required: true)
            r(longOpt: 'resend', 'The path to the directory to copy input events for resend', args: 1, argName: 'RESEND_DIR', required: false)
            w(longOpt: 'wait', 'The timeout in seconds before checking for regression events, default 15 seconds', args: 1, argName: 'WAIT', required: false)
        }
def opt = cli.parse(args)
if (!opt) {
    return
}

def found = []
def inputEvents = []

String orgRoot = opt.i
String regRoot = opt.o
String resendDir = opt.r
boolean resendFlag = opt.r ? true : false
int waitTimeOut = opt.w ? (opt.w as int) * 1000 : 15000

srcDir = new File(orgRoot)

srcDir.traverse(
        type: FILES,
        nameFilter: ~/.*Step-1.*(xml)$/,
)
        { File file ->
            found.add file
        }

println "INFO: Number of test input events: ${found.size()}"

AntBuilder ant = new AntBuilder()

found.each { File file ->

    def event = new XmlParser(false, false).parseText(file.text)
    String localTransactionId = event."wmb:eventPointData"."wmb:eventData"."wmb:eventCorrelation"."@wmb:localTransactionId".text()
    String uniqueFlowName = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:messageFlow"."@wmb:uniqueFlowName".text()

    String msgId = event."wmb:applicationData"."wmb:simpleContent".find { it."@wmb:name" == "MsgId" }."@wmb:value"
    String counter = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:counter".text()
    File rfhFile = new File(file.getCanonicalPath().replace('xml', 'rfh'))
    OrgInputEvent orgInputEvent = new OrgInputEvent(flowName: uniqueFlowName, localTransactionId: localTransactionId, fileSize: rfhFile.size(), path: file.path, messageId: msgId, file: file, counter: counter)

    File testParent = file.getParentFile()
    List outputEvents = []
    testParent.traverse(
            type: FILES,
            nameFilter: ~/^${localTransactionId}_Step.*Out.(xml)$/,
            {
                //println "DEBUG: Found matching out event for input : ${it.name}"
                File rfhFileOut = new File(it.getCanonicalPath().replace('xml', 'rfh'))
                outputEvents.add(new OrgOutputEvent(localTransactionId: localTransactionId, fileSize: rfhFileOut.size(), path: it.path, file: it))
            }
    )

    orgInputEvent.outputEvents = outputEvents
    inputEvents.add(orgInputEvent)
}

inputEvents.each { OrgInputEvent inputEvent ->

    println "INFO: Test for original sequence with localTransactionId: ${inputEvent.localTransactionId}"
    println "INFO: ${inputEvent.flowName}"

    if (resendFlag) {
        println "INFO: Resend by copying file: ${inputEvent.path} to ${resendDir}"
        ant.copy( file:"$inputEvent.path", todir:"$resendDir")

        println "INFO: Waiting ${waitTimeOut / 1000} seconds for new output events"
        Thread.sleep(waitTimeOut)
    }

    String localTransactionIdMD5 = shorterGroovyMD5Hash(inputEvent.localTransactionId)

    def regDir = new File(regRoot)
    def result = []
    regDir.traverse(
            type: FILES,
            nameFilter: ~/.*Step-1.*(xml)$/,
    )
            { File file ->
                if (file.text.contains(localTransactionIdMD5)){
                    println "DEBUG: Found matching event with same localTransactionId in file: ${file.name}"
                    result.add(file)
                }
            }


    //  def result = new AntBuilder().fileset(dir: "$regRoot", includes: '**/*Step-1*.xml') {
    //      containsregexp expression: ".*${inputEvent.messageId}.*"
    //  }*.file

    println "INFO: Found ${result.size()} sequences to verify."

    result.each { file ->
        def event = new XmlParser(false, false).parseText(file.text)
        String localTransactionId = event."wmb:eventPointData"."wmb:eventData"."wmb:eventCorrelation"."@wmb:localTransactionId".text()
        String uniqueFlowName = event."wmb:eventPointData"."wmb:messageFlowData"."wmb:messageFlow"."@wmb:uniqueFlowName".text()

        String msgId = event."wmb:applicationData"."wmb:simpleContent".find { it."@wmb:name" == "MsgId" }."@wmb:value"
        String counter = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:counter".text()
        File rfhFile = new File(file.getCanonicalPath().replace('xml', 'rfh'))

        RegInputEvent regInputEvent = new RegInputEvent(flowName: uniqueFlowName, orgLocalTransactionId: inputEvent.localTransactionId, localTransactionId: localTransactionId, fileSize: rfhFile.size(), path: file.path, messageId: msgId, file: file, counter: counter)

        File testParent = file.getParentFile()
        List outputEvents = []
        testParent.traverse(
                type: FILES,
                nameFilter: ~/^${localTransactionId}_Step.*Out.(xml)$/,
                {
                    File rfhFileOut = new File(it.getCanonicalPath().replace('xml', 'rfh'))
                    outputEvents.add(new RegOutputEvent(localTransactionId: localTransactionId, fileSize: rfhFileOut?.size(), path: it.path, file: it))
                }
        )

        regInputEvent.outputEvents = outputEvents
        inputEvent.regInputEvents.add(regInputEvent)

    }
    verify(inputEvent)

}

def shorterGroovyMD5Hash(somethingToHash){
    MessageDigest.getInstance("MD5").
            digest(somethingToHash.getBytes("UTF-8")).
            encodeHex().
            toString()
}

def verify(def inputEvent) {

    println "INFO: Verifying sequences for localTransactionId ${inputEvent.localTransactionId}"

    boolean hasOutput = (inputEvent.regInputEvents)

    def orgOutputSizes = inputEvent.outputEvents.collect { evt -> evt.fileSize }

    inputEvent.regInputEvents.each {

        println "INFO: \tVerifying output sequence with localTransactionId ${it.localTransactionId}"
        boolean sameNumberOfEvents = (inputEvent.outputEvents.size() == it.outputEvents.size() )

        println "INFO: \t\tSame number of events: ${sameNumberOfEvents}"

        if (! sameNumberOfEvents) {
            println "WARNING: \t\tVerification failed, number of reference output events ${inputEvent.outputEvents.size()}, found ${it.outputEvents.size()} events."
        }

        def fileSizesOutput = it.outputEvents.collect { evt -> evt.fileSize }
        boolean outputFileSizesMatch = ( orgOutputSizes.sort() == fileSizesOutput.sort() )
        println "INFO: \t\tOutput file sizes match: ${outputFileSizesMatch}"
        if (! outputFileSizesMatch) {
            println "WARNING: \t\tFileSizes: input ${orgOutputSizes} and output ${fileSizesOutput}"
        }
    }
}


return ""

class OrgInputEvent {
    String flowName
    String localTransactionId
    String fileSize
    String path
    File file
    String counter
    private String messageId
    private List outputEvents
    private List regInputEvents = []
}

class OrgOutputEvent extends OrgInputEvent {

}

class RegInputEvent {
    String flowName
    String orgLocalTransactionId
    String localTransactionId
    String fileSize
    String path
    String counter
    File file
    private String messageId
    private List outputEvents
}


class RegOutputEvent extends RegInputEvent {

}