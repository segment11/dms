package plugin.callback

import groovy.transform.CompileStatic
import model.AppDTO
import transfer.ContainerInfo

@CompileStatic
interface ConfigFileReloaded {
    void reloaded(AppDTO app, ContainerInfo x, List<String> changedDistList)
}
