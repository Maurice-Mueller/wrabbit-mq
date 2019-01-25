package com.esentri.wrabbitmq.internal.consumer

import com.esentri.wrabbitmq.WrabbitReplierWithContext
import com.esentri.wrabbitmq.connection.WrabbitHeader
import com.esentri.wrabbitmq.internal.converter.WrabbitObjectConverter
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope

// TODO replace with new NewChannel method
class WrabbitConsumerReplier<MESSAGE_TYPE, RETURN_TYPE>(channel: Channel,
                                                        val wrabbitReplier: WrabbitReplierWithContext<MESSAGE_TYPE, RETURN_TYPE>)
   : WrabbitConsumerBase(channel) {

   override fun handleDelivery(consumerTag: String?, envelope: Envelope, properties: AMQP.BasicProperties?, body: ByteArray?) {
      val message: MESSAGE_TYPE = WrabbitObjectConverter.byteArrayToObject(body!!)
      try {
         val result = wrabbitReplier(properties!!.headers, message)
         sendAnswer(properties, responseByteArray(result))
         super.getChannel().basicAck(envelope.deliveryTag, false)
      } catch (e: Exception) {
         sendAnswer(properties!!, responseByteArray(e))
         super.getChannel().basicAck(envelope.deliveryTag, false)
      }
   }

   private fun responseByteArray(result: RETURN_TYPE): ByteArray =
      WrabbitObjectConverter.objectToByteArray(WrabbitReplyWrapperMessage(value = result))

   private fun responseByteArray(exception: Exception): ByteArray =
      WrabbitObjectConverter.objectToByteArray(WrabbitReplyWrapperMessage<RETURN_TYPE>(exception = exception))


   private fun sendAnswer(receivedProperties: AMQP.BasicProperties, body: ByteArray) {
      super.getChannel().basicPublish("",
         receivedProperties.replyTo,
         replyProperties(receivedProperties),
         body)
   }

   private fun replyProperties(properties: AMQP.BasicProperties): AMQP.BasicProperties = AMQP.BasicProperties
      .Builder()
      .correlationId(properties.correlationId)
      .headers(properties.headers.filter { it.key == WrabbitHeader.EVENT.key || it.key == WrabbitHeader.TOPIC.key })
      .build()
}