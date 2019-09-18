package pl.szczodrzynski.edziennik.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.github.bassaer.chatmessageview.model.IChatUser
import com.github.bassaer.chatmessageview.model.Message
import com.github.bassaer.chatmessageview.view.ChatView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.App.APP_URL
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.databinding.FragmentFeedbackBinding
import pl.szczodrzynski.edziennik.datamodels.FeedbackMessage
import pl.szczodrzynski.edziennik.network.ServerRequest
import pl.szczodrzynski.edziennik.utils.Anim
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.Utils.crc16
import pl.szczodrzynski.edziennik.utils.Utils.openUrl
import java.util.*

class FeedbackFragment : Fragment() {

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentFeedbackBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = FragmentFeedbackBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        b.faqText.setOnClickListener { v -> openFaq() }
        b.faqButton.setOnClickListener { v -> openFaq() }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val message = app.gson.fromJson(intent.getStringExtra("message"), FeedbackMessage::class.java)
                val c = Calendar.getInstance()
                c.timeInMillis = message.sentTime
                val chatMessage = Message.Builder()
                        .setUser(
                                if (intent.getStringExtra("type") == "dev_chat")
                                    User(crc16(message.fromUser.toByteArray()), message.fromUserName, BitmapFactory.decodeResource(resources, R.drawable.ic_account_circle))
                                else
                                    dev)
                        .setRight(!message.received)
                        .setText(message.text)
                        .setSendTime(c)
                        .hideIcon(!message.received)
                        .build()
                if (message.received)
                    mChatView.receive(chatMessage)
                else
                    mChatView.send(chatMessage)
            }
        }

        //Set UI parameters if you need
        mChatView.setLeftBubbleColor(Utils.getAttr(activity, R.attr.colorSurface))
        mChatView.setLeftMessageTextColor(Utils.getAttr(activity, android.R.attr.textColorPrimary))
        mChatView.setRightBubbleColor(Utils.getAttr(activity, R.attr.colorPrimary))
        mChatView.setRightMessageTextColor(Color.WHITE)

        //mChatView.setBackgroundColor(ContextCompat.getColor(this, R.color.blueGray500));
        mChatView.setSendButtonColor(Utils.getAttr(activity, R.attr.colorAccent))
        mChatView.setSendIcon(R.drawable.ic_action_send)
        //mChatView.setUsernameTextColor(Color.WHITE);
        //mChatView.setSendTimeTextColor(Color.WHITE);
        //mChatView.setDateSeparatorColor(Color.WHITE);
        mChatView.setInputTextHint("Napisz...")
        //mChatView.setInputTextColor(Color.BLACK);
        mChatView.setMessageMarginTop(5)
        mChatView.setMessageMarginBottom(5)

        if (App.devMode && app.deviceId == "f054761fbdb6a238") {
            b.targetDeviceLayout.visibility = View.VISIBLE
            b.targetDeviceDropDown.setOnClickListener { v ->
                AsyncTask.execute {
                    val messageList = app.db.feedbackMessageDao().allWithCountNow
                    activity.runOnUiThread {
                        val popupMenu = PopupMenu(activity, b.targetDeviceDropDown)
                        var index = 0
                        for (message in messageList) {
                            popupMenu.menu.add(0, index, index, message.fromUserName + " - " + message.fromUser + " (" + message.messageCount + ")")
                            index++
                        }
                        popupMenu.setOnMenuItemClickListener { item ->
                            b.targetDeviceDropDown.setText(item.title)
                            mChatView.getMessageView().removeAll()
                            val message = messageList[item.itemId]
                            deviceToSend = message.fromUser
                            nameToSend = message.fromUserName
                            AsyncTask.execute {
                                val messageList2 = app.db.feedbackMessageDao().getAllByUserNow(deviceToSend)
                                activity.runOnUiThread {
                                    b.chatLayout.visibility = View.VISIBLE
                                    b.inputLayout.visibility = View.GONE
                                    for (message2 in messageList2) {
                                        val c = Calendar.getInstance()
                                        c.timeInMillis = message2.sentTime
                                        val chatMessage = Message.Builder()
                                                .setUser(if (message2.received) User(crc16(message2.fromUser.toByteArray()), message2.fromUserName, BitmapFactory.decodeResource(resources, R.drawable.ic_account_circle)) else user)
                                                .setRight(!message2.received)
                                                .setText(message2.text)
                                                .setSendTime(c)
                                                .hideIcon(!message2.received)
                                                .build()
                                        if (message2.received)
                                            mChatView.receive(chatMessage)
                                        else
                                            mChatView.send(chatMessage)
                                    }
                                }
                            }
                            false
                        }
                        popupMenu.show()
                    }
                }
            }
        } else {
            AsyncTask.execute {
                val messageList = app.db.feedbackMessageDao().allNow
                firstSend = messageList.size == 0
                activity.runOnUiThread {
                    if (firstSend) {
                        openFaq()
                        b.chatLayout.visibility = View.GONE
                        b.inputLayout.visibility = View.VISIBLE
                        b.sendButton.setOnClickListener { v ->
                            if (b.textInput.text == null || b.textInput.text!!.length == 0) {
                                Toast.makeText(app, "Podaj treść wiadomości.", Toast.LENGTH_SHORT).show()
                            } else {
                                send(b.textInput.text!!.toString())
                            }
                        }
                    } else {
                        /*new MaterialDialog.Builder(this)
                .title(R.string.faq)
                .content(R.string.faq_text)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive(((dialog, which) -> {
                    openFaq();
                }))
                .show();*/
                        b.chatLayout.visibility = View.VISIBLE
                        b.inputLayout.visibility = View.GONE
                    }
                    for (message in messageList) {
                        val c = Calendar.getInstance()
                        c.timeInMillis = message.sentTime
                        val chatMessage = Message.Builder()
                                .setUser(if (message.fromUser != null) User(crc16(message.fromUser.toByteArray()), message.fromUserName, BitmapFactory.decodeResource(resources, R.drawable.ic_account_circle)) else if (message.received) dev else user)
                                .setRight(!message.received)
                                .setText(message.text)
                                .setSendTime(c)
                                .hideIcon(!message.received)
                                .build()
                        if (message.received)
                            mChatView.receive(chatMessage)
                        else
                            mChatView.send(chatMessage)
                    }
                }
            }
        }

        //Click Send Button
        mChatView.setOnClickSendButtonListener(View.OnClickListener { send(mChatView.inputText) })
    }

    private var firstSend = true
    private var deviceToSend: String? = null
    private var nameToSend: String? = null

    private var receiver: BroadcastReceiver? = null

    class User(internal var id: Int?, internal var name: String, internal var icon: Bitmap) : IChatUser {

        override fun getId(): String {
            return this.id!!.toString()
        }

        override fun getName(): String? {
            return this.name
        }

        override fun getIcon(): Bitmap? {
            return this.icon
        }

        override fun setIcon(icon: Bitmap) {
            this.icon = icon
        }
    }

    private val dev: User by lazy {
        User(0, "Szkolny.eu", BitmapFactory.decodeResource(activity.resources, R.mipmap.ic_splash))
    }
    private val user: User by lazy {
        User(1, "Ja", BitmapFactory.decodeResource(activity.resources, R.drawable.profile_))
    }
    private val mChatView: ChatView by lazy {
        b.chatView
    }

    private fun send(text: String) {
        /*if ("enable dev mode pls".equals(text)) {
            try {
                Log.d(TAG, Utils.AESCrypt.encrypt("ok here you go it's enabled now", "8iryqZUfIUiLmJGi"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }*/
        val progressDialog = MaterialDialog.Builder(activity)
                .title(R.string.loading)
                .content(R.string.sending_message)
                .negativeText(R.string.cancel)
                .show()
        ServerRequest(app, app.requestScheme + APP_URL + "main.php?feedback_message", "FeedbackSend")
                .setBodyParameter("message_text", text)
                .setBodyParameter("target_device", if (deviceToSend == null) "null" else deviceToSend)
                .run { e, result ->
                    progressDialog.dismiss()
                    if (result != null && result.get("success") != null && result.get("success").asBoolean) {
                        val feedbackMessage = FeedbackMessage(false, text)
                        if (deviceToSend != null) {
                            feedbackMessage.fromUser = deviceToSend
                            feedbackMessage.fromUserName = nameToSend
                        }
                        AsyncTask.execute { app.db.feedbackMessageDao().add(feedbackMessage) }
                        var message = Message.Builder()
                                .setUser(user!!)
                                .setRight(true)
                                .setText(feedbackMessage.text)
                                .hideIcon(true)
                                .build()
                        mChatView!!.send(message)
                        mChatView!!.inputText = ""
                        b.textInput.setText("")
                        if (firstSend) {
                            Anim.fadeOut(b.inputLayout, 500, object : Animation.AnimationListener {
                                override fun onAnimationStart(animation: Animation) {

                                }

                                override fun onAnimationEnd(animation: Animation) {
                                    b.inputLayout.visibility = View.GONE
                                    Anim.fadeIn(b.chatLayout, 500, null)
                                }

                                override fun onAnimationRepeat(animation: Animation) {

                                }
                            })
                            if (deviceToSend == null) {
                                // we are not the developer
                                val feedbackMessage2 = FeedbackMessage(true, "Postaram się jak najszybciej Tobie odpowiedzieć. Dostaniesz powiadomienie o odpowiedzi, która pokaże się w tym miejscu.")
                                AsyncTask.execute { app.db.feedbackMessageDao().add(feedbackMessage2) }
                                message = Message.Builder()
                                        .setUser(dev!!)
                                        .setRight(false)
                                        .setText(feedbackMessage2.text)
                                        .hideIcon(false)
                                        .build()
                                mChatView!!.receive(message)
                            }
                            firstSend = false
                        }
                    } else {
                        Toast.makeText(app, "Nie udało się wysłać wiadomości.", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun openFaq() {
        openUrl(activity, "http://szkolny.eu/pomoc/")
        MaterialDialog.Builder(activity)
                .title(R.string.faq_back_title)
                .content(R.string.faq_back_text)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive { dialog, which ->

                }
                .onNegative { dialog, which ->

                }
                .show()
    }

    override fun onResume() {
        super.onResume()
        if (receiver != null)
            activity.registerReceiver(receiver, IntentFilter("pl.szczodrzynski.edziennik.activities.FeedbackActivity"))
    }

    override fun onPause() {
        super.onPause()
        if (receiver != null)
            activity.unregisterReceiver(receiver)
    }
}