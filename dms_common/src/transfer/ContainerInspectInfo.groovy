package transfer

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
class ContainerInspectInfo {
    String[] args
    String created
    String driver
    String execDriver

    String hostnamePath
    String hostsPath
    String logPath
    String id

    Integer sizeRootFs
    String imageId
    String name
    Integer restartCount

    ContainerStateInfo state

    List<ContainerInfo.Mount> mounts
    List<ContainerInfo.PortMapping> ports

    String networkMode

    OneProcMem procMem
    String[] procArgs

    String[] cmd
    String[] entrypoint
    String[] env

    String user
    String workingDir

    @CompileStatic
    @ToString(includeNames = true)
    static class OneProcMem {
        long size
        long resident
        long share
        long minorFaults
        long majorFaults
        long pageFaults
    }

    @CompileStatic
    @ToString(includeNames = true)
    static class ContainerStateInfo {
        String status
        Boolean running
        Boolean paused
        Boolean restarting
        Boolean oomKilled
        Boolean dead
        Long pid
        Long exitCode
        String error
        String startedAt
        String finishedAt
    }
}
