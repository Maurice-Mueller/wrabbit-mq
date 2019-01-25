package com.esentri.wrabbitmq.internal

import com.esentri.wrabbitmq.NewChannel
import com.esentri.wrabbitmq.exceptions.WrabbitBasicReplyException
import com.esentri.wrabbitmq.internal.consumer.WrabbitConsumerReplyListener
import com.esentri.wrabbitmq.internal.converter.WrabbitObjectConverter
import com.rabbitmq.client.AMQP
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
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
   val replyQueue = replyChannel.queueDeclare(UUID.randomUUID().toString(), false, false, true, emptyMap()).queue
   replyChannel.close()

   SendMessage(topicName, sendingProperties.builder().replyTo(replyQueue).build(), message)

   val returnFuture = CompletableFuture<RETURN>()
   waitForMessage<RETURN>(replyQueue, timeoutMS).whenComplete(BiConsumer { value: RETURN?, exception: Throwable? ->
      if (value != null) {
         returnFuture.complete(value)
         return@BiConsumer
      }

      returnFuture.completeExceptionally(WrabbitBasicReplyException(sendingProperties, exception!!))
   })

   return returnFuture
}

private fun <RETURN> waitForMessage(queueName: String, timeoutMS: Long): CompletableFuture<RETURN> {
   val replyChannel = NewChannel()
   val replyFuture = CompletableFuture<RETURN>()
   val consumer = WrabbitConsumerReplyListener<RETURN>(replyChannel, replyFuture)
   replyChannel.basicConsume(queueName, true, consumer)
   replyFuture.orTimeout(timeoutMS, TimeUnit.MILLISECONDS).exceptionally {
      if (it is TimeoutException) {
         replyChannel.basicCancel(consumer.consumerTag)
      }
      null
   }
   return replyFuture
}