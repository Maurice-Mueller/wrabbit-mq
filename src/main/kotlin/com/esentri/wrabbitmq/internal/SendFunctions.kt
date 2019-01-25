package com.esentri.wrabbitmq.internal

import com.esentri.wrabbitmq.NewChannel
import com.esentri.wrabbitmq.exceptions.NoOneRepliedException
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
   message: Any): CompletableFuture<RETURN> {

   val replyChannel = NewChannel()
   val replyQueue = replyChannel.queueDeclare().queue
   val correlationID = UUID.randomUUID().toString()
   val sendChannel = NewChannel()
   sendChannel.basicPublish(topicName, "",
      sendingProperties.builder().correlationId(correlationID).replyTo(replyQueue).build(),
      WrabbitObjectConverter.objectToByteArray(message))
   sendChannel.close()
   val replyFuture = CompletableFuture<RETURN>()
   val consumer = WrabbitConsumerReplyListener<RETURN>(replyChannel, sendingProperties
   ) {
      replyFuture.complete(it)
   }
   replyChannel.basicConsume(replyQueue, true, consumer)
   replyFuture.orTimeout(1, TimeUnit.SECONDS)
   val returnFuture = CompletableFuture<RETURN>()
   replyFuture.whenComplete(BiConsumer { value: RETURN?, _: Throwable? ->
      if (value != null) {
         returnFuture.complete(value)
         return@BiConsumer
      }
      replyChannel.basicCancel(consumer.consumerTag)
      returnFuture.completeExceptionally(NoOneRepliedException(sendingProperties))
   })
   return returnFuture
}
