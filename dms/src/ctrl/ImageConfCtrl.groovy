package ctrl

import auth.User
import model.*
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance

h.group('/image/config') {
    h.before(~/\/[^\/]+\/delete\/.*/, { req, resp ->
        User u = req.attr('user') as User
        if (!u.isImageManager()) {
            resp.halt(403, 'not a docker manager')
        }
    }, 10).before(~/\/[^\/]+\/update/, { req, resp ->
        User u = req.attr('user') as User
        if (!u.isImageManager()) {
            resp.halt(403, 'not a docker manager')
        }
    }, 10)

    h.group('/env') {
        h.get('/list') { req, resp ->
            def p = req.param('pageNum')
            int pageNum = p ? p as int : 1
            final int pageSize = 10

            def keyword = req.param('keyword')
            new ImageEnvDTO().noWhere().where(keyword as boolean, '(image_name like ?) or (name like ?)',
                    '%' + keyword + '%', '%' + keyword + '%').listPager(pageNum, pageSize)
        }.delete('/delete') { req, resp ->
            def id = req.param('id')
            assert id
            new ImageEnvDTO(id: id as int).delete()
            [flag: true]
        }.post('/update') { req, resp ->
            def one = req.bodyAs(ImageEnvDTO)
            assert one.name && one.imageName && one.env
            if (one.id) {
                one.update()
                return [id: one.id]
            } else {
                def id = one.add()
                return [id: id]
            }
        }
    }

    h.group('/port') {
        h.get('/list') { req, resp ->
            def p = req.param('pageNum')
            int pageNum = p ? p as int : 1
            final int pageSize = 10

            def keyword = req.param('keyword')
            new ImagePortDTO().noWhere().where(keyword as boolean, '(image_name like ?) or (name like ?)',
                    '%' + keyword + '%', '%' + keyword + '%').listPager(pageNum, pageSize)
        }.delete('/delete') { req, resp ->
            def id = req.param('id')
            assert id
            new ImagePortDTO(id: id as int).delete()
            [flag: true]
        }.post('/update') { req, resp ->
            def one = req.bodyAs(ImagePortDTO)
            assert one.name && one.imageName && one.port
            if (one.id) {
                one.update()
                return [id: one.id]
            } else {
                def id = one.add()
                return [id: id]
            }
        }
    }

    h.group('/tpl') {
        h.get('/list') { req, resp ->
            def p = req.param('pageNum')
            int pageNum = p ? p as int : 1
            final int pageSize = 10

            def keyword = req.param('keyword')
            new ImageTplDTO().noWhere().where(keyword as boolean, '(image_name like ?) or (name like ?)',
                    '%' + keyword + '%', '%' + keyword + '%').listPager(pageNum, pageSize)
        }.delete('/delete') { req, resp ->
            def id = req.param('id')
            assert id
            new ImageTplDTO(id: id as int).delete()
            [flag: true]
        }.post('/update') { req, resp ->
            def one = req.bodyAs(ImageTplDTO)
            assert one.name && one.imageName
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

    h.group('/volume') {
        h.get('/list') { req, resp ->
            def p = req.param('pageNum')
            int pageNum = p ? p as int : 1
            final int pageSize = 10

            def clusterId = req.param('clusterId')
            assert clusterId

            def keyword = req.param('keyword')
            new NodeVolumeDTO().where('cluster_id = ?', clusterId).
                    where(keyword as boolean, '(image_name like ?) or (name like ?)',
                            '%' + keyword + '%', '%' + keyword + '%').listPager(pageNum, pageSize)
        }.delete('/delete') { req, resp ->
            def id = req.param('id')
            assert id
            new NodeVolumeDTO(id: id as int).delete()
            [flag: true]
        }.post('/update') { req, resp ->
            def one = req.bodyAs(NodeVolumeDTO)
            assert one.name && one.clusterId && one.dir
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

    h.group('/registry') {
        h.get('/list') { req, resp ->
            new ImageRegistryDTO().noWhere().list()
        }.delete('/delete') { req, resp ->
            def id = req.param('id')
            assert id
            new ImageRegistryDTO(id: id as int).delete()
            [flag: true]
        }.post('/update') { req, resp ->
            def one = req.bodyAs(ImageRegistryDTO)
            assert one.name && one.url
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
}