File eventXml= new File('resend.xml')

def configuration = new XmlParser(false, false).parse(eventXml)

def currentFlow = "TotalPurchaseOrderFlow"
def currentEventSrc = "InputOrder.transaction.Start"

configuration."${currentFlow}".find { it.@eventSrc == currentEventSrc }.@resendQueue