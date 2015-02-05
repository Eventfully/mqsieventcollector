package scripts

import com.ibm.broker.config.proxy.*

def cli = new CliBuilder(
        usage: 'mqsibrokerutil.groovy [options]',
        header: '\nAvailable options (use -h for help):\n',
        footer: '\nInformation provided via above options is used to generate printed string.\n')


cli.with
        {
            h(longOpt: 'help', 'Show help', args: 0, argName: 'HELP', required: false)
            b(longOpt: 'brokerfile', 'The name of the .broker connection file', args: 1, argName: 'BROKERFILE', required: false)
            i(longOpt: 'ip', 'The ip or hostname of the broker', args: 1, argName: 'IP', required: false)
            p(longOpt: 'port', 'The port of the broker mq listener', args: 1, argName: 'PORT', required: false)
            q(longOpt: 'qmgr', 'The broker queuemanager name', args: 1, argName: 'QMGR', required: false)
            c(longOpt: 'create', 'Create profile', args: 0, argName: 'CREATE', required: false)
            e(longOpt: 'eg', 'The execution group/integration server name', args: 1, argName: 'EG', required: false)
            f(longOpt: 'flow', 'The message flow name, note that you must specify eg as well using this option.', args: 1, argName: 'flow', required: false)
            w(longOpt: 'workdir', 'The working directory for generating output files, defaults to current directory', args: 1, argName: 'WORKDIR', required: false)
            s(longOpt: 'sources', 'eventsources=eventname key-value for generating events, mandatory for command profile, can be repeated multiple times.', args: 2, valueSeparator: '=', argName: 'SOURCES', required: false)
            a(longOpt: 'auto', 'Auto create a monitoring profile for the flow in eg specified by -m -e, default is enabling MQInputNodes with transaction.start with payload and configured but disabled MQOutputNodes terminal.in with payload.', args: 0, argName: 'AUTO', required: false)
        }
def opt = cli.parse(args)
if (!opt || opt.h) {
    cli.usage()
    return
}

String brokerFile = opt.b ?: null
String hostname = opt.i ?: null
int port = opt.p ? opt.p as int : 1414
String qmgr = opt.q ?: null
Command command = opt.c ? Command.profile : Command.list
String egName = opt.e ?: null
String msgFlowName = opt.f ?: null
def sources = opt.s ?: null
String workDirName = opt.w ?: System.properties.getProperty('user.dir')
boolean autoCreateProfile = opt.a

BrokerConnectionParameters bcp

if (brokerFile) {
    println "Connect using .broker file: " + brokerFile
    bcp = new MQPropertyFileBrokerConnectionParameters(brokerFile)
} else if (hostname && port && qmgr) {
    println "Connect using hostname, port and qmgr arguments"
    bcp = new MQBrokerConnectionParameters(hostname, port, qmgr)
} else {
    println "You must specify either the [ -b | --brokername ] or [ -f | --brokerfile ] or all of [ -p, -q, -i ]"
    return
}

