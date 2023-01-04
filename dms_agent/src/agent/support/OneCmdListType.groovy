package agent.support

import com.fasterxml.jackson.core.type.TypeReference
import deploy.OneCmd
import groovy.transform.CompileStatic

@CompileStatic
class OneCmdListType extends TypeReference<List<OneCmd>> {
}