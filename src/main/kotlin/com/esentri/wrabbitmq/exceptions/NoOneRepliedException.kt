package com.esentri.wrabbitmq.exceptions

import com.esentri.wrabbitmq.connection.WrabbitHeader
import com.esentri.wrabbitmq.internal.extensions.wrabbitHeader
import com.rabbitmq.client.AMQP

class NoOneRepliedException(sendingProperties: AMQP.BasicProperties):
   RuntimeException(generateMessage(sendingProperties)) {
   companion object {
       fun generateMessage(sendingProperties: AMQP.BasicProperties) = "No one replied to ${sendingProperties.wrabbitHeader(WrabbitHeader
          .TOPIC)}::${sendingProperties.wrabbitHeader(WrabbitHeader.EVENT)}"
   }
}