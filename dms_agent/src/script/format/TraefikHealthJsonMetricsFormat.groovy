package script.format

import support.ToJson

String body = super.binding.getProperty('body')

HashMap one = ToJson.read(body, HashMap)
Map status = one.total_status_code_count as Map

def r = [:]
r.status500 = status.get('500')
r.average_response_size = one.average_response_size
r.average_response_time = one.average_response_time.toString().replace('ms', '')
r