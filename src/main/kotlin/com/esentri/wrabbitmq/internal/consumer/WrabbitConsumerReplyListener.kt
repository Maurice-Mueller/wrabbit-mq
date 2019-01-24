package com.esentri.wrabbitmq.internal.consumer

import com.esentri.wrabbitmq.WrabbitListener
import com.esentri.wrabbitmq.exceptions.NoOneRepliedException
import com.esentri.wrabbitmq.internal.converter.WrabbitObjectConverter
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope

class WrabbitConsumerReplyListener<MESSAGE_TYPE>(exculsiveChannel: Channel,
                                                 val sendingProperties: AMQP.BasicProperties,
                                                 val wrabbitConsumerListener: WrabbitListener<MESSAGE_TYPE>)
   : WrabbitConsumerBase(exculsiveChannel) {
   override fun handleDelivery(consumerTag: String?, envelope: Envelope, properties: AMQP.BasicProperties?, body: ByteArray?) {
      val message: MESSAGE_TYPE = WrabbitObjectConverter.byteArrayToObject(body!!)
      wrabbitConsumerListener(message)
   }

   override fun handleCancelOk(consumerTag: String?) {
      println(NoOneRepliedException.generateMessage(sendingProperties))
   }
}