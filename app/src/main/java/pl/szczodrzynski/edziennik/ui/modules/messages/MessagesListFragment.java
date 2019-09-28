package pl.szczodrzynski.edziennik.ui.modules.messages;


import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.api.AppError;
import pl.szczodrzynski.edziennik.api.Edziennik;
import pl.szczodrzynski.edziennik.api.interfaces.SyncCallback;
import pl.szczodrzynski.edziennik.databinding.MessagesListBinding;
import pl.szczodrzynski.edziennik.datamodels.LoginStore;
import pl.szczodrzynski.edziennik.datamodels.Message;
import pl.szczodrzynski.edziennik.datamodels.MessageFull;
import pl.szczodrzynski.edziennik.datamodels.MessageRecipientFull;
import pl.szczodrzynski.edziennik.datamodels.Profile;
import pl.szczodrzynski.edziennik.datamodels.ProfileFull;
import pl.szczodrzynski.edziennik.utils.Themes;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static pl.szczodrzynski.edziennik.datamodels.LoginStore.LOGIN_TYPE_LIBRUS;
import static pl.szczodrzynski.edziennik.utils.Utils.d;

public class MessagesListFragment extends Fragment {

    private App app = null;
    private MainActivity activity = null;
    private MessagesListBinding b = null;

    private Rect viewRect = new Rect();
    private MessagesAdapter messagesAdapter = null;
    private ViewGroup viewParent = null;

    static final Interpolator transitionInterpolator = new FastOutSlowInInterpolator();
    static final long TRANSITION_DURATION = 300L;
    static final String TAP_POSITION = "tap_position";

    private static int tapPosition = NO_POSITION;
    private static int topPosition = NO_POSITION;
    private static int bottomPosition = NO_POSITION;

    private int messageType = Message.TYPE_RECEIVED;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        if (getActivity() == null || getContext() == null)
            return null;
        app = (App) activity.getApplication();
        getContext().getTheme().applyStyle(Themes.INSTANCE.getAppTheme(), true);
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false);
        // activity, context and profile is valid
        b = DataBindingUtil.inflate(inflater, R.layout.messages_list, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        long messageId = -1;
        if (getArguments() != null) {
            messageId = getArguments().getLong("messageId", -1);
        }
        if (messageId != -1) {
            Bundle args = new Bundle();
            args.putLong("messageId", messageId);
            getArguments().remove("messageId");
            activity.loadTarget(MainActivity.TARGET_MESSAGES_DETAILS, args);
            return;
        }

        if (app.profile.getLoginStoreType() == LOGIN_TYPE_LIBRUS && app.profile.getStudentData("accountPassword", null) == null) {
            new MaterialDialog.Builder(activity)
                    .title("Wiadomości w systemie Synergia")
                    .content("Moduł Wiadomości w aplikacji Szkolny.eu jest przeglądarką zasobów szkolnego konta Synergia. Z tego powodu, musisz wpisać swoje hasło do tego konta, aby móc korzystać z tej funkcji.")
                    .positiveText(R.string.ok)
                    .onPositive(((dialog, which) -> {
                        new MaterialDialog.Builder(activity)
                                .title("Zaloguj się")
                                .content(Html.fromHtml("Podaj hasło do konta Synergia z loginem <b>"+app.profile.getStudentData("accountLogin", "???")+"</b>"))
                                .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                .input(null, null, (dialog1, input) -> {
                                    app.profile.putStudentData("accountPassword", input.toString());
                                    Edziennik.getApi(app, app.profile.getLoginStoreType()).syncMessages(activity, new SyncCallback() {
                                        @Override
                                        public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {

                                        }

                                        @Override
                                        public void onSuccess(Context activityContext, ProfileFull profileFull) {
                                            app.profile.putStudentData("accountPassword", input.toString());
                                            app.profileSaveFullAsync(profileFull);
                                            ((MainActivity) activityContext).recreate();
                                        }

                                        @Override
                                        public void onError(Context activityContext, AppError error) {
                                            new Handler(activityContext.getMainLooper()).post(() -> {
                                                app.profile.removeStudentData("accountPassword");
                                                app.profileSaveFullAsync(app.profile);
                                                new MaterialDialog.Builder(activity)
                                                        .title(R.string.login_failed)
                                                        .content(R.string.login_failed_text)
                                                        .positiveText(R.string.ok)
                                                        .neutralText(R.string.report)
                                                        .onNeutral(((dialog2, which1) -> {
                                                            app.apiEdziennik.guiReportError(getActivity(), error, null);
                                                        }))
                                                        .show();
                                            });
                                        }

                                        @Override
                                        public void onProgress(int progressStep) {

                                        }

                                        @Override
                                        public void onActionStarted(int stringResId) {

                                        }
                                    }, app.profile);
                                })
                                .positiveText(R.string.ok)
                                .negativeText(R.string.cancel)
                                .show();
                    }))
                    .show();
        }

        if (getArguments() != null) {
            messageType = getArguments().getInt("messageType", Message.TYPE_RECEIVED);
        }

        /*b.refreshLayout.setOnRefreshListener(() -> {
            activity.syncCurrentFeature(messageType, b.refreshLayout);
        });*/

        messagesAdapter = new MessagesAdapter(app, ((parent, view1, position, id) -> {
            // TODO ANIMATION
            /*tapPosition = position;
            topPosition = ((LinearLayoutManager) b.emailList.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            bottomPosition = ((LinearLayoutManager) b.emailList.getLayoutManager()).findLastCompletelyVisibleItemPosition();

            view1.getGlobalVisibleRect(viewRect);
            ((Transition) MessagesListFragment.this.getExitTransition()).setEpicenterCallback(new Transition.EpicenterCallback() {
                @Override
                public Rect onGetEpicenter(@NonNull Transition transition) {
                    return viewRect;
                }
            });*/

            Bundle args = new Bundle();
            args.putLong("messageId", messagesAdapter.messageList.get(position).id);
            activity.loadTarget(MainActivity.TARGET_MESSAGES_DETAILS, args);

            // KOD W WERSJI 2.7
            // TODO ANIMATION
            /*TransitionSet sharedElementTransition = new TransitionSet()
                    .addTransition(new Fade())
                    .addTransition(new ChangeBounds())
                    .addTransition(new ChangeTransform())
                    .addTransition(new ChangeImageTransform())
                    .setDuration(TRANSITION_DURATION)
                    .setInterpolator(transitionInterpolator);

            MessagesDetailsFragment fragment = new MessagesDetailsFragment();
            Bundle args = new Bundle();
            args.putLong("messageId", messagesAdapter.messageList.get(position).id);
            fragment.setArguments(args);
            fragment.setSharedElementEnterTransition(sharedElementTransition);
            fragment.setSharedElementReturnTransition(sharedElementTransition);*/

            // JAKIS STARSZY KOD
            /*Intent intent = new Intent(activity, MessagesDetailsActivity.class);
            intent.putExtra("item_id", 1);
            intent.putExtra("transition_name", ViewCompat.getTransitionName(view1));


            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    view1,
                    getString(R.string.transition_name)
            );

            TransitionManager.beginDelayedTransition((ViewGroup) view1, sharedElementTransition);
            setEnterTransition(sharedElementTransition);
            setReturnTransition(sharedElementTransition);
            setExitTransition(sharedElementTransition);
            setSharedElementEnterTransition(sharedElementTransition);
            setSharedElementReturnTransition(sharedElementTransition);
            startActivity(intent, options.toBundle());*/

            /*activity.getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .addSharedElement(view1, getString(R.string.transition_name))
                    .commit();*/

        }));


        //tapPosition = savedInstanceState != null ? savedInstanceState.getInt(TAP_POSITION, tapPosition) : tapPosition;

        // May not be the best place to postpone transition. Just an example to demo how reenter transition works.
         // TODO ANIMATION
        //postponeEnterTransition();

        viewParent = (ViewGroup) view.getParent();

        b.emailList.setLayoutManager(new LinearLayoutManager(view.getContext()));
        b.emailList.addItemDecoration(new DividerItemDecoration(view.getContext(), LinearLayoutManager.VERTICAL));
        b.emailList.setAdapter(messagesAdapter);

        if (messageType == Message.TYPE_RECEIVED) {
            app.db.messageDao().getReceived(App.profileId).observe(this, messageFulls -> {
                createMessageList(messageFulls);
            });
        }
        else if (messageType == Message.TYPE_DELETED) {
            app.db.messageDao().getDeleted(App.profileId).observe(this, messageFulls -> {
                createMessageList(messageFulls);
            });
        }
        else if (messageType == Message.TYPE_SENT) {
            app.db.messageDao().getSent(App.profileId).observe(this, messageFulls -> {
                AsyncTask.execute(() -> {
                    List<MessageRecipientFull> messageRecipients = app.db.messageRecipientDao().getAll(App.profileId);
                    List<Long> messageIds = new ArrayList<>();
                    for (MessageFull messageFull: messageFulls) {
                        messageIds.add(messageFull.id);
                    }
                    for (MessageRecipientFull messageRecipientFull: messageRecipients) {
                        if (messageRecipientFull.id == -1)
                            continue;

                        int index = -1;

                        int i = -1;
                        for (long id: messageIds) {
                            //index++;
                            i++;
                            if (id == messageRecipientFull.messageId) {
                                index = i;
                                break;
                            }
                        }

                        if (index >= 0) {
                            MessageFull messageFull = messageFulls.get(index);
                            if (messageFull != null) {
                                messageFull.addRecipient(messageRecipientFull);
                            }
                        }
                    }
                    activity.runOnUiThread(() -> {
                        createMessageList(messageFulls);
                    });
                });
            });
        }


    }

    private void createMessageList(List<MessageFull> messageFulls) {
        b.progressBar.setVisibility(View.GONE);
        b.emailList.setVisibility(View.VISIBLE);
        messagesAdapter.setData(messageFulls);
        // TODO ANIMATION
        /*final ViewTreeObserver observer = viewParent.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                viewParent.getViewTreeObserver().removeOnPreDrawListener(this);
                if (getExitTransition() == null) {
                    setExitTransition(new SlideExplode().setDuration(TRANSITION_DURATION).setInterpolator(transitionInterpolator));
                }

                LinearLayoutManager layoutManager = (LinearLayoutManager) b.emailList.getLayoutManager();
                View view2 = layoutManager != null ? layoutManager.findViewByPosition(tapPosition) : null;
                if (view2 != null) {
                    view2.getGlobalVisibleRect(viewRect);
                    ((Transition) getExitTransition()).setEpicenterCallback(new Transition.EpicenterCallback() {
                        @Override
                        public Rect onGetEpicenter(@NonNull Transition transition) {
                            return viewRect;
                        }
                    });
                }

                d("MessagesList", "topPosition "+topPosition);
                d("MessagesList", "tapPosition "+tapPosition);
                d("MessagesList", "bottomPosition "+bottomPosition);
                if (tapPosition != NO_POSITION && layoutManager != null) {
                    d("MessageList", "Scrolling");

                    if (bottomPosition > layoutManager.findLastCompletelyVisibleItemPosition()) {
                        b.emailList.scrollToPosition(bottomPosition);
                    }
                    else if (topPosition < layoutManager.findFirstCompletelyVisibleItemPosition()) {
                        b.emailList.scrollToPosition(topPosition);
                    }
                    else {
                        b.emailList.scrollToPosition(tapPosition);
                    }

                    tapPosition = NO_POSITION;
                    topPosition = NO_POSITION;
                    bottomPosition = NO_POSITION;
                }

                startPostponedEnterTransition();
                return true;
            }
        });*/
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        d("MessagesList", "onSaveInstanceState position "+tapPosition);
        outState.putInt(TAP_POSITION, tapPosition);
    }
}
