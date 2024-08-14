package com.bh.centrifugo_android_client


import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bh.centrifugo_android_client.databinding.RtmMessageItemBinding
import com.bh.centrifugo_android_client.vo.RtmChannelMsg

class RtmMessageAdapter(
    private var messages: ArrayList<RtmChannelMsg>,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<RtmMessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(val binding: RtmMessageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val textView: TextView =
            binding.messageTextView  // Assuming combinedTextView is your TextView's ID
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): MessageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding =
            RtmMessageItemBinding.inflate(layoutInflater, parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        // Create the full message string based on whether the name is available and the message type.
        val displayName = if (!message.name.isNullOrEmpty()) {
            if (message.type == MeetingConstants.MEETING_ROOM_COMMAND_TEXT) {
                "${message.name}: " // Including colon for command type
            } else {
                "${message.name} " // Ensure it's not null
            }
        } else {
            ""
        }

        // Concatenate the name and message with styles.
        val fullText = SpannableString(displayName + message.text)

        // Apply style to the name part
        fullText.setSpan(
            ForegroundColorSpan(Color.parseColor("#4DA0FF")),
            0, displayName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        fullText.setSpan(
            StyleSpan(Typeface.NORMAL),
            0,
            displayName.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Optionally apply different style to the message part
        fullText.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    holder.textView.context,
                   R.color.t_primary_night
                )
            ),
            displayName.length,
            fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set the styled text to TextView
        holder.textView.text = fullText
    }


    override fun getItemCount(): Int = messages.size

    // 定义最大显示数量
    private val maxDisplayCount = 5

    fun updateData(newMessages: List<RtmChannelMsg>) {

        messages.clear()
        messages.addAll(newMessages)

        if (messages.size > maxDisplayCount) {
            messages = ArrayList(messages.takeLast(maxDisplayCount))
        }

        notifyDataSetChanged()

        if (messages.isNotEmpty() && !recyclerView.isComputingLayout) {
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }

}
