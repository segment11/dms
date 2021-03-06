package ctrl

import auth.User
import model.*
import org.segment.web.handler.ChainHandler

def h = ChainHandler.instance


h.group('/image/config') {
    h.before(~/\/[^\/]+\/delete\/.*/) { req, resp ->
        User u = req.session('user')
        if (!u.isImageManager()) {
            resp.halt(403, 'not a docker manager')
        }
    }.before(~/\/[^\/]+\/update/) { req, resp ->
        User u = req.session('user')
        if (!u.isImageManager()) {
            resp.halt(403, 'not a docker manager')
        }
    }

    h.group('/env') {
        h.get('/list') { req, resp ->
            def p = req.param('pageNum')
            int pageNum = p ? p as int : 1
            final int pageSize = 10

            def keyword = req.param('keyword')
            new ImageEnvDTO().where('1=1').where(!!keyword, '(image_name like ?) or (name like ?)',
                    '%' + keyword + '%', '%' + keyword + '%').loadPager(pageNum, pageSize)
        }.delete('/delete/:id') { req, resp ->
            def id = req.param(':id')
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
            new ImagePortDTO().where('1=1').where(!!keyword, '(image_name like ?) or (name like ?)',
                    '%' + keyword + '%', '%' + keyword + '%').loadPager(pageNum, pageSize)
        }.delete('/delete/:id') { req, resp ->
            def id = req.param(':id')
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
            new ImageTplDTO().where('1=1').where(!!keyword, '(image_name like ?) or (name like ?)',
                    '%' + keyword + '%', '%' + keyword + '%').loadPager(pageNum, pageSize)
        }.delete('/delete/:id') { req, resp ->
            def id = req.param(':id')
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
                    where(!!keyword, '(name like ?) or (des like ?)',
                            '%' + keyword + '%', '%' + keyword + '%').loadPager(pageNum, pageSize)
        }.delete('/delete/:id') { req, resp ->
            def id = req.param(':id')
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
            new ImageRegistryDTO().where('1=1').loadList()
        }.delete('/delete/:id') { req, resp ->
            def id = req.param(':id')
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