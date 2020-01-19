package pl.szczodrzynski.edziennik.ui.modules.feedback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.bassaer.chatmessageview.model.IChatUser;
import com.github.bassaer.chatmessageview.model.Message;
import com.github.bassaer.chatmessageview.view.ChatView;

import java.util.Calendar;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage;
import pl.szczodrzynski.edziennik.data.db.full.FeedbackMessageWithCount;
import pl.szczodrzynski.edziennik.databinding.ActivityFeedbackBinding;
import pl.szczodrzynski.edziennik.network.ServerRequest;
import pl.szczodrzynski.edziennik.utils.Anim;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;

import static pl.szczodrzynski.edziennik.utils.Utils.crc16;
import static pl.szczodrzynski.edziennik.utils.Utils.openUrl;

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = "FeedbackActivity";
    private App app;
    private ActivityFeedbackBinding b;
    private boolean firstSend = true;
    private String deviceToSend = null;
    private String nameToSend = null;

    private BroadcastReceiver receiver;

    private class User implements IChatUser {
        Integer id;
        String name;
        Bitmap icon;

        public User(int id, String name, Bitmap icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }

        @Override
        public String getId() {
            return this.id.toString();
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Bitmap getIcon() {
            return this.icon;
        }

        @Override
        public void setIcon(Bitmap icon) {
            this.icon = icon;
        }
    }

    private User dev;
    private User user;
    private ChatView mChatView;

    private void send(String text){
        /*if ("enable dev mode pls".equals(text)) {
            try {
                Log.d(TAG, Utils.AESCrypt.encrypt("ok here you go it's enabled now", "8iryqZUfIUiLmJGi"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }*/
        MaterialDialog progressDialog = new MaterialDialog.Builder(this)
                .title(R.string.loading)
                .content(R.string.sending_message)
                .negativeText(R.string.cancel)
                .show();
        new ServerRequest(app, "https://edziennik.szczodrzynski.pl/app/main.php?feedback_message", "FeedbackSend")
                .setBodyParameter("message_text", text)
                .setBodyParameter("target_device", deviceToSend == null ? "null" : deviceToSend)
                .run(((e, result) -> {
                    progressDialog.dismiss();
                    if (result != null && result.get("success") != null && result.get("success").getAsBoolean()) {
                        FeedbackMessage feedbackMessage = new FeedbackMessage(false, text);
                        if (deviceToSend != null) {
                            feedbackMessage.fromUser = deviceToSend;
                            feedbackMessage.fromUserName = nameToSend;
                        }
                        AsyncTask.execute(() -> app.db.feedbackMessageDao().add(feedbackMessage));
                        Message message = new Message.Builder()
                                .setUser(user)
                                .setRight(true)
                                .setText(feedbackMessage.text)
                                .hideIcon(true)
                                .build();
                        mChatView.send(message);
                        mChatView.setInputText("");
                        b.textInput.setText("");
                        if (firstSend) {
                            Anim.fadeOut(b.inputLayout, 500, new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    b.inputLayout.setVisibility(View.GONE);
                                    Anim.fadeIn(b.chatLayout, 500, null);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });
                            if (deviceToSend == null) {
                                // we are not the developer
                                FeedbackMessage feedbackMessage2 = new FeedbackMessage(true, "Postaram się jak najszybciej Tobie odpowiedzieć. Dostaniesz powiadomienie o odpowiedzi, która pokaże się w tym miejscu.");
                                AsyncTask.execute(() -> app.db.feedbackMessageDao().add(feedbackMessage2));
                                message = new Message.Builder()
                                        .setUser(dev)
                                        .setRight(false)
                                        .setText(feedbackMessage2.text)
                                        .hideIcon(false)
                                        .build();
                                mChatView.receive(message);
                            }
                            firstSend = false;
                        }
                    }
                    else {
                        Toast.makeText(app, "Nie udało się wysłać wiadomości.", Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void openFaq() {
        openUrl(this, "http://szkolny.eu/pomoc/");
        new MaterialDialog.Builder(this)
                .title(R.string.faq_back_title)
                .content(R.string.faq_back_text)
                .positiveText(R.string.yes)
                .negativeText(R.string.no)
                .onPositive(((dialog, which) -> {

                }))
                .onNegative(((dialog, which) -> {

                }))
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Themes.INSTANCE.getAppTheme());
        b = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_feedback, null, false);
        setContentView(b.getRoot());
        app = (App) getApplication();

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        b.faqText.setOnClickListener((v -> {
            openFaq();
        }));
        b.faqButton.setOnClickListener((v -> {
            openFaq();
        }));

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                FeedbackMessage message = app.getGson().fromJson(intent.getStringExtra("message"), FeedbackMessage.class);
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(message.sentTime);
                Message chatMessage = new Message.Builder()
                        .setUser(intent.getStringExtra("type").equals("dev_chat") ? new User(crc16(message.fromUser.getBytes()), message.fromUserName, BitmapFactory.decodeResource(getResources(), R.drawable.ic_account_circle)) : dev)
                        .setRight(!message.received)
                        .setText(message.text)
                        .setSendTime(c)
                        .hideIcon(!message.received)
                        .build();
                if (message.received)
                    mChatView.receive(chatMessage);
                else
                    mChatView.send(chatMessage);
            }
        };

        mChatView = b.chatView;

        dev = new User(0, "Szkolny.eu", BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        user = new User(1, "Ja", BitmapFactory.decodeResource(getResources(), R.drawable.profile));

        //Set UI parameters if you need
        mChatView.setLeftBubbleColor(Utils.getAttr(this, R.attr.colorSurface));
        mChatView.setLeftMessageTextColor(Utils.getAttr(this, android.R.attr.textColorPrimary));
        mChatView.setRightBubbleColor(Utils.getAttr(this, R.attr.colorPrimary));
        mChatView.setRightMessageTextColor(Color.WHITE);

        //mChatView.setBackgroundColor(ContextCompat.getColor(this, R.color.blueGray500));
        mChatView.setSendButtonColor(Utils.getAttr(this, R.attr.colorAccent));
        mChatView.setSendIcon(R.drawable.ic_action_send);
        //mChatView.setUsernameTextColor(Color.WHITE);
        //mChatView.setSendTimeTextColor(Color.WHITE);
        //mChatView.setDateSeparatorColor(Color.WHITE);
        mChatView.setInputTextHint("Napisz...");
        //mChatView.setInputTextColor(Color.BLACK);
        mChatView.setMessageMarginTop(5);
        mChatView.setMessageMarginBottom(5);

        if (App.Companion.getDevMode() && app.getDeviceId().equals("f054761fbdb6a238")) {
            b.targetDeviceLayout.setVisibility(View.VISIBLE);
            b.targetDeviceDropDown.setOnClickListener((v -> {
                AsyncTask.execute(() -> {
                    List<FeedbackMessageWithCount> messageList = app.db.feedbackMessageDao().getAllWithCountNow();
                    runOnUiThread(() -> {
                        PopupMenu popupMenu = new PopupMenu(this, b.targetDeviceDropDown);
                        int index = 0;
                        for (FeedbackMessageWithCount message: messageList) {
                            popupMenu.getMenu().add(0, index, index, message.fromUserName+" - "+message.fromUser+" ("+message.messageCount+")");
                            index++;
                        }
                        popupMenu.setOnMenuItemClickListener(item -> {
                            b.targetDeviceDropDown.setText(item.getTitle());
                            mChatView.getMessageView().removeAll();
                            FeedbackMessageWithCount message = messageList.get(item.getItemId());
                            deviceToSend = message.fromUser;
                            nameToSend = message.fromUserName;
                            AsyncTask.execute(() -> {
                                List<FeedbackMessage> messageList2 = app.db.feedbackMessageDao().getAllByUserNow(deviceToSend);
                                runOnUiThread(() -> {
                                    b.chatLayout.setVisibility(View.VISIBLE);
                                    b.inputLayout.setVisibility(View.GONE);
                                    for (FeedbackMessage message2 : messageList2) {
                                        Calendar c = Calendar.getInstance();
                                        c.setTimeInMillis(message2.sentTime);
                                        Message chatMessage = new Message.Builder()
                                                .setUser(message2.received ? new User(crc16(message2.fromUser.getBytes()), message2.fromUserName, BitmapFactory.decodeResource(getResources(), R.drawable.ic_account_circle)) : user)
                                                .setRight(!message2.received)
                                                .setText(message2.text)
                                                .setSendTime(c)
                                                .hideIcon(!message2.received)
                                                .build();
                                        if (message2.received)
                                            mChatView.receive(chatMessage);
                                        else
                                            mChatView.send(chatMessage);
                                    }
                                });
                            });
                            return false;
                        });
                        popupMenu.show();
                    });
                });
            }));
        }
        else {
            AsyncTask.execute(() -> {
                List<FeedbackMessage> messageList = app.db.feedbackMessageDao().getAllNow();
                firstSend = messageList.size() == 0;
                runOnUiThread(() -> {
                    if (firstSend) {
                        openFaq();
                        b.chatLayout.setVisibility(View.GONE);
                        b.inputLayout.setVisibility(View.VISIBLE);
                        b.sendButton.setOnClickListener((v -> {
                            if (b.textInput.getText() == null || b.textInput.getText().length() == 0) {
                                Toast.makeText(app, "Podaj treść wiadomości.", Toast.LENGTH_SHORT).show();
                            } else {
                                send(b.textInput.getText().toString());
                            }
                        }));
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
                        b.chatLayout.setVisibility(View.VISIBLE);
                        b.inputLayout.setVisibility(View.GONE);
                    }
                    for (FeedbackMessage message : messageList) {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(message.sentTime);
                        Message chatMessage = new Message.Builder()
                                .setUser(message.fromUser != null ? new User(crc16(message.fromUser.getBytes()), message.fromUserName, BitmapFactory.decodeResource(getResources(), R.drawable.ic_account_circle)) : message.received ? dev : user)
                                .setRight(!message.received)
                                .setText(message.text)
                                .setSendTime(c)
                                .hideIcon(!message.received)
                                .build();
                        if (message.received)
                            mChatView.receive(chatMessage);
                        else
                            mChatView.send(chatMessage);
                    }
                });
            });
        }

        //Click Send Button
        mChatView.setOnClickSendButtonListener(view -> {
            send(mChatView.getInputText());
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) // Press Back Icon
        {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter("pl.szczodrzynski.edziennik.ui.modules.base.FeedbackActivity"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}
