package ctrl

import auth.User
import com.segment.common.Conf
import common.Utils
import deploy.DeploySupport
import deploy.OneCmd
import model.DeployFileDTO
import model.NodeDTO
import model.NodeKeyPairDTO
import org.segment.web.handler.ChainHandler
import org.slf4j.LoggerFactory
import server.AgentCaller
import server.InMemoryAllContainerManager
import server.InMemoryCacheSupport

def h = ChainHandler.instance

def log = LoggerFactory.getLogger(this.getClass())

h.get('/deploy/node-file/list') { req, resp ->
    User u = req.attr('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    def clusterId = req.param('clusterId')
    assert clusterId

    def p = req.param('pageNum')
    int pageNum = p ? p as int : 1
    final int pageSize = 10
    def pager = new NodeKeyPairDTO(clusterId: clusterId as int).
            orderBy('ip asc').
            listPager(pageNum, pageSize)

    def instance = InMemoryAllContainerManager.instance
    def dat = Utils.getNodeAliveCheckLastDate(3)

    List<NodeDTO> nodeList
    if (pager.list) {
        nodeList = new NodeDTO().whereIn('ip', pager.list.collect { it.ip }).list()
    }

    def pager2 = pager.transfer {
        def one = it as NodeKeyPairDTO
        def d = instance.getHeartBeatDate(one.ip)
        boolean isOk = d && d > dat

        def tags = nodeList?.find { node -> node.ip == one.ip }?.tags
        [id         : one.id,
         ip         : one.ip,
         user       : one.user,
         sshPort    : one.sshPort,
         updatedDate: one.updatedDate,
         tagList    : tags ? tags.split(',') : [],
         isOk       : isOk]
    }

    def keyword = req.param('keyword')
    def deployFileList = new DeployFileDTO().noWhere().
            where(keyword as boolean, '(dest_path like ?) or (local_path like ?)',
                    '%' + keyword + '%', '%' + keyword + '%').list()

    [pager: pager2, deployFileList: deployFileList]
}.post('/deploy/begin') { req, resp ->
    User u = req.attr('user') as User
    if (!u.isAdmin()) {
        resp.halt(403, 'not admin')
    }

    Map item = req.bodyAs()
    List<String> nodeIpList = item.get('nodeIpList') as List<String>
    int targetId = item.get('targetId') as int

    def targetOne = new DeployFileDTO(id: targetId).one()
    if (!targetOne) {
        return [flag: false, message: 'File not exists!']
    }

    OneCmd oneCmd
    def cmd = targetOne.initCmd
    if (cmd) {
        String changedCmd
        if (cmd.contains('$destPath')) {
            changedCmd = cmd.replace('$destPath', targetOne.destPath)
        } else {
            changedCmd = cmd
        }

        oneCmd = OneCmd.simple(changedCmd)
    }

    for (nodeIp in nodeIpList) {
        def kp = new NodeKeyPairDTO(ip: nodeIp).one()
        if (!kp) {
            return [flag: false, message: 'Node is not init ssh connect: ' + nodeIp]
        }

        def one = InMemoryCacheSupport.instance.oneCluster(kp.clusterId)
        String proxyNodeIp = one.globalEnvConf.proxyNodeIp

        if (proxyNodeIp && proxyNodeIp != kp.ip) {
            def r = AgentCaller.instance.doSshCopy(kp, targetOne.localPath, targetOne.destPath,
                    null, 30000, [isOverwrite: true])
            log.info r ? r.toString() : '...'
        } else {
            String localFilePath = Conf.isWindows() ? new File(targetOne.localPath).absolutePath : targetOne.localPath
            DeploySupport.instance.send(kp, localFilePath, targetOne.destPath)
        }

        if (oneCmd) {
            if (proxyNodeIp && proxyNodeIp != kp.ip) {
                def r = AgentCaller.instance.doSshExec(kp, oneCmd.cmd)
                log.info r ? r.toString() : '...'
            } else {
                DeploySupport.instance.exec(kp, oneCmd)
                if (!oneCmd.ok()) {
                    return [flag: false, message: oneCmd.toString()]
                }
                oneCmd.clear()
            }
        }
    }

    [flag: true]
}

h.group('/deploy-file') {
    h.get('/list') { req, resp ->
        def keyword = req.param('keyword')
        def deployFileList = new DeployFileDTO().noWhere().
                where(keyword as boolean, '(dest_path like ?) or (local_path like ?)',
                        '%' + keyword + '%', '%' + keyword + '%').list()
        [deployFileList: deployFileList]
    }.delete('/delete') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def id = req.param('id')
        assert id
        new DeployFileDTO(id: id as int).delete()
        [flag: true]
    }.post('/update') { req, resp ->
        User u = req.attr('user') as User
        if (!u.isAdmin()) {
            resp.halt(403, 'not admin')
        }

        def one = req.bodyAs(DeployFileDTO)
        assert one.localPath && one.destPath

        // check if local file exists
        def f = new File(one.localPath)
        if (!f.exists()) {
            return [flag: false, message: 'File not exists: ' + one.localPath]
        }
        if (f.isDirectory()) {
            return [flag: false, message: 'File should not be a directory: ' + one.localPath]
        }
        one.fileLen = f.length()
        one.updatedDate = new Date()

        if (one.id) {
            one.update()
            return [id: one.id]
        } else {
            def id = one.add()
            return [id: id]
        }
    }
}