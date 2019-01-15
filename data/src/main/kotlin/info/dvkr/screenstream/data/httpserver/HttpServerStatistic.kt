package info.dvkr.screenstream.data.httpserver

import com.elvishew.xlog.XLog
import info.dvkr.screenstream.data.model.AppError
import info.dvkr.screenstream.data.model.FatalError
import info.dvkr.screenstream.data.model.HttpClient
import info.dvkr.screenstream.data.model.TrafficPoint
import info.dvkr.screenstream.data.other.asString
import info.dvkr.screenstream.data.other.getLog
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

internal class HttpServerStatistic(
    private val onStatistic: (List<HttpClient>, List<TrafficPoint>) -> Unit,
    onError: (AppError) -> Unit
) : HttpServerCoroutineScope(onError) {

    private data class LocalClient(
        val clientAddress: InetSocketAddress,
        var isSlowConnection: Boolean = false,
        var isDisconnected: Boolean = false,
        var sendBytes: Long = 0,
        var disconnectedTime: Long = 0
    ) {
        internal fun isDisconnectHoldTimePass(now: Long) =
            (now - disconnectedTime) > AppHttpServer.CLIENT_DISCONNECT_HOLD_TIME_SECONDS * 1000

        internal fun toHttpClient() =
            HttpClient(clientAddress.hashCode().toLong(), clientAddress.asString(), isSlowConnection, isDisconnected)
    }

    internal sealed class StatisticEvent {
        object CalculateTraffic : StatisticEvent()
        object SendStatistic : StatisticEvent()

        data class Connected(val address: InetSocketAddress) : StatisticEvent()
        data class Disconnected(val address: InetSocketAddress) : StatisticEvent()
        data class Backpressure(val address: InetSocketAddress) : StatisticEvent()
        data class NextBytes(val address: InetSocketAddress, val bytesCount: Int) : StatisticEvent() {
            override fun toString(): String = this::class.java.simpleName
        }

        override fun toString(): String = this::class.java.simpleName
    }

    private val eventQueue = ConcurrentLinkedDeque<StatisticEvent>() // For debug

    private val statisticEventChannel: SendChannel<StatisticEvent> = actor(capacity = 32) {
        val clientsMap = HashMap<InetSocketAddress, LocalClient>()
        val trafficHistory = LinkedList<TrafficPoint>()

        val past = System.currentTimeMillis() - AppHttpServer.TRAFFIC_HISTORY_SECONDS * 1000
        (0..AppHttpServer.TRAFFIC_HISTORY_SECONDS + 1).forEach { i ->
            trafficHistory.addLast(TrafficPoint(i * 1000 + past, 0))
        }

        for (event in this@actor) try {
            XLog.v(this@HttpServerStatistic.getLog("Actor", event.toString()))
            val e = eventQueue.poll()
            if (eventQueue.size > 8) XLog.i(getLog("Actor", "eventQueue.size:${eventQueue.size} : $e"))

            when (event) {
                is StatisticEvent.Connected -> clientsMap[event.address] = LocalClient(event.address)

                is StatisticEvent.Disconnected -> clientsMap[event.address]?.apply {
                    isDisconnected = true
                    disconnectedTime = System.currentTimeMillis()
                }

                is StatisticEvent.Backpressure -> clientsMap[event.address]?.isSlowConnection = true

                is StatisticEvent.NextBytes -> clientsMap[event.address]?.apply {
                    sendBytes = sendBytes.plus(event.bytesCount)
                }

                is StatisticEvent.CalculateTraffic -> {
                    val now = System.currentTimeMillis()
                    clientsMap.values.removeAll { it.isDisconnected && it.isDisconnectHoldTimePass(now) }
                    val traffic = clientsMap.values.map { it.sendBytes }.sum()
                    clientsMap.values.forEach { it.sendBytes = 0 }
                    trafficHistory.removeFirst()
                    trafficHistory.addLast(TrafficPoint(now, traffic))
                }

                is StatisticEvent.SendStatistic -> onStatistic(
                    clientsMap.values.toList().map { it.toHttpClient() }.sortedBy { it.id },
                    trafficHistory.toList().sortedBy { it.time }
                )
            }
        } catch (throwable: Throwable) {
            XLog.e(this@HttpServerStatistic.getLog("actor"), throwable)
            onError(FatalError.ActorException)
        }
    }

    init {
        XLog.d(getLog("init", "Invoked"))

        launch {
            while (isActive) {
                sendStatisticEvent(StatisticEvent.CalculateTraffic)
                sendStatisticEvent(StatisticEvent.SendStatistic)
                delay(1000)
            }
        }
    }

    internal fun sendStatisticEvent(event: StatisticEvent) {
        try {
            if (supervisorJob.isActive.not()) throw IllegalStateException("JobIsNotActive")
            eventQueue.addLast(event)
            if (eventQueue.size > 8)
                XLog.i(
                    getLog("sendStatisticEvent", "eventQueue.size:${eventQueue.size} : ${eventQueue.joinToString()}")
                )
            if (statisticEventChannel.offer(event).not()) throw IllegalStateException("ChannelIsFull")
        } catch (th: Throwable) {
            XLog.e(getLog("sendStatisticEvent"), th)
            onError(FatalError.ChannelException)
        }
    }
}