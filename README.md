# mqsieventcollector
An simple collector of IBM mqsi events.

# Pre requisites 

## WebSphere MQ
You will need a local installation of WebSphere MQ and that the 
environment variable "MQ_JAVA_LIB_DIR" is set.

## Gradle
The project is built using Gradle. As long as you have a JRE on your path you can actually use the 
bundled gradle wrapper instead (gradlew on Linux or gradlew.bat on Windows). It will download an embedded
gradle jar-file and run so you don't need to install anything.

## Test using IBM Integration Bus V9
The simplest way to enable and test this using for instance IIB V9 is to start from the 
Samples and Tutorials -> Monitoring -> WebSphere Business Monitor sample
Import it and then change the configured events for "Transaction Start" and "Transaction END"
Make sure they "Include bitstream data in payload" with content "All" and Encoding "base64Binary"

Follow the instructions in it for setting up subscription and activating the events.
$SYS/Broker/IB9NODE/Monitoring/test/TotalPurchaseOrderFlow
mqsichangeflowmonitoring IB9NODE -e test -f TotalPurchaseOrderFlow -c active



Note that a Developer Edition of IIB is readily available through IBM's website.
