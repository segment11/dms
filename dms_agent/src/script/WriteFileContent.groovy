package script

import org.apache.commons.io.FileUtils

Map params = super.binding.getProperty('params') as Map
String filePath = params.filePath as String
String fileContent = params.fileContent as String

def file = new File(filePath)
if (!file.exists()) {
    def parentFile = file.parentFile
    if (!parentFile.exists()) {
        FileUtils.forceMkdir(parentFile)
    }
}

file.text = fileContent

[flag: true]
