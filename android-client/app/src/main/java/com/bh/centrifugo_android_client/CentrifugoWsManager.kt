package com.bh.centrifugo_android_client

import android.util.Log
import io.github.centrifugal.centrifuge.*
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CentrifugoWsManager(
    private val wsAddress: String, private val token: String
) {
    private lateinit var client: Client
    private val subscriptions = mutableMapOf<String, Subscription>()

    private var statusListener: ((String) -> Unit)? = null
    private var messageListener: ((String) -> Unit)? = null

    fun setStatusListener(listener: (String) -> Unit) {
        statusListener = listener
    }

    fun setMessageListener(listener: (String) -> Unit) {
        messageListener = listener
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun connect() {
        val options = Options()
        options.token = token

        client = Client(wsAddress, options, object : EventListener() {
            override fun onConnecting(
                client: Client?, event: ConnectingEvent?
            ) {
                val message = "Connecting: ${event?.reason}"
                Log.d("Centrifuge", message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }

            override fun onConnected(client: Client?, event: ConnectedEvent?) {
                val message = "Connected"
                Log.d("Centrifuge", message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }

            override fun onDisconnected(
                client: Client?, event: DisconnectedEvent?
            ) {
                val message = "Disconnected: ${event?.reason}"
                Log.d("Centrifuge", message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }

            override fun onError(client: Client?, event: ErrorEvent?) {
                val message = "Error: ${event?.error}"
                Log.e("Centrifuge", message)
                statusListener?.invoke("[${getCurrentTimestamp()}] $message")
            }
        })

        client.connect()
    }

    fun disconnect() {
        client.disconnect()
    }

    fun subscribe(channel: String) {
        if (subscriptions.containsKey(channel)) {
            messageListener?.invoke("[${getCurrentTimestamp()}] Already subscribed to $channel")
            return
        }

        val subscription = client.newSubscription(channel,
            object : SubscriptionEventListener() {
                override fun onSubscribing(
                    sub: Subscription?, event: SubscribingEvent?
                ) {
                    val message = "Subscribing to channel: ${sub?.channel}"
                    Log.d("Centrifuge", message)
                    messageListener?.invoke("[${getCurrentTimestamp()}] $message")
                }

                override fun onSubscribed(
                    sub: Subscription?, event: SubscribedEvent?
                ) {
                    val message = "Subscribed to channel: ${sub?.channel}"
                    Log.d("Centrifuge", message)
                    messageListener?.invoke("[${getCurrentTimestamp()}] $message")
                }

                override fun onUnsubscribed(
                    sub: Subscription?, event: UnsubscribedEvent?
                ) {
                    val message = "Unsubscribed from channel: ${sub?.channel}"
                    Log.d("Centrifuge", message)
                    messageListener?.invoke("[${getCurrentTimestamp()}] $message")
                }

                override fun onError(
                    sub: Subscription?, event: SubscriptionErrorEvent?
                ) {
                    val message = "Subscription error: ${event?.message}"
                    Log.e("Centrifuge", message)
                    messageListener?.invoke("[${getCurrentTimestamp()}] $message")
                }

                override fun onPublication(
                    sub: Subscription?, event: PublicationEvent?
                ) {
                    event?.data?.let { data ->
                        val jsonString = String(data, StandardCharsets.UTF_8)
                        val message =
                            "Received message from ${sub?.channel}: $jsonString"
                        Log.d("Centrifuge", message)
                        messageListener?.invoke("[${getCurrentTimestamp()}] $message")
                    }
                }
            })
        subscription.subscribe()
        subscriptions[channel] = subscription
    }

    fun publishMessage(channel: String, message: String) {
        val subscription = subscriptions[channel]
        if (subscription == null) {
            messageListener?.invoke("[${getCurrentTimestamp()}] Cannot publish: Not subscribed to the channel $channel")
            return
        }

        val jsonMessage = "{\"input\": \"$message\"}"
        subscription.publish(jsonMessage.toByteArray()) { e, _ ->
            if (e != null) {
                val errorMessage = "Error publishing message: $e"
                Log.e("Centrifuge", errorMessage)
                messageListener?.invoke("[${getCurrentTimestamp()}] $errorMessage")
            } else {
                val successMessage = "Message published to channel $channel"
                Log.d("Centrifuge", successMessage)
                messageListener?.invoke("[${getCurrentTimestamp()}] $successMessage")
            }
        }
    }
}
