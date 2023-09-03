package support

import com.segment.common.Conf
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import model.AgentScriptDTO
import org.segment.d.D

@CompileStatic
@Slf4j
class LocalGroovyScriptLoader {
    static void loadWhenFirstStart(boolean forceUpdate = false) { def agentScriptOne = new AgentScriptDTO().queryFields('id').noWhere().one()
        if (!agentScriptOne || forceUpdate) {
            load()
        }
    }

    static void load(String scriptNameGiven = null) {
        def file = new File(Conf.instance.projectPath('/../dms_agent/src/script'))
        if (!file.exists()) {
            file = new File(Conf.instance.projectPath('/dms_agent/src/script'))
        }
        def scriptFileList = file.listFiles()
        scriptFileList.each { File f ->
            if (f.isDirectory()) {
                return
            }
            String scriptName = D.toUnderline(f.name.split(/\./)[0]).replaceAll('_', ' ')
            if (scriptNameGiven && scriptNameGiven != scriptName) {
                return
            }
            def one = new AgentScriptDTO(name: scriptName).queryFields('id').one()
            if (one) {
                one.des = scriptName
                one.content = f.text
                one.updatedDate = new Date()
                one.update()
                log.info 'done update script - {}', scriptName
            } else {
                def add = new AgentScriptDTO()
                add.name = scriptName
                add.des = scriptName
                add.content = f.text
                add.updatedDate = new Date()
                add.add()
                log.info 'done add script - {}', scriptName
            }
        }
    }
}
