package com.esentri.wrabbitmq

import org.fest.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.concurrent.CountDownLatch

class ExceptionTest {

   private val topic = WrabbitTopic("TestTopic-Exception")
   private fun <MESSAGE: Serializable, REPLY: Serializable> newEventWithReply() =
      NewEventWithReply<MESSAGE, REPLY>(topic)

   @Test
   fun noReplier_withDefaultTimeout() {
      val countDownLatch = CountDownLatch(1)
      val event = newEventWithReply<String, String>()
      event.sendAndReceive("hello").thenAccept {
         countDownLatch.countDown()
      }.exceptionally {
         assertThat(it.message)
            .contains(event.eventName)
            .contains(topic.topicName)
         countDownLatch.countDown()
         null
      }
      countDownLatch.await()
   }

   @Test
   fun noReplier_withCustomTimeout() {
      val countDownLatch = CountDownLatch(1)
      val event = newEventWithReply<String, String>()
      event.sendAndReceive("hello", 10).thenAccept {
         countDownLatch.countDown()
      }.exceptionally {
         assertThat(it.message)
            .contains(event.eventName)
            .contains(topic.topicName)
         countDownLatch.countDown()
         null
      }
      countDownLatch.await()
   }
}