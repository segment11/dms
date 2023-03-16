package script

import com.github.dockerjava.api.DockerClient
import org.hyperic.sigar.Sigar
import transfer.ContainerInfo
import transfer.ContainerInspectInfo

import static common.ContainerHelper.getPidFromProcess
import static common.ContainerHelper.isProcess

Sigar sigar = super.binding.getProperty('sigar') as Sigar
DockerClient docker = super.binding.getProperty('docker') as DockerClient
Map params = super.binding.getProperty('params') as Map
String id = params.id
if (!id) {
    return [error: 'id required']
}

def r = new ContainerInspectInfo()
r.id = id

if (isProcess(id)) {
    int pid = getPidFromProcess(id)
    try {
        r.procArgs = sigar.getProcArgs(pid as long)
        def mem = sigar.getProcMem(pid as long)
        def procMem = new ContainerInspectInfo.OneProcMem()
        procMem.size = mem.size
        procMem.resident = mem.resident
        procMem.share = mem.share
        procMem.minorFaults = mem.minorFaults
        procMem.majorFaults = mem.majorFaults
        procMem.pageFaults = mem.pageFaults
        r.procMem = procMem

        def state = new ContainerInspectInfo.ContainerStateInfo()
        state.status = 'running'
        state.running = true
        r.state = state
    } catch (Exception e) {
        // ignore
        def state = new ContainerInspectInfo.ContainerStateInfo()
        state.status = 'exited'
        state.running = false
        r.state = state
    }
    [container: r]
} else {
    def a = docker.inspectContainerCmd(id).exec()
    r.args = a.args
    r.created = a.created
    r.driver = a.driver
    r.execDriver = a.execDriver

    r.hostnamePath = a.hostnamePath
    r.hostsPath = a.hostsPath
    r.logPath = a.logPath

    r.sizeRootFs = a.sizeRootFs
    r.imageId = a.imageId
    r.name = a.name
    r.restartCount = a.restartCount

    r.networkMode = a.hostConfig.networkMode

    r.cmd = a.config.cmd
    r.entrypoint = a.config.entrypoint
    r.env = a.config.env
    r.user = a.config.user
    r.workingDir = a.config.workingDir

    def state = new ContainerInspectInfo.ContainerStateInfo()
    state.status = a.state.status
    state.running = a.state.running
    r.state = state

    if (a.hostConfig.portBindings) {
        r.ports = []
        a.hostConfig.portBindings.bindings.each { expose, b ->
            def p = new ContainerInfo.PortMapping()
            def spec = b[0].hostPortSpec
            p.privatePort = spec.contains('-') ? spec.split(/\-/)[0] as int : spec as int
            p.publicPort = expose.port
            p.type = expose.protocol.toString()
            p.ip = b[0].hostIp
            r.ports << p
        }
    }

    if (a.mounts) {
        r.mounts = a.mounts.collect { mount ->
            def m = new ContainerInfo.Mount()
            m.name = mount.name
            m.source = mount.source
            m.destination = mount.destination.path
            m.driver = mount.driver
            m.mode = mount.mode
            m.rw = mount.RW
            m
        }
    }
}

r