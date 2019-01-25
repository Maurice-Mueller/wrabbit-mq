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

   return WaitForMessage<RETURN>(replyQueue, timeoutMS).handle { value, exception ->
      if (value == null) {
         val wrappedException = WrabbitBasicReplyException(sendingProperties, exception!!)
         ReplyLogger.error("{} -> {}", wrappedException.message, exception.toString())
         throw wrappedException
      }
      return@handle value
   }
}

private fun <RETURN> WaitForMessage(queueName: String, timeoutMS: Long): CompletableFuture<RETURN> {
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