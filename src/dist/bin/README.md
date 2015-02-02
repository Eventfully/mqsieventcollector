# INTRO
This folder contains the scripts for running the different utilities.

# EventCollector

## Prerequisites
1.  Monitoring events enabled on a WMB 6.1 FP3+ broker/bus
2.  Subscription to a queue that the eventcollector can read.
3.  The collector always run as a WMQ client so it doesn't matter if the queuemanager
is local or remote.

## Configure operation
Configuration is in the config/application.properties for the application

## Configure logging
To configure the logging the config/log4j.properties is used.

## Start
Start the eventcollector through the startcollector.bat script


# mqsibrokerutil
This is a script with the primarily purpose of generating monitoringprofiles and supporting scripts.

## Usage
`usage: mqsibrokerutil.groovy [options]`

    Available options (use -h for help):
     -a,--auto                      Auto create a monitoring profile for the
                                    flow in eg specified by -m -e
     -b,--brokerfile <BROKERFILE>   The name of the .broker connection file
     -c,--create                    Create profile
     -e,--eg <EG>                   The execution group/integration server
                                    name
     -f,--flow <flow>               The message flow name, note that you must
                                    specify eg as well using this option.
     -h,--help                      Show help
     -i,--ip <IP>                   The ip or hostname of the broker
     -p,--port <PORT>               The port of the broker mq listener
     -q,--qmgr <QMGR>               The broker queuemanager name
     -s,--sources <SOURCES>         eventsources=eventname key-value for
                                    generating events, mandatory for command
                                    profile, can be repeated multiple times.
     -w,--workdir <WORKDIR>         The working directory for generating
                                    output files, defaults to current
                                    directory

    Information provided via above options is used to generate printed string.

## Examples

### List execution groups on broker IB9NODE
`mqsibrokerutil.bat -b config/IB9NODE.broker`

    Connect using .broker file: config/IB9NODE.broker
    Connected to: 'IB9NODE'
            EG 'default' is running
            EG 'test' is stopped
            
### List messageflows in EG default
`mqsibrokerutil.bat -b config/IB9NODE -e default`

    Connect using .broker file: config/IB9NODE.broker
    Connected to: 'IB9NODE'
                    MF: 'MessageFlow1' in EG:'default' is running
                    MF: 'MessageFlow2' in EG:'default' is stopped

### Display messageflow details of messageflow MessageFlow1
`mqsibrokerutil.bat -b config/IB9NODE -e default -f MessageFlow1`

### Auto-create profile scripts for messageflow
`mqsibrokerutil.bat -b config/IB9NODE -e default -f MessageFlow1 -c -a`

