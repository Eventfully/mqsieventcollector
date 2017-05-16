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
            ap(longOpt: 'app', 'The application name, note that you must specify eg as well using this option.', args: 1, argName: 'app', required: false)
            a(longOpt: 'auto', 'Auto create a monitoring profile for the flow in eg specified by -m -e, default is enabling MQInputNodes with transaction.start with payload and configured but disabled MQOutputNodes terminal.in with payload.', args: 0, argName: 'AUTO', required: false)
            t(longOpt: 'transfer', 'If the created profiles should be transferd to remote location, with ssh. Configuration from application.properties', args: 1, argName: 'TRANSFER', required: false)
			u(longOpt: 'UNIX', 'Default all files are saved as .cmd, use this flag to save as .sh', args: 0, argName: 'UNIX', required: false)
			
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
String applicationName = opt.ap ?: null
def sources = opt.s ?: null
String workDirName = opt.w ?: System.properties.getProperty('user.dir')
boolean autoCreateProfile = opt.a
boolean environment = opt.u
String transfer = opt.t ?:null

BrokerConnectionParameters bcp

println "INFO: hostname: $hostname port: $port" 
if (brokerFile) {
    println "Connect using .broker file: " + brokerFile
    bcp = new IntegrationNodeConnectionParameters(brokerFile)

} else if (hostname && port && !qmgr) {
    println "Connect using hostname and webgui port arguments"
    bcp = new IntegrationNodeConnectionParameters(hostname, port)
	
} else if (hostname && port && qmgr) {
	println "Connect using hostname, port and queue manager arguments"
    bcp = new MQBrokerConnectionParameters(hostname, port, qmgr)
	
}

