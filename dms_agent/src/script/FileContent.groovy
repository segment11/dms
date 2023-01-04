package script

Map params = super.binding.getProperty('params') as Map

String pathLocal = params.path
def f = new File(pathLocal)
if (!f.exists()) {
    return [content: '']
}

if (f.isFile()) {
    return [content: f.canRead() ? f.text : '']
} else {
    if (!f.canRead()) {
        return [content: '']
    } else {
        def list = f.listFiles()
        def sortedList = list.sort { a, b ->
            a.name <=> b.name
        }
        return [content: sortedList.collect { File it ->
            it.name.padRight(50, ' ') + ' - ' + (it.isDirectory() ? 'dir' : 'file')
        }.join("\r\n")]
    }
}