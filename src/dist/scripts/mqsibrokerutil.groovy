package scripts

import com.ibm.broker.config.proxy.*

def cli = new CliBuilder(
        usage: 'mqsibrokerutil.groovy [options]',
        header: '\nAvailable options (use -h for help):\n',
        footer: '\nInformation provided via above options is used to generate printed string.\n')


cli.with
        {
            //  b(longOpt: 'brokername', 'The name the local broker', args:1, argName:'LOCALBROKER', required: false)
            f(longOpt: 'brokerfile', 'The name of the .broker connection file', args: 1, argName: 'BROKERFILE', required: false)
            i(longOpt: 'ip', 'The ip or hostname of the broker', args: 1, argName: 'IP', required: false)
            p(longOpt: 'port', 'The port of the broker mq listener', args: 1, argName: 'PORT', required: false)
            q(longOpt: 'qmgr', 'The broker queuemanager name', args: 1, argName: 'QMGR', required: false)
            c(longOpt: 'cmd', 'The command, one of: [ list, profile]', args: 1, argName: 'CMD', required: true)
            e(longOpt: 'eg', 'The execution group/integration server name', args: 1, argName: 'EG', required: false)
            m(longOpt: 'msgflow', 'The message flow name, note that you must specify eg as well using this option.', args: 1, argName: 'MSGFLOW', required: false)
            w(longOpt: 'workdir', 'The working directory for generating output files, defaults to current directory', args: 1, argName: 'WORKDIR', required: false)
            s(longOpt: 'sources', 'eventsources=eventname key-value for generating events, mandatory for command profile, can be repeated multiple times.', args: 2, valueSeparator: '=', argName: 'SOURCES', required: false)
            a(longOpt: 'auto', 'Auto create a monitoring profile for the flow in eg specified by -m -e, default is for MQInputNodes and transaction.start/end with payload', args: 0, argName: 'AUTO', required: false)
        }
def opt = cli.parse(args)
if (!opt) {
//    cli.usage()
    return
}

String localBroker = opt.b ?: null
String brokerFile = opt.f ?: null
String hostname = opt.i ?: null
int port = opt.p ? opt.p as int : 1414
String qmgr = opt.q ?: null
Command command = Command.valueOf(opt.c)
String egName = opt.e ?: null
String msgFlowName = opt.m ?: null
def sources = opt.s ?: null
String workDirName = opt.w ?: System.properties.getProperty('user.dir')
boolean autoCreateProfile = opt.a

BrokerConnectionParameters bcp

if (localBroker) {
    println "Connect to local broker: " + localBroker
    bcp = new LocalBrokerConnectionParameters(localBroker)
} else if (brokerFile) {
    println "Connect using .broker file: " + brokerFile
    bcp = new MQPropertyFileBrokerConnectionParameters(brokerFile)
} else if (hostname && port && qmgr) {
    println "Connect using hostname, port and qmgr arguments"
    bcp = new MQBrokerConnectionParameters(hostname, port, qmgr)
} else {
    println "You must specify either the [ -b | --brokername ] or [ -f | --brokerfile ] or all of [ -p, -q, -i ]"
    return
}

BrokerProxy proxy = BrokerProxy.getInstance(bcp);

if (!proxy.hasBeenPopulatedByBroker(true)) {
    println "ERROR: Broker not responding..."
    return
}

String brokerName = proxy.name
println "Connected to: '${brokerName}'"

if (command == Command.list) {
    if (msgFlowName && egName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        MessageFlowProxy mfp = egp.getMessageFlowByName(msgFlowName)
        displayFlowInExecutionGroup(egp, msgFlowName)
        return displayFlowDetails(mfp)
    } else if (egName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        return listFlowsInExecutionGroup(egp)
    } else {
        return listEGs(proxy)
    }
} else if (command == Command.profile) {
    String profile = null
    if (sources) {
        def sourceMap = opt.ss.toSpreadMap()
        sourceMap.each { k, v -> println k + " " + v }
        profile = MonitoringProfileFactory.newProfile(sourceMap)
    } else if (autoCreateProfile && msgFlowName && egName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        MessageFlowProxy mfp = egp.getMessageFlowByName(msgFlowName)
        def sourceMap = createFlowInputMQEventSourcesMap(mfp)
        profile = MonitoringProfileFactory.newProfile(sourceMap)
    } else {
        println "Sorry, nothing here."
        return
    }

    println profile
    File outputDir = new File(workDirName + "/${msgFlowName}")
    outputDir.deleteDir()
    outputDir.mkdir()
    String xmlProfileFileName = "${msgFlowName}-profile.xml"
    File outputProfileFile = new File(outputDir, xmlProfileFileName)
    outputProfileFile.setText(profile, "UTF-8")
    println "profile xml created: ${outputProfileFile}"

    println "creating mqsicommand files"
    createMqsiCommandFilesUsingTemplates(outputDir, brokerName, egName, msgFlowName, xmlProfileFileName)

} else {
    println "not implemented yet"
    return
}

