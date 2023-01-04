package common

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Conf {

    static boolean isWindows() {
        System.getProperty('os.name').toLowerCase().contains('windows')
    }

    private String workDir

    Conf resetWorkDir() {
        workDir = new File('.').absolutePath.replaceAll("\\\\", '/').
                replace('/src/', '')
        if (workDir[-1] == '.') {
            workDir = workDir[0..-2]
        }
        put('workDir', workDir)
        this
    }

    String projectPath(String relativePath = '') {
        workDir + relativePath
    }

    Map<String, String> params = [:]

    Conf load() {
        def resource = this.class.classLoader.getResource('conf.properties')
        def confFile = resource ? new File(resource.file) : new File(projectPath('/conf.properties'))
        if (confFile.exists()) {
            confFile.readLines().findAll { it.trim() && !it.startsWith('#') }.each {
                def arr = it.split('=')
                if (arr.length >= 2) {
                    params[arr[0]] = arr[1..-1].join('=')
                }
            }
        }
        this
    }

    String get(String key) {
        params[key]
    }

    String getString(String key, String defaultValue) {
        get(key) ?: defaultValue
    }

    int getInt(String key, int defaultValue) {
        def s = get(key)
        s ? s as int : defaultValue
    }

    boolean isOn(String key) {
        '1' == get(key)
    }

    Conf put(String key, Object value) {
        params[key] = value.toString()
        this
    }

    Conf on(String key) {
        put(key, 1)
    }

    Conf off(String key) {
        put(key, 0)
    }

    @Override
    String toString() {
        params.toString()
    }
}