if (command == Command.profile) {
    if (!(sources || autoCreateProfile)) {
        println "For the create profile option you must either specify the -a or -s 'eventSrc=eventName' options as well."
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
    if (msgFlowName && egName && !applicationName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        MessageFlowProxy mfp = egp.getMessageFlowByName(msgFlowName)
        displayFlowInExecutionGroup(egp, msgFlowName)
        return displayFlowDetails(mfp)
        
      } else if (applicationName && msgFlowName && egName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        ApplicationProxy app = egp.getApplicationByName(applicationName)
        displayAppFlowInExecutionGroup(egp, applicationName)
        return displayAppFlowDetails(app, msgFlowName)
    
    } else if (applicationName && egName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        ApplicationProxy app = egp.getApplicationByName(applicationName)
        displayAppFlowInExecutionGroup(egp, applicationName)
        return displayAppDetails(app, egp)
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
    if (sources && !autoCreateProfile) {
	    def sourceMap = opt.ss.toSpreadMap()
        sourceMap.each { k, v -> println k + " " + v }
        profile = MonitoringProfileFactory.newSourceProfile(sourceMap)
	} else if (sources && autoCreateProfile && msgFlowName && egName && applicationName) {
		ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        ApplicationProxy app = egp.getApplicationByName(applicationName)
        MessageFlowProxy mfp = app.getMessageFlowByName(msgFlowName)
		def sourceMap = createFlowInputAllEventSourcesMap(mfp)
		profile = MonitoringProfileFactory.newSourceProfile(sourceMap)
		
    } else if (autoCreateProfile && msgFlowName && egName && !applicationName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        MessageFlowProxy mfp = egp.getMessageFlowByName(msgFlowName)
        def sourceMQInputMap = createFlowInputMQEventSourcesMap(mfp)
        Map sourceMQOutputMap = createFlowOutputMQEventSourcesMap(mfp)
        outputEventSources = sourceMQOutputMap.collect { it.key }.join(',')
        profile = MonitoringProfileFactory.newProfile(sourceMQInputMap, sourceMQOutputMap)
    } else if (autoCreateProfile && msgFlowName && egName && applicationName) {
        ExecutionGroupProxy egp = proxy.getExecutionGroupByName(egName)
        assert egp
        ApplicationProxy app = egp.getApplicationByName(applicationName)
        MessageFlowProxy mfp = app.getMessageFlowByName(msgFlowName)
        def sourceMQInputMap = createFlowInputMQEventSourcesMap(mfp)
        Map sourceMQOutputMap = createFlowOutputMQEventSourcesMap(mfp)
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
    if (applicationName) {
      createMqsiCommandFilesForApplicationsUsingTemplates(outputDir, brokerName, egName, msgFlowName, xmlProfileFileName, outputEventSources, applicationName, environment)
    } else {
      createMqsiCommandFilesUsingTemplates(outputDir, brokerName, egName, msgFlowName, xmlProfileFileName, outputEventSources, environment)
    }
    if (transfer) {
      transferMonitoring(msgFlowName, transfer, workDirName)
    }

} else {
    println "not implemented yet"
    return
}
def transferMonitoring(String nameOfDir, String password, String sourceDir){
    println "Transfer to remote location"
    def filter = new File('../config/application.properties')
    def props = new java.util.Properties()
    props.load(new FileInputStream(filter))
    def config = new ConfigSlurper().parse(props)

    def transferCommand = config.transfer.user + '@' + config.transfer.host + ':' + config.transfer.dir
    def ant = new AntBuilder()
    ant.scp(
            todir:transferCommand,
            trust: true,
            password: password){
        fileset(dir:sourceDir) {
            include(name: '/' + nameOfDir +'/')
        }
    }

}
def createMqsiCommandFilesUsingTemplates(File outputDir, String brokerName, String egName, String msgFlowName, String profileXmlFileName, String outputEventSources, Boolean type) {
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
                         "enableMQOutputEvents.template",
                         "disableMQOutputEvents.template"]

    String templateDirName = System.properties.getProperty('user.dir') + "/templates"
	def envType = type ? 'sh' : 'cmd'
    templateFiles.each { templateFileName ->
        File templateFile = new File(templateDirName, templateFileName)
        def engine = new GStringTemplateEngine()
        def activeTemplate = engine.createTemplate(templateFile).make(binding)
        File ouputFile = new File(outputDir, templateFileName.replaceFirst("template", envType))
        ouputFile.text = activeTemplate.toString()
    }
}
def createMqsiCommandFilesForApplicationsUsingTemplates(File outputDir, String brokerName, String egName, String msgFlowName, String profileXmlFileName, String outputEventSources, String applicationName, Boolean type) {
    String exportFileName = "%CD%\\exported-" + profileXmlFileName

    def binding = ["brokerName"        : brokerName,
                   "egName"            : egName,
                   "msgFlowName"       : msgFlowName,
                   "applicationName"   : applicationName, 
                   "profileFileName"   : profileXmlFileName,
                   "profileName"       : msgFlowName,
                   "exportFileName"    : exportFileName,
                   "outputEventSources": outputEventSources]

    def templateFiles = ["activateFlowEventsApp.template",
                         "createMonitoringProfileApp.template",
                         "deleteMonitoringProfileApp.template",
                         "exportFlowMonitoringProfileApp.template",
                         "inactivateFlowEventsApp.template",
                         "listAllConfigurableEventsInFlowApp.template",
                         "listAllConfiguredEventsInFlowApp.template",
                         "updateMonitoringProfileApp.template",
                         "enableMQOutputEventsApp.template",
                         "disableMQOutputEventsApp.template"]

    String templateDirName = System.properties.getProperty('user.dir') + "/templates"
	def envType = type ? 'sh' : 'cmd'
	
    templateFiles.each { templateFileName ->
        File templateFile = new File(templateDirName, templateFileName)
        def engine = new GStringTemplateEngine()
        def activeTemplate = engine.createTemplate(templateFile).make(binding)
        File ouputFile = new File(outputDir, templateFileName.replaceFirst("template", envType))
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
    egp.getApplications().each { ApplicationProxy app ->
        println "\t\tAPP: '${app.name}' in EG:'${egp.name}' is ${app.isRunning() ? 'running' : 'stopped'}"
    }
}

def displayFlowInExecutionGroup(ExecutionGroupProxy egp, String msgFlowName) {
    MessageFlowProxy mfp = egp.getMessageFlowByName(msgFlowName)
    assert mfp
    println "\t\tMF: '${mfp.name}' in EG:'${egp.name}' is ${mfp.isRunning() ? 'running' : 'stopped'}"
}

def displayAppFlowInExecutionGroup(ExecutionGroupProxy egp, String applicationName) {
    ApplicationProxy app = egp.getApplicationByName(applicationName)
    assert app
    println "\t\tAPP: '${app.name}' in EG:'${egp.name}' is ${app.isRunning() ? 'running' : 'stopped'}"
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
def displayAppDetails(ApplicationProxy app, ExecutionGroupProxy egp) {
    if (app.isRunning()) {
       
        println "\t\t\t\tFlows in Application"
        app.getMessageFlows().each { MessageFlowProxy mfp ->
         println "\t\tMF: '${mfp.name}' in EG:'${egp.name}' is ${mfp.isRunning() ? 'running' : 'stopped'}"
        }
        
       
    }
}
def displayAppFlowDetails(ApplicationProxy app, String msgFlowName) {
    if (app.isRunning()) {
     
       MessageFlowProxy mfp = app.getMessageFlowByName(msgFlowName)
       println app.getRuntimeProperty('This/monitoring')
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
}
def createFlowInputAllEventSourcesMap(MessageFlowProxy mfp) {
    def eventSourceMap = [:]
    def nodeNames = mfp.nodes
    nodeNames.each { MessageFlowProxy.Node node ->
        
            println "\tFound node: " + node.name + " type: " + node.type 
            eventSourceMap.put("${node.name}.terminal.out", "${node.name}.Start")
            //eventSourceMap.put("${node.name}.transaction.End", "${node.name}.End")
            // eventSourceMap.put("${node.name}.transaction.Rollback", "${node.name}.Rollback")
        
      
    }
	println eventSourceMap
    return eventSourceMap
}


def createFlowInputMQEventSourcesMap(MessageFlowProxy mfp) {
    def eventSourceMap = [:]
    def nodeNames = mfp.nodes
    nodeNames.each { MessageFlowProxy.Node node ->
        if (node.type == 'ComIbmMQInputNode') {
            println "\tFound MQ input node: " + node.name + " with queue: " + node.properties.getProperty('queueName')
            eventSourceMap.put("${node.name}.terminal.out", "${node.name}.Start")
            //eventSourceMap.put("${node.name}.transaction.End", "${node.name}.End")
            // eventSourceMap.put("${node.name}.transaction.Rollback", "${node.name}.Rollback")
        }
      
    }
	println eventSourceMap
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
	println eventSourceMap
    return eventSourceMap
}

enum Command {
    list, profile
}

import groovy.text.GStringTemplateEngine

class MonitoringProfileFactory {

    //static String xmlProfile = '''<profile:monitoringProfile xmlns:profile="http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0.3/monitoring/profile" profile:version="2.0"><% sources.each { %><profile:eventSource profile:eventSourceAddress="${it.eventSource}" profile:enabled="${it.enabled}"><profile:eventPointDataQuery><profile:eventIdentity><profile:eventName profile:literal="${it.eventName}"/></profile:eventIdentity><profile:eventCorrelation><profile:localTransactionId profile:sourceOfId="automatic"/><profile:parentTransactionId profile:sourceOfId="automatic"/><profile:globalTransactionId profile:sourceOfId="automatic"/></profile:eventCorrelation><profile:eventFilter profile:queryText="true()"/><profile:eventUOW profile:unitOfWork="messageFlow"/></profile:eventPointDataQuery><profile:applicationDataQuery></profile:applicationDataQuery><profile:bitstreamDataQuery profile:bitstreamContent="${it.bitstreamContent}" profile:encoding="${it.bitstreamEncoding}"/></profile:eventSource><% } %></profile:monitoringProfile>'''

    static String templateDirName = System.properties.getProperty('user.dir') + "/templates"

    File templateFile = new File(templateDirName, templateFileName)
    static String xmlProfile = new File(templateDirName, "defaultMonitoringProfile.xml").getText("UTF-8")
	static String xmlNewProfile = new File(templateDirName, "defaultNewMonitoringProfile.xml").getText("UTF-8")
	
    static String newProfile(def inputSources, def outputSources) {
        GStringTemplateEngine engine = new GStringTemplateEngine()


        def expandoSources = []
        inputSources.each { key, value ->
            expandoSources.add(new Expando(eventSource: key, eventName: value, enabled: true, bitstreamEncoding: BitstreamEncoding.base64Binary, bitstreamContent: BitstreamContent.all))
        }
        outputSources.each { key, value ->
            expandoSources.add(new Expando(eventSource: key, eventName: value, enabled: true, bitstreamEncoding: BitstreamEncoding.base64Binary, bitstreamContent: BitstreamContent.all))
        }

        def binding = [sources: expandoSources]
        def template = engine.createTemplate(xmlProfile).make(binding)
        return template.toString()
    }
	
	static String newSourceProfile(def inputSources) {
        GStringTemplateEngine engine = new GStringTemplateEngine()

        def expandoSources = []
        inputSources.each { key, value ->
            expandoSources.add(new Expando(eventSource: key, eventName: value, enabled: true, bitstreamEncoding: BitstreamEncoding.base64Binary, bitstreamContent: BitstreamContent.all))
        }
      
        def binding = [sources: expandoSources]
        def template = engine.createTemplate(xmlNewProfile).make(binding)
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