package deploy

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.jcraft.jsch.KeyPairRSA
import groovy.transform.CompileStatic

@CompileStatic
class RsaKeyPairGenerator {
    @CompileStatic
    static class One {
        String publicKeyBase64
        String privateKeyBase64
    }

    static One generate(int keySize = 2048, String comment = '123456@dms') {
        def one = new One()

        KeyPairRSA kp = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, keySize) as KeyPairRSA

        def os = new ByteArrayOutputStream()
        kp.writePublicKey(os, comment)
        one.publicKeyBase64 = os.toString()

        def os2 = new ByteArrayOutputStream()
        kp.writePrivateKey(os2)
        one.privateKeyBase64 = os2.toString()

        kp.dispose()

        one
    }
}
