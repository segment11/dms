package ctrl

import auth.User
import model.AppDTO
import model.NodeVolumeDTO
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.get('/help/table/volume/clean') { req, resp ->
    User u = req.attr('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }
    
    def clusterId = req.param('clusterId')
    assert clusterId

    def appList = new AppDTO(clusterId: clusterId as int).list()

    def hostDirList = appList.findAll { it.conf?.dirVolumeList }.collect { AppDTO it ->
        it.conf.dirVolumeList.collect { dirVolume ->
            dirVolume.dir
        }
    }.flatten()

    def list = new NodeVolumeDTO().whereNotIn('dir', hostDirList, true).
            queryFields('id').list()
    int number = new NodeVolumeDTO().whereIn('id', list.collect { it.id }).deleteAll()
    [flag: true, number: number]
}
