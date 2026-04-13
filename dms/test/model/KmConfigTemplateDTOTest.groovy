package model

import model.json.ConfigItems
import model.json.KVPair
import org.segment.d.D
import org.segment.d.Ds
import spock.lang.Specification
import support.DmsTestDbSupport

class KmConfigTemplateDTOTest extends Specification {
    private Ds ds
    private D d

    def setup() {
        ds = DmsTestDbSupport.newCachedDmsH2Ds()
        d = DmsTestDbSupport.newMysqlStyleD(ds)
        DmsTestDbSupport.execStatements(d, '''
create table km_config_template
(
    id           int auto_increment primary key,
    name         varchar(50),
    des          varchar(200),
    config_items text,
    updated_date timestamp default current_timestamp
)
''')
    }

    def cleanup() {
        ds?.closeConnect()
        DmsTestDbSupport.cleanupCachedDmsDs()
    }

    def 'crud persists config items json field for km config templates'() {
        given:
        def created = new KmConfigTemplateDTO(
                name: 'km-tpl-1',
                des: 'test template',
                configItems: new ConfigItems(
                        items: [new KVPair<String>(key: 'host', value: '10.0.0.1'), new KVPair<String>(key: 'port', value: '6379')]
                ),
                updatedDate: new Date()
        )

        when:
        def id = created.add()

        then:
        id > 0

        when:
        def loaded = new KmConfigTemplateDTO(id: id).one()

        then:
        loaded.name == 'km-tpl-1'
        loaded.des == 'test template'
        loaded.configItems.items.size() == 2
        loaded.configItems.items[0].key == 'host'
        loaded.configItems.items[0].value == '10.0.0.1'
        loaded.configItems.items[1].key == 'port'
        loaded.configItems.items[1].value == '6379'

        when:
        loaded.name = 'km-tpl-1-updated'
        loaded.des = 'updated template'
        loaded.configItems = new ConfigItems(
                items: [new KVPair<String>(key: 'timeout', value: '30')]
        )
        def updated = loaded.update()

        then:
        updated == 1

        when:
        def reloaded = new KmConfigTemplateDTO(id: id).one()

        then:
        reloaded.name == 'km-tpl-1-updated'
        reloaded.des == 'updated template'
        reloaded.configItems.items.size() == 1
        reloaded.configItems.items[0].key == 'timeout'
        reloaded.configItems.items[0].value == '30'

        when:
        def deleted = new KmConfigTemplateDTO(id: id).delete()

        then:
        deleted == 1
        new KmConfigTemplateDTO(id: id).one() == null
    }
}
