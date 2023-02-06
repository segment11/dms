package script

import org.apache.commons.io.FileUtils

Map params = super.binding.getProperty('params') as Map
String dir = params.dir as String

for (one in dir.split(',')) {
    FileUtils.forceMkdir(new File(one))
}

[flag: true]
