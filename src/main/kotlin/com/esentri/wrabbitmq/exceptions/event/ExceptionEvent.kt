package com.esentri.wrabbitmq.exceptions.event

import com.esentri.wrabbitmq.connection.WrabbitInternalTopic
import com.esentri.wrabbitmq.java.WrabbitEvent

object ExceptionEvent:
   WrabbitEvent<ExceptionEventMessage>(
      WrabbitInternalTopic,
      "EXCEPTION"
   )