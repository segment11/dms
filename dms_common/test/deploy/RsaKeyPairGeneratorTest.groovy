package deploy

import spock.lang.Specification

class RsaKeyPairGeneratorTest extends Specification {
    def 'test all'() {
        given:
        def one = RsaKeyPairGenerator.generate()

        expect:
        one.privateKeyBase64 != null
        one.publicKeyBase64 != null
    }
}
