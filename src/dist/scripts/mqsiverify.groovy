package scripts

import static groovy.io.FileType.*
import static groovy.io.FileVisitResult.*

def cli = new CliBuilder(
        usage: 'mqsiverify.groovy [options]',
        header: '\nAvailable options (use -h for help):\n',
        footer: '\nInformation provided via above options is used to generate printed string.\n')

cli.with
        {
            o(longOpt: 'original', 'The path to the directory of events from the original broker/bus', args: 1, argName: 'ORG_DIR', required: true)
            r(longOpt: 'regression', 'The path to the directory of events from the regression broker/bus', args: 1, argName: 'REG_DIR', required: true)
            s(longOpt: 'send', 'The path to the directory to copy input events for resend', args: 1, argName: 'SEND_DIR', required: true)
            w(longOpt: 'wait', 'The timeout in seconds before checking for regression events, default 15 seconds', args: 1, argName: 'WAIT', required: false)
        }
def opt = cli.parse(args)
if (!opt) {
    return
}




def found = []
def inputEvents = []

String orgRoot = opt.o
String regRoot = opt.r
String resendDir = opt.s
int waitTimeOut = opt.w ? (opt.w as int) * 1000 : 15000

srcDir = new File(orgRoot)

srcDir.traverse(
        type: FILES,
        nameFilter: ~/.*Step-1.*(xml)$/,
)
        { File file ->
            found.add file
        }

AntBuilder ant = new AntBuilder()

found.each { File file ->
    def event = new XmlParser(false, false).parseText(file.text)
    String localTransactionId = event."wmb:eventPointData"."wmb:eventData"."wmb:eventCorrelation"."@wmb:localTransactionId".text()
    String msgId = event."wmb:applicationData"."wmb:simpleContent".find { it."@wmb:name" == "MsgId" }."@wmb:value"
    String counter = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:counter".text()
    File rfhFile = new File(file.getCanonicalPath().replace('xml', 'rfh'))
    OrgInputEvent orgInputEvent = new OrgInputEvent(localTransactionId: localTransactionId, fileSize: rfhFile.size(), path: file.path, messageId: msgId, file: file, counter: counter)


    File testParent = file.getParentFile()
    List outputEvents = []
    testParent.traverse(
            type: FILES,
            nameFilter: ~/^${localTransactionId}.*Out.(xml)$/,
            {
                File rfhFileOut = new File(it.getCanonicalPath().replace('xml', 'rfh'))
                outputEvents.add(new OrgOutputEvent(localTransactionId: localTransactionId, fileSize: rfhFileOut.size(), path: it.path, file: it))
            }
    )

    orgInputEvent.outputEvents = outputEvents
    inputEvents.add(orgInputEvent)
}



inputEvents.each { OrgInputEvent inputEvent ->

    println "Copy input event to resend"
    // ant.copy( file:"$inputEvent.path", todir:"$resendDir")

    println "WAIT..."
    Thread.sleep(waitTimeOut)

    println "Check output"

    def regDir = new File(regRoot)

    def result = new AntBuilder().fileset(dir: "$regRoot", includes: '**/*Step-1*.xml') {
        containsregexp expression: ".*${inputEvent.messageId}.*"
    }*.file

    result.each { file ->
        def event = new XmlParser(false, false).parseText(file.text)
        String localTransactionId = event."wmb:eventPointData"."wmb:eventData"."wmb:eventCorrelation"."@wmb:localTransactionId".text()
        String msgId = event."wmb:applicationData"."wmb:simpleContent".find { it."@wmb:name" == "MsgId" }."@wmb:value"
        String counter = event."wmb:eventPointData"."wmb:eventData"."wmb:eventSequence"."@wmb:counter".text()
        File rfhFile = new File(file.getCanonicalPath().replace('xml', 'rfh'))

        RegInputEvent regInputEvent = new RegInputEvent(orgLocalTransactionId: inputEvent.localTransactionId, localTransactionId: localTransactionId, fileSize: rfhFile.size(), path: file.path, messageId: msgId, file: file, counter: counter)

        File testParent = file.getParentFile()
        List outputEvents = []
        testParent.traverse(
                type: FILES,
                nameFilter: ~/^${localTransactionId}.*Out.(xml)$/,
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

def verify(def inputEvent) {
    boolean numberOfEventsSame
    boolean sameSequence
    boolean allSameLength
    boolean hasOutput = (inputEvent.regInputEvents)

    println hasOutput

    println "number of original output events ${inputEvent.outputEvents.size()}"
    println "number of regression input events ${inputEvent.regInputEvents.size()}"

    def orgOutputSizes = inputEvent.outputEvents.collect { evt -> evt.fileSize }


    inputEvent.regInputEvents.each {
        println "Verifying output same number of events: ${inputEvent.outputEvents.size()} == ${it.outputEvents.size()}"
        def fileSizesOutput = it.outputEvents.collect { evt -> evt.fileSize }
        println "FileSizes: orginal output : ${orgOutputSizes} and regr. output ${fileSizesOutput} match: ${(orgOutputSizes == fileSizesOutput)}"

    }


}


return ""

class OrgInputEvent {
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