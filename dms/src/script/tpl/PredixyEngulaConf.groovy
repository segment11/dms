package script.tpl

def appId = super.binding.getProperty('appId') as int
def nodeIp = super.binding.getProperty('nodeIp') as String

// , split
def servers = super.binding.getProperty('servers') as String
def dbPassword = super.binding.getProperty('dbPassword') as String
def logDir = super.binding.getProperty('logDir') as String
def port = super.binding.getProperty('port') as int

def serverArr = servers.split(',')
def pre = ''.padLeft(8, ' ')
def x = serverArr.collect {
    def oneServer = it.toString().trim()
    pre + '+ ' + oneServer
}.join('\n')

"""
Name PredixyDmsApp_${appId}
Bind ${nodeIp}:${port}

WorkerThreads 8
ClientTimeout 300
Log ${logDir}/predixy.log
LogRotate 1d

## LogLevelSample, output a log every N
## all level sample can change online by CONFIG SET LogXXXSample N
LogVerbSample 0
LogDebugSample 0
LogInfoSample 10000
LogNoticeSample 1
LogWarnSample 1
LogErrorSample 1


################################### AUTHORITY ##################################
Authority {
    Auth "${dbPassword}" {
        Mode write
    }
}

################################### SERVERS ####################################
ClusterServerPool {
    MasterReadPriority 60
    StaticSlaveReadPriority 50
    DynamicSlaveReadPriority 50
    RefreshInterval 1
    ServerTimeout 1
    ServerFailureLimit 10
    ServerRetryTimeout 1
    KeepAlive 120
    Servers {
${x}
    }
}

################################### LATENCY ####################################
LatencyMonitor all {
    Commands {
        + all
        - blpop
        - brpop
        - brpoplpush
    }
    TimeSpan {
        + 100
        + 200
        + 300
        + 400
        + 500
        + 600
        + 700
        + 800
        + 900
        + 1000
        + 1200
        + 1400
        + 1600
        + 1700
        + 1800
        + 2000
        + 2500
        + 3000
        + 3500
        + 4000
        + 4500
        + 5000
        + 6000
        + 7000
        + 8000
        + 9000
        + 10000
    }
}

LatencyMonitor get {
    Commands {
        + get
    }
    TimeSpan {
        + 100
        + 200
        + 300
        + 400
        + 500
        + 600
        + 700
        + 800
        + 900
        + 1000
    }
}

LatencyMonitor set {
    Commands {
        + set
        + setnx
        + setex
    }
    TimeSpan {
        + 100
        + 200
        + 300
        + 400
        + 500
        + 600
        + 700
        + 800
        + 900
        + 1000
    }
}

LatencyMonitor blist {
    Commands {
        + blpop
        + brpop
        + brpoplpush
    }
    TimeSpan {
        + 1000
        + 2000
        + 3000
        + 4000
        + 5000
        + 6000
        + 7000
        + 8000
        + 9000
        + 10000
        + 20000
        + 30000
        + 40000
        + 50000
        + 60000
        + 70000
        + 80000
        + 90000
        + 100000
    }
}
"""
