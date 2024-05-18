package server.dns

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.dns.*
import io.netty.util.AttributeKey

@CompileStatic
@Slf4j
class DmsDnsHandler extends SimpleChannelInboundHandler<DatagramDnsQuery> {
    private final int ttl
    private final DmsDnsAnswerHandler answerHandler

    DmsDnsHandler(int ttl, DmsDnsAnswerHandler answerHandler) {
        this.ttl = ttl
        this.answerHandler = answerHandler
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsQuery msg) throws Exception {
        def channel = ctx.channel()
        channel.attr(AttributeKey.<DatagramDnsQuery> valueOf(String.valueOf(msg.id()))).set(msg)

        def resp = new DatagramDnsResponse(msg.recipient(), msg.sender(), msg.id())
        def dnsQuestion = msg.recordAt(DnsSection.QUESTION)
        if (dnsQuestion != null) {
            resp.addRecord(DnsSection.QUESTION, dnsQuestion)
        }

        String name = dnsQuestion.name()
        def rawBytes = answerHandler.answer(dnsQuestion.name())
        if (rawBytes != null) {
            def queryAnswer = new DefaultDnsRawRecord(
                    name,
                    DnsRecordType.A,
                    ttl,
                    Unpooled.wrappedBuffer(rawBytes))
            resp.addRecord(DnsSection.ANSWER, queryAnswer)
            channel.writeAndFlush(resp)
        } else {
            DmsDnsProxy.instance.send(name, msg.id(), channel)
        }
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error 'handle dns query error', cause
    }
}