def createMqsiCommandFilesUsingTemplates(File outputDir, String brokerName, String egName, String msgFlowName, String profileXmlFileName) {
    String exportFileName = "exported-" + profileXmlFileName
    def binding = ["brokerName"        : brokerName,
                   "egName"            : egName,
                   "msgFlowName"       : msgFlowName,
                   "profileFileName"   : profileXmlFileName,
                   "profileName"       : msgFlowName,
                   "exportFileName"    : exportFileName]

    def templateFiles = [ "activateFlowEvents.template",
                          "createMonitoringProfile.template",
                          "deleteMonitoringProfile.template",
                          "exportFlowMonitoringProfile.template",
                          "inactivateFlowEvents.template",
                          "listAllConfigurableEventsInFlow.template",
                          "listAllConfiguredEventsInFlow.template",
                          "updateMonitoringProfile.template"]

    String templateDirName = System.properties.getProperty('user.dir') + "/scripts/templates"

    templateFiles.each { templateFileName ->
        File templateFile = new File(templateDirName, templateFileName)
        def engine = new GStringTemplateEngine()
        def activeTemplate = engine.createTemplate(templateFile).make(binding)
        File ouputFile = new File(outputDir, templateFileName.replaceFirst("template", "cmd"))
        ouputFile.text = activeTemplate.toString()
    }
}

def listEGs(BrokerProxy proxy) {
    proxy.getExecutionGroups().each { ExecutionGroupProxy egp ->
        println "\tEG '${egp.name}' is ${egp.isRunning() ? 'running' : 'stopped'}"
    }
}

def listFlowsInExecutionGroup(ExecutionGroupProxy egp) {
    egp.getMessageFlows().each { MessageFlowProxy mfp ->
        println "\t\tMF: '${mfp.name}' in EG:'${egp.name}' is ${mfp.isRunning() ? 'running' : 'stopped'}"
    }
}

def displayFlowInExecutionGroup(ExecutionGroupProxy egp, String msgFlowName) {
    MessageFlowProxy mfp = egp.getMessageFlowByName(msgFlowName)
    assert mfp
    println "\t\tMF: '${mfp.name}' in EG:'${egp.name}' is ${mfp.isRunning() ? 'running' : 'stopped'}"
}

def displayFlowDetails(MessageFlowProxy mfp) {
    if (mfp.isRunning()) {
        def monitoring = mfp.getRuntimeProperty('This/monitoring')
        println "\t\t\t\tMonitoring is: ${monitoring}"
        def queueNames = mfp.queues
        println "\t\t\t\tQueues used in flow:"
        queueNames.each { println "\t\t\t\t\t" + it }
        def nodeNames = mfp.nodes
        println "\t\t\t\tNodes used in flow:"
        nodeNames.each { MessageFlowProxy.Node node ->
            println "\t\t\t\t\t" + node.name + " [ type: ${node.type} ${(node.type).startsWith('ComIbmMQ') ? ' ,queue: ' + node.properties.getProperty('queueName') : ''} ]"

        }
    }
}

def createFlowInputMQEventSourcesMap(MessageFlowProxy mfp) {
    def eventSourceMap = [:]
    def nodeNames = mfp.nodes
    nodeNames.each { MessageFlowProxy.Node node ->
        if (node.type == 'ComIbmMQInputNode') {
            println "\tFound MQ input node: " + node.name + " with queue: " + node.properties.getProperty('queueName')
            eventSourceMap.put("${node.name}.transaction.Start", "${node.name}.Start")
            eventSourceMap.put("${node.name}.transaction.End", "${node.name}.End")
            // eventSourceMap.put("${node.name}.transaction.Rollback", "${node.name}.Rollback")
        }
    }
    return eventSourceMap
}

enum Command {
    list, profile
}

import groovy.text.GStringTemplateEngine

class MonitoringProfileFactory {

    static String xmlProfile = '''<profile:monitoringProfile xmlns:profile="http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0.3/monitoring/profile" profile:version="2.0"><% sources.each { %><profile:eventSource profile:eventSourceAddress="${it.eventSource}" profile:enabled="true"><profile:eventPointDataQuery><profile:eventIdentity><profile:eventName profile:literal="${it.eventName}"/></profile:eventIdentity><profile:eventCorrelation><profile:localTransactionId profile:sourceOfId="automatic"/><profile:parentTransactionId profile:sourceOfId="automatic"/><profile:globalTransactionId profile:sourceOfId="automatic"/></profile:eventCorrelation><profile:eventFilter profile:queryText="true()"/><profile:eventUOW profile:unitOfWork="messageFlow"/></profile:eventPointDataQuery><profile:applicationDataQuery></profile:applicationDataQuery><profile:bitstreamDataQuery profile:bitstreamContent="${it.bitstreamContent}" profile:encoding="${it.bitstreamEncoding}"/></profile:eventSource><% } %></profile:monitoringProfile>'''

    static String newProfile(def sources) {
        GStringTemplateEngine engine = new GStringTemplateEngine()

        def expandoSources = []
        sources.each { key, value ->
            expandoSources.add(new Expando(eventSource: key, eventName: value, bitstreamEncoding: BitstreamEncoding.base64Binary, bitstreamContent: BitstreamContent.all))
        }

        def binding = [sources: expandoSources]
        def template = engine.createTemplate(xmlProfile).make(binding)
        return template.toString()
    }
}


enum BitstreamContent {
    none, all
}

enum BitstreamEncoding {
    none, base64Binary
}

class EventConfig {

    String name
    String source
    BitstreamContent content
    BitstreamEncoding encoding

}