package model.server

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.AppDTO
import model.json.AppConf
import model.json.GlobalEnvConf

@CompileStatic
@ToString(includeNames = true)
class CreateContainerConf {
    // copy one
    AppConf conf

    AppDTO app

    String nodeIp

    Integer appId

    Integer jobId

    Integer clusterId

    Integer namespaceId

    Integer instanceIndex

    List<String> nodeIpList

    List<Integer> appIdList

    GlobalEnvConf globalEnvConf

    String imageWithTag

}
