package com.esentri.wrabbitmq.internal

import com.esentri.wrabbitmq.NewChannel
import com.esentri.wrabbitmq.exceptions.WrabbitBasicReplyException
import com.esentri.wrabbitmq.internal.consumer.WrabbitConsumerReplyListener
import com.esentri.wrabbitmq.internal.converter.WrabbitObjectConverter
import com.rabbitmq.client.AMQP
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

fun SendMessage(topicName: String, sendingProperties: AMQP.BasicProperties, message: Any) {
   val newChannel = NewChannel()
   newChannel.basicPublish(topicName, "", sendingProperties, WrabbitObjectConverter.objectToByteArray(message))
   newChannel.close()
}

fun <RETURN> SendAndReceiveMessage(
   topicName: String,
   sendingProperties: AMQP.BasicProperties,
   message: Any,
   timeoutMS: Long
): CompletableFuture<RETURN> {

   val replyChannel = NewChannel()
   val replyQueue = replyChannel.queueDeclare().queue
   val correlationID = UUID.randomUUID().toString()
   val sendChannel = NewChannel()
   sendChannel.basicPublish(topicName, "",
      sendingProperties.builder().correlationId(correlationID).replyTo(replyQueue).build(),
      WrabbitObjectConverter.objectToByteArray(message))
   sendChannel.close()
   val replyFuture = CompletableFuture<RETURN>()
   val consumer = WrabbitConsumerReplyListener<RETURN>(replyChannel, replyFuture)
   replyChannel.basicConsume(replyQueue, true, consumer)
   replyFuture.orTimeout(timeoutMS, TimeUnit.MILLISECONDS)
   val returnFuture = CompletableFuture<RETURN>()

   replyFuture.whenComplete(BiConsumer { value: RETURN?, exception: Throwable? ->
      if (value != null) {
         returnFuture.complete(value)
         return@BiConsumer
      }
      returnFuture.completeExceptionally(WrabbitBasicReplyException(sendingProperties, exception!!))
   })
   return returnFuture
}
