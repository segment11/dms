package server.dns

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.codec.dns.*
import io.netty.util.AttributeKey

import java.util.concurrent.ConcurrentHashMap

@CompileStatic
@Singleton
@Slf4j
class DmsDnsProxy {
    private Map<Integer, Channel> msgIdChannelMap = new ConcurrentHashMap<>()
    private Channel proxyChannel

    void clear() {
        this.msgIdChannelMap.clear()
        this.proxyChannel = null
    }

    private String targetIp
    private int targetPort

    void init(String targetIp, int targetPort, int ttl, NioEventLoopGroup workerGroup) {
        this.targetIp = targetIp
        this.targetPort = targetPort

        def bootstrap = new Bootstrap()
        try {
            bootstrap.group(workerGroup).channel(NioDatagramChannel)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        void initChannel(NioDatagramChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new DatagramDnsQueryEncoder())
                                    .addLast(new DatagramDnsResponseDecoder())
                                    .addLast(new SimpleChannelInboundHandler<DatagramDnsResponse>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsResponse msg) throws Exception {
                                            def channelQuery = msgIdChannelMap.remove(msg.id())
                                            if (channelQuery == null) {
                                                log.warn 'channel query not found for id: {}', msg.id()
                                                return
                                            }

                                            def dnsQuery = channelQuery.attr(AttributeKey.<DatagramDnsQuery> valueOf(String.valueOf(msg.id()))).get()
                                            def question = msg.recordAt(DnsSection.QUESTION)
                                            def dnsResponse = new DatagramDnsResponse(dnsQuery.recipient(), dnsQuery.sender(), msg.id())
                                            dnsResponse.addRecord(DnsSection.QUESTION, question)

                                            for (int i = 0, count = msg.count(DnsSection.ANSWER); i < count; i++) {
                                                def record = msg.recordAt(DnsSection.ANSWER, i)
                                                if (record.type() == DnsRecordType.A) {
                                                    // just print the IP after query
                                                    def raw = (DnsRawRecord) record
                                                    def rawBytes = ByteBufUtil.getBytes(raw.content())
                                                    def queryAnswer = new DefaultDnsRawRecord(
                                                            question.name(),
                                                            DnsRecordType.A,
                                                            ttl,
                                                            Unpooled.wrappedBuffer(rawBytes))
                                                    dnsResponse.addRecord(DnsSection.ANSWER, queryAnswer)
                                                }
                                            }

                                            channelQuery.writeAndFlush(dnsResponse)
                                        }

                                        @Override
                                        void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                            log.error('handle dns proxy query error', cause)
                                        }
                                    })
                        }
                    })

            this.proxyChannel = bootstrap.connect(targetIp, targetPort).sync().channel()
            log.info 'get dns proxy channel success, target name server: {}:{}', targetIp, targetPort
        } catch (InterruptedException e) {
            log.error('server start interrupted error', e)
        }
    }

    void send(String domain, int id, Channel requestChannel) {
        this.msgIdChannelMap[id] = requestChannel

        def query = new DatagramDnsQuery(null, new InetSocketAddress(targetIp, targetPort), id)
        query.setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domain, DnsRecordType.A))
        this.proxyChannel.writeAndFlush(query)
    }
}