if (command == Command.profile) {
    if (!(sources || autoCreateProfile)) {
        println "For the create profile option you must either specify the -a or -s [eventSrc:eventName] options as well."
        return
    }
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
    def outputEventSources
    if (sources) {
        def sourceMap = opt.ss.toSpreadMap()
        sourceMap.each { k, v -> println k + " " + v }
        profile = MonitoringProfileFactory.newProfile(sourceMap)
    } else if (autoCreateProfile && msgFlowName && egName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        MessageFlowProxy mfp = egp.getMessageFlowByName(msgFlowName)
        def sourceMQInputMap = createFlowInputMQEventSourcesMap(mfp)
        Map sourceMQOutputMap = createFlowInputMQEventSourcesMap(mfp)
        outputEventSources = sourceMQOutputMap.collect { it.key }.join(',')
        profile = MonitoringProfileFactory.newProfile(sourceMQInputMap, sourceMQOutputMap)
    } else {
        println "Wrong arguments supplied for profile."
        cli.usage()
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
    createMqsiCommandFilesUsingTemplates(outputDir, brokerName, egName, msgFlowName, xmlProfileFileName, outputEventSources)

} else {
    println "not implemented yet"
    return
}

def createMqsiCommandFilesUsingTemplates(File outputDir, String brokerName, String egName, String msgFlowName, String profileXmlFileName, String outputEventSources) {
    String exportFileName = "%CD%\\exported-" + profileXmlFileName

    def binding = ["brokerName"        : brokerName,
                   "egName"            : egName,
                   "msgFlowName"       : msgFlowName,
                   "profileFileName"   : profileXmlFileName,
                   "profileName"       : msgFlowName,
                   "exportFileName"    : exportFileName,
                   "outputEventSources": outputEventSources]

    def templateFiles = ["activateFlowEvents.template",
                         "createMonitoringProfile.template",
                         "deleteMonitoringProfile.template",
                         "exportFlowMonitoringProfile.template",
                         "inactivateFlowEvents.template",
                         "listAllConfigurableEventsInFlow.template",
                         "listAllConfiguredEventsInFlow.template",
                         "updateMonitoringProfile.template",
                         "activateMQOutputEvents.template",
                         "inactivateMQOutputEvents.template"]

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
            //eventSourceMap.put("${node.name}.transaction.End", "${node.name}.End")
            // eventSourceMap.put("${node.name}.transaction.Rollback", "${node.name}.Rollback")
        }
    }
    return eventSourceMap
}

def createFlowOutputMQEventSourcesMap(MessageFlowProxy mfp) {
    def eventSourceMap = [:]
    def nodeNames = mfp.nodes
    nodeNames.each { MessageFlowProxy.Node node ->
        if (node.type == 'ComIbmMQOutputNode') {
            println "\tFound MQ output node: " + node.name + " with queue: " + node.properties.getProperty('queueName')
            eventSourceMap.put("${node.name}.terminal.in", "${node.name}.Out")
        }
    }
    return eventSourceMap
}

enum Command {
    list, profile
}

import groovy.text.GStringTemplateEngine

class MonitoringProfileFactory {

    static String xmlProfile = '''<profile:monitoringProfile xmlns:profile="http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0.3/monitoring/profile" profile:version="2.0"><% sources.each { %><profile:eventSource profile:eventSourceAddress="${it.eventSource}" profile:enabled="${it.enabled}"><profile:eventPointDataQuery><profile:eventIdentity><profile:eventName profile:literal="${it.eventName}"/></profile:eventIdentity><profile:eventCorrelation><profile:localTransactionId profile:sourceOfId="automatic"/><profile:parentTransactionId profile:sourceOfId="automatic"/><profile:globalTransactionId profile:sourceOfId="automatic"/></profile:eventCorrelation><profile:eventFilter profile:queryText="true()"/><profile:eventUOW profile:unitOfWork="messageFlow"/></profile:eventPointDataQuery><profile:applicationDataQuery></profile:applicationDataQuery><profile:bitstreamDataQuery profile:bitstreamContent="${it.bitstreamContent}" profile:encoding="${it.bitstreamEncoding}"/></profile:eventSource><% } %></profile:monitoringProfile>'''

    static String newProfile(def inputSources, def outputSources) {
        GStringTemplateEngine engine = new GStringTemplateEngine()

        def expandoSources = []
        inputSources.each { key, value ->
            expandoSources.add(new Expando(eventSource: key, eventName: value, enabled: true, bitstreamEncoding: BitstreamEncoding.base64Binary, bitstreamContent: BitstreamContent.all))
        }
        outputSources.each { key, value ->
            expandoSources.add(new Expando(eventSource: key, eventName: value, enabled: false, bitstreamEncoding: BitstreamEncoding.base64Binary, bitstreamContent: BitstreamContent.all))
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