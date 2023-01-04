package script

import org.hyperic.sigar.DirUsage
import org.hyperic.sigar.FileSystem
import org.hyperic.sigar.Sigar

Sigar sigar = super.binding.getProperty('sigar') as Sigar
Map params = super.binding.getProperty('params') as Map

String dirs = params.dirs
if (dirs) {
    Map<String, DirUsage> x = [:]
    dirs.split(',').each {
        String fileLocal = it
        if (new File(fileLocal).isDirectory()) {
            x[fileLocal] = sigar.getDirUsage(fileLocal)
        }
    }
    return x
}

def r = [:]
sigar.fileSystemList.each { FileSystem it ->
    r[it.dirName] = sigar.getFileSystemUsage(it.dirName)
}
r
