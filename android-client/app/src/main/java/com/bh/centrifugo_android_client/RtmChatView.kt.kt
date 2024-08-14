package com.bh.centrifugo_android_client

import com.bh.centrifugo_android_client.vo.RtmChannelMsg
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bh.centrifugo_android_client.databinding.RtmChatViewLayoutBinding


class RtmChatView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(removeMessageRunnable) // 取消回调
    }

    private val messages = ArrayList<RtmChannelMsg>() // 用於存儲消息的鏈表
    private val handler = Handler(Looper.getMainLooper()) // 用於處理延時操作的 Handler
    private val removeMessageRunnable =
        Runnable { removeMessage() } // 定義一個 Runnable 用於移除消息

    private val autoHideInputRunnable =
        Runnable {
            if(!isParentControlVisible) {
                hideInputBox()
            }
        }

    private fun hideInputBox() {
        binding.inputMessageBox.isVisible = false
    }

    // 初始化 View Binding
    private var binding: RtmChatViewLayoutBinding =
        RtmChatViewLayoutBinding.inflate(
            LayoutInflater.from(context), this
        )
    private var adapter: RtmMessageAdapter

    init {

        orientation = VERTICAL

        adapter = RtmMessageAdapter(messages, binding.recyclerView)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        Log.d("ChatView", "Adapter set: $adapter")

        binding.inputMessageBox.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // 监听 EditText 焦点变化
        binding.inputMessageBox.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                handler.postDelayed(autoHideInputRunnable, 5000)
            } else {
                handler.removeCallbacks(autoHideInputRunnable)
            }
        }
    }

    private fun sendMessage() {
        val messageText = binding.inputMessageBox.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val newMessage = RtmChannelMsg(
                account = myAccount,
                // id = MeetingManager.rtmId2uid(myAccount),
                id = myAccount,
                name = myDisplayName,
                text = messageText,
                timestamp = System.currentTimeMillis(),
                type = MeetingConstants.MEETING_ROOM_COMMAND_TEXT
            )

            sendMessageListener?.onSendMessage(messageText)

            newMessage.name = "You"

            messages.add(newMessage)
            adapter.updateData(messages.toList()) // 更新 adapter 的数据
            binding.inputMessageBox.setText("") // 清空输入框

            // 延时启动移除消息
            handler.removeCallbacks(removeMessageRunnable)
            handler.postDelayed(removeMessageRunnable, 3000)  // 每3秒检查一次
        }
    }


    private fun removeMessage() {
        val now = System.currentTimeMillis()

        // 移除五秒之前的消息
        val iterator = messages.iterator()
        while (iterator.hasNext()) {
            val message = iterator.next()
            if ((now - message.timestamp!!) > 5000) {  // 检查消息是否超过五秒
                iterator.remove()  // 移除超过五秒的消息
            }
        }

        adapter.updateData(messages.toList()) // 更新 adapter 的数据

        // 只要列表不为空，继续定时调度任务
        if (messages.isNotEmpty()) {
            handler.postDelayed(removeMessageRunnable, 3000)  // 每3秒检查一次
        }
    }


    interface OnSendMessageListener {
        fun onSendMessage(message: String)
    }

    private var isParentControlVisible = false

    private var sendMessageListener: OnSendMessageListener? = null

    fun pushMessage(
        message: String?,
        name: String? = null,
        msgType: String? = null
    ) {
        if (message != null) {
            //val displayName = MeetingManager.extractName(name ?: "")
            val displayName = name
            val newMessage = RtmChannelMsg(
                name = displayName,
                text = message,
                timestamp = System.currentTimeMillis(),
                type = msgType
            )
            messages.add(newMessage)
            adapter.updateData(messages.toList())

            // 延时启动移除消息
            handler.removeCallbacks(removeMessageRunnable)
            handler.postDelayed(removeMessageRunnable, 3000)  // 每3秒检查一次
        }
    }

    private var myDisplayName = ""
    private var myUid = 0
    private var myAccount = ""

    fun setMyInfo(displayName: String?, account: String?, uid: Int) {
        myDisplayName = displayName ?: ""
        myAccount = account ?: ""
        myUid = uid
    }

    fun setOnSendMessageListener(listener: OnSendMessageListener) {
        sendMessageListener = listener
    }
}
