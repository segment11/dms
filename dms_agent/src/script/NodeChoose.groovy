package script

import com.alibaba.fastjson.JSON
import model.json.ContainerResourceAsk

Map params = super.binding.getProperty('params') as Map
String leftResourceListJson = params.leftResourceListJson

def arr = JSON.parseArray(leftResourceListJson, ContainerResourceAsk)
List<String> list = arr.sort { -it.weight }.collect { it.nodeIp }
[list: list]

