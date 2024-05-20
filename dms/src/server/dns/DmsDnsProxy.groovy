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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@CompileStatic
@Singleton
@Slf4j
class DmsDnsProxy {
    @CompileStatic
    private static class WrapperMsgTimeWithChannel {
        long time
        Channel channel

        WrapperMsgTimeWithChannel(long time, Channel channel) {
            this.time = time
            this.channel = channel
        }
    }

    private Map<Integer, WrapperMsgTimeWithChannel> msgIdChannelMap = new ConcurrentHashMap<>()
    private Channel proxyChannel

    private ScheduledExecutorService executorService
    private final long timeoutMillis = 10000

    synchronized void stop() {
        if (this.executorService != null) {
            this.executorService.shutdown()
            this.executorService = null
        }

        this.msgIdChannelMap.clear()
        this.proxyChannel = null
    }

    private String targetIp
    private int targetPort

    synchronized void init(String targetIp, int targetPort, int ttl, NioEventLoopGroup workerGroup) {
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
                                            def wrapper = msgIdChannelMap.remove(msg.id())
                                            if (wrapper == null) {
                                                log.warn 'channel query not found for id: {}', msg.id()
                                                return
                                            }

                                            def channelQuery = wrapper.channel
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

            startIntervalRemoveTimeoutMsg()
        } catch (InterruptedException e) {
            log.error('server start interrupted error', e)
        }
    }

    void send(String domain, int id, Channel requestChannel) {
        this.msgIdChannelMap[id] = new WrapperMsgTimeWithChannel(System.currentTimeMillis(), requestChannel)

        def query = new DatagramDnsQuery(null, new InetSocketAddress(targetIp, targetPort), id)
        query.setRecord(DnsSection.QUESTION, new DefaultDnsQuestion(domain, DnsRecordType.A))
        this.proxyChannel.writeAndFlush(query)
    }

    private void startIntervalRemoveTimeoutMsg() {
        executorService = Executors.newSingleThreadScheduledExecutor()
        executorService.scheduleAtFixedRate({
            long now = System.currentTimeMillis()
            msgIdChannelMap.each { id, wrapper ->
                if (now - wrapper.time > timeoutMillis) {
                    log.warn 'remove timeout msg id: {}', id
                    msgIdChannelMap.remove(id)
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS)
        log.info 'start interval remove timeout msg task...'
    }
}
