package com.esentri.wrabbitmq.exceptions.event

import java.io.Serializable

data class ExceptionEventMessage(
   val correlationId: String,
   val deliveryTag: String,
   val sendingConsumerTag: String,
   val topicName: String,
   val eventName: String
): Serializable