package agent

import com.github.dockerjava.api.model.Container
import com.github.dockerjava.api.model.ContainerPort
import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.hyperic.sigar.CpuPerc
import org.hyperic.sigar.FileSystem
import org.hyperic.sigar.Mem
import org.hyperic.sigar.Sigar
import transfer.ContainerInfo
import transfer.NodeInfo

@CompileStatic
@Slf4j
class AgentHelper {
    static NodeInfo collectNodeSigarInfo(NodeInfo info, Sigar sigar) {
        sigar.cpuPercList.each { CpuPerc it ->
            info.cpuPercList << new NodeInfo.CpuPerc(user: it.user, sys: it.sys, idle: it.idle)
        }
        Mem mem = sigar.mem
        info.mem = new NodeInfo.Mem(total: (mem.total / 1024 / 1024).doubleValue().round(2),
                free: (mem.free / 1024 / 1024).doubleValue().round(2),
                actualFree: (mem.actualFree / 1024 / 1024).doubleValue().round(2),
                actualUsed: (mem.actualUsed / 1024 / 1024).doubleValue().round(2),
                used: (mem.used / 1024 / 1024).doubleValue().round(2),
                usedPercent: mem.usedPercent.round(4))
        if (!Conf.isWindows()) {
            info.loadAverage = sigar.loadAverage
        }
        sigar.fileSystemList.each { FileSystem it ->
            def usage = sigar.getFileSystemUsage(it.dirName)
            info.fileUsageList << new NodeInfo.FileUsage(
                    dirName: it.dirName, total: (usage.total / 1024 / 1024).doubleValue().round(2),
                    free: (usage.free / 1024 / 1024).doubleValue().round(2),
                    usePercent: usage.usePercent * 100)
        }
        info
    }

    static ContainerInfo transfer(Container it) {
        def one = new ContainerInfo()
        one.id = it.id
        one.names = it.names.toList()
        one.image = it.image
        one.imageId = it.imageId
        one.command = it.command
        one.created = it.created
        one.state = it.state
        one.status = it.status
        one.ports = it.ports?.collect { ContainerPort port ->
            def p = new ContainerInfo.PortMapping()
            p.privatePort = port.privatePort
            p.publicPort = port.publicPort
            p.type = port.type
            p.ip = port.ip
            p
        }
        one.labels = it.labels
        one.sizeRw = it.sizeRw
        one.sizeRootFs = it.sizeRootFs
        one.mounts = it.mounts?.collect { mount ->
            def m = new ContainerInfo.Mount()
            m.name = mount.name
            m.source = mount.source
            m.destination = mount.destination
            m.driver = mount.driver
            m.mode = mount.mode
            m.rw = mount.rw
            m.propagation = mount.propagation
            m
        }
        one
    }
}
