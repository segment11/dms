package push

import com.segment.common.http.Invoker

def i = new Invoker()
i.serverAddr = 'http://localhost:5040'
def uri = '/dms/push/to-client-event/add'

def data = [:]
data.clientId = 'test-local'
data.action = 'test-action'
data.data = [key: 'value']

println i.request(uri, data, String.class, null, true)
