package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import model.json.GwEntryPoints
import model.json.GwService
import model.json.KVPair
import org.segment.d.json.JSONFiled

@CompileStatic
@ToString(includeNames = true, includeSuper = false)
class GwRouterDTO extends BaseRecord<GwRouterDTO> {
    @CompileStatic
    @ToString(includeNames = true)
    class Tls implements JSONFiled {
        String options
        String certResolver
        List<Domain> domains = []

        boolean asBoolean() {
            options || certResolver || domains
        }
    }

    @CompileStatic
    @ToString(includeNames = true)
    class Domain {
        String main
        List<String> sans = []
    }

    @CompileStatic
    @ToString(includeNames = true)
    class Failover implements JSONFiled {
        String service
        String fallback

        boolean asBoolean() {
            service && fallback
        }
    }

    Integer id

    Integer clusterId

    String name

    String des

    String rule

    GwService service

    Tls tls

    Failover failover

    GwEntryPoints entryPoints

    Integer priority

    Date createdDate

    Date updatedDate

    List<KVPair<String>> toKVList() {
        // refer to https://doc.traefik.io/traefik/routing/routers/
        // https://doc.traefik.io/traefik/routing/providers/kv/
        final String prefix = '/http/routers/' + name + '/'

        List<KVPair<String>> kvList = []
        kvList << new KVPair(prefix + 'rule', rule)
        kvList << new KVPair(prefix + 'service', service.name)

        if (priority) {
            kvList << new KVPair(prefix + 'priority', priority.toString())
        }

        if (entryPoints) {
            entryPoints.entryPoints.eachWithIndex { String entryPoint, int i ->
                kvList << new KVPair(prefix + 'entryPoints/' + i, entryPoint)
            }
        }

        if (tls) {
            kvList << new KVPair(prefix + 'tls', 'true')
            if (tls.options) {
                kvList << new KVPair(prefix + 'tls/options', tls.options)
            }
            if (tls.certResolver) {
                kvList << new KVPair(prefix + 'tls/certResolver', tls.certResolver)
            }
            if (tls.domains) {
                tls.domains.eachWithIndex { Domain domain, int i ->
                    kvList << new KVPair(prefix + 'tls/domains/' + i + '/main', domain.main)
                    if (domain.sans) {
                        domain.sans.eachWithIndex { String san, int j ->
                            kvList << new KVPair(prefix + 'tls/domains/' + i + '/sans/' + j, san)
                        }
                    }
                }
            }
        }

        if (failover) {
            kvList << new KVPair('/http/services/' + name + '/failover/service', failover.service)
            kvList << new KVPair('/http/services/' + name + '/failover/fallback', failover.fallback)
        }

        if (service) {
            kvList.addAll(service.toKVList())
        }

        kvList
    }
}