package server.dns

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.dns.DatagramDnsQueryDecoder
import io.netty.handler.codec.dns.DatagramDnsResponseEncoder

@CompileStatic
@Singleton
@Slf4j
class DmsDnsServer {
    private NioEventLoopGroup workerGroup

    boolean isListening() {
        workerGroup != null
    }

    void listen(String host, int port, int ttl, String targetIp, int targetPort, DmsDnsAnswerHandler answerHandler) {
        if (workerGroup != null) {
            throw new IllegalStateException('Dns server already started')
        }

        log.info 'Start dns server, listening on {}:{}', host, port
        workerGroup = new NioEventLoopGroup(1)

        // use same event loop group
        DmsDnsProxy.instance.init(targetIp, targetPort, ttl, workerGroup)

        def bootstrap = new Bootstrap()
        try {
            bootstrap.group(workerGroup).channel(NioDatagramChannel)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        void initChannel(NioDatagramChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new DatagramDnsQueryDecoder())
                                    .addLast(new DatagramDnsResponseEncoder())
                                    .addLast(new DmsDnsHandler(ttl, answerHandler))
                        }
                    })
                    .option(ChannelOption.SO_BROADCAST, true)

            def future = bootstrap.bind(host, port).sync()
            future.channel().closeFuture().sync()
        } catch (InterruptedException e) {
            log.error('server start interrupted error', e)
        }
    }

    void stop() {
        log.info 'Stopping dns server...'
        if (workerGroup) {
            workerGroup.shutdownGracefully()
            workerGroup = null
        }

        DmsDnsProxy.instance.stop()
    }
}
