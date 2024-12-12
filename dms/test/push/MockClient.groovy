package push

import com.segment.common.http.Invoker

def instance = ClientRequestHandleLoop.instance

instance.clientId = 'test-local'
instance.cloudProvider = 'aws'
instance.region = 'ap-east-1'

def i = new Invoker()
i.serverAddr = 'http://localhost:5040'
instance.invoker = i

instance.eventHandler = { ClientAction clientAction ->
    println 'action: ' + clientAction.action + ', data: ' + clientAction.data
    [currentDate: new Date().format('yyyy-MM-dd HH:mm:ss')]
}

instance.start(1000)

Thread.sleep(2000)
instance.updateInfo('test-vpc-id', 'test-instance-id')

Thread.sleep(1000 * 60 * 5)
instance.stop()