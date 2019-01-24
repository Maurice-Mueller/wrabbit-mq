package com.esentri.wrabbitmq.internal.consumer

import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.ShutdownSignalException

abstract class WrabbitConsumerBase(channel: Channel): DefaultConsumer(channel) {

   override fun handleCancelOk(consumerTag: String?) {
      throw RuntimeException("asdf")
   }

   override fun handleShutdownSignal(consumerTag: String?, sig: ShutdownSignalException?) {
      throw RuntimeException("asdf")
   }

}