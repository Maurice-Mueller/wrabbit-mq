package com.esentri.wrabbitmq

import com.esentri.wrabbitmq.connection.WrabbitHeader
import com.esentri.wrabbitmq.internal.SendAndReceiveMessage
import com.esentri.wrabbitmq.internal.consumer.WrabbitConsumerReplier
import java.io.Serializable
import java.util.*
import java.util.concurrent.CompletableFuture

open class WrabbitEventWithReply<MESSAGE: Serializable, RETURN: Serializable>(
   wrabbitTopic: WrabbitTopic,
   eventName: String): WrabbitEvent<MESSAGE>(wrabbitTopic, eventName) {

   override fun messageBuilder() = WrabbitMessageBuilderReplier<MESSAGE, RETURN>(wrabbitTopic.topicName, super.standardSendingProperties)

   fun sendAndReceive(message: MESSAGE, timeoutMS: Long = WrabbitReplyTimeout()): CompletableFuture<RETURN> =
      SendAndReceiveMessage(wrabbitTopic.topicName,
         super.standardSendingProperties,
         message)

   fun replier(replier: WrabbitReplier<MESSAGE, RETURN>) {
      this.replier { _, message ->  replier(message)}
   }

   fun replier(replier: WrabbitReplierWithContext<MESSAGE, RETURN>) {
      val newChannel = NewChannel()
      val queueName = "${wrabbitTopic.topicName}.$eventName.REPLIER"
      newChannel.queueDeclare(queueName, true, true, false, emptyMap())
      newChannel.queueBind(queueName, wrabbitTopic.topicName, "", replierHeadersForEvent())
      newChannel.basicConsume(queueName, false, WrabbitConsumerReplier(newChannel, replier))
   }

   private fun replierHeadersForEvent(): MutableMap<String, Any?> {
      val headers: MutableMap<String, Any?> = HashMap()
      headers["x-match"] = "all"
      headers[WrabbitHeader.EVENT.key] = eventName
      return headers
   }

}