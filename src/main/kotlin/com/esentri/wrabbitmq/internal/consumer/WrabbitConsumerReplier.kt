package com.esentri.wrabbitmq.internal.consumer

import com.esentri.wrabbitmq.WrabbitReplierWithContext
import com.esentri.wrabbitmq.internal.converter.WrabbitObjectConverter
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope

class WrabbitConsumerReplier<MESSAGE_TYPE, RETURN_TYPE>(channel: Channel,
                                                        val wrabbitReplier: WrabbitReplierWithContext<MESSAGE_TYPE, RETURN_TYPE>)
   : WrabbitConsumerBase(channel) {

   override fun handleDelivery(consumerTag: String?, envelope: Envelope, properties: AMQP.BasicProperties?, body: ByteArray?) {
      val message: MESSAGE_TYPE = WrabbitObjectConverter.byteArrayToObject(body!!)
      val result = wrabbitReplier(properties!!.headers, message)
      super.getChannel().basicPublish("",
         properties.replyTo,
         AMQP.BasicProperties
            .Builder()
            .correlationId(properties.correlationId)
            .build(),
         WrabbitObjectConverter.objectToByteArray(result!!))
      //TODO replace with new NewChannel method
      super.getChannel().basicAck(envelope.deliveryTag, false)
   }
}