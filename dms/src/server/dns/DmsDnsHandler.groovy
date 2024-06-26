package server.dns

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.dns.DatagramDnsQuery
import io.netty.handler.codec.dns.DatagramDnsResponse
import io.netty.handler.codec.dns.DnsSection
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

        def answerList = answerHandler.answer(dnsQuestion, ttl)
        if (answerList != null) {
            answerList.each { resp.addRecord(DnsSection.ANSWER, it) }
            channel.writeAndFlush(resp)
        } else {
            DmsDnsProxy.instance.send(dnsQuestion.name(), msg.id(), channel)
        }
    }

    @Override
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error 'handle dns query error', cause
    }
}
