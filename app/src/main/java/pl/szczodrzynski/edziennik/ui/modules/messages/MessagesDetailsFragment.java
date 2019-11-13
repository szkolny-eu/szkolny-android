package pl.szczodrzynski.edziennik.ui.modules.messages;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.chip.Chip;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;
import com.theartofdev.edmodo.cropper.CropImage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull;
import pl.szczodrzynski.edziennik.databinding.MessagesDetailsBinding;
import pl.szczodrzynski.edziennik.ui.modules.error.ErrorDialog;
import pl.szczodrzynski.edziennik.utils.Themes;
import pl.szczodrzynski.edziennik.utils.Utils;

import static android.app.Activity.RESULT_OK;
import static pl.szczodrzynski.edziennik.utils.Utils.getResizedBitmap;
import static pl.szczodrzynski.edziennik.utils.Utils.getStringFromFile;
import static pl.szczodrzynski.edziennik.utils.Utils.readableFileSize;

public class MessagesDetailsFragment extends Fragment {
    private long messageId = -1;

    private App app = null;
    private MainActivity activity = null;
    private MessagesDetailsBinding b = null;

    private MessageFull message;

    private List<Attachment> attachmentList = new ArrayList<>();

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
        b = DataBindingUtil.inflate(inflater, R.layout.messages_details, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (app == null || app.profile == null || activity == null || b == null || !isAdded())
            return;

        if (getArguments() != null)
            messageId = getArguments().getLong("messageId", -1);

        b.messageContent.setVisibility(View.GONE);

        /*if (messageId != -1) {
            AsyncTask.execute(() -> {
                if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                    return;

                MessageFull messageRaw = app.db.messageDao().getById(App.profileId, messageId);
                Edziennik.getApi(app, app.profile.getLoginStoreType()).getMessage(activity, new SyncCallback() {
                    @Override public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) { }
                    @Override public void onSuccess(Context activityContext, ProfileFull profileFull) { }
                    @Override public void onProgress(int progressStep) { }
                    @Override public void onActionStarted(int stringResId) { }
                    @Override
                    public void onError(Context activityContext, AppError error) {
                        new Handler(activityContext.getMainLooper()).post(() -> {
                            app.apiEdziennik.guiShowErrorDialog(activity, error, R.string.messages_download_error);
                        });
                    }
                }, app.profile, messageRaw, messageFull -> {
                    if (app == null || app.profile == null || activity == null || b == null || !isAdded())
                        return;

                    message = messageFull;
                    if (message.body == null) {
                        return;
                    }
                    b.messageBody.setText(Html.fromHtml(message.body.replaceAll("\\[META:[A-z0-9]+;[0-9-]+]", "")));
                    b.progress.setVisibility(View.GONE);
                    Anim.fadeIn(b.messageContent, 200, null);

                    MessagesFragment.Companion.setPageSelection(Math.min(message.type, 1));

                    MessagesUtils.MessageInfo messageInfo = MessagesUtils.getMessageInfo(app, message, 40, 20, 14, 10);
                    b.messageProfileBackground.setImageBitmap(messageInfo.profileImage);
                    b.messageSender.setText(messageInfo.profileName);

                    b.messageSubject.setText(message.subject);
                    b.messageDate.setText(getString(R.string.messages_date_time_format, Date.fromMillis(message.addedDate).getFormattedStringShort(), Time.fromMillis(message.addedDate).getStringHM()));

                    StringBuilder messageRecipients = new StringBuilder("<ul>");
                    for (MessageRecipientFull recipient: message.recipients) {
                        if (recipient.readDate == -1) messageRecipients.append(getString(
                                R.string.messages_recipients_list_unknown_state_format,
                                recipient.fullName
                        ));
                        else if (recipient.readDate == 0) messageRecipients.append(getString(
                                R.string.messages_recipients_list_unread_format,
                                recipient.fullName
                                ));
                        else if (recipient.readDate == 1) messageRecipients.append(getString(
                                R.string.messages_recipients_list_read_unknown_date_format,
                                recipient.fullName
                        ));
                        else messageRecipients.append(getString(
                                R.string.messages_recipients_list_read_format,
                                recipient.fullName,
                                Date.fromMillis(recipient.readDate).getFormattedString(),
                                Time.fromMillis(recipient.readDate).getStringHM()
                                ));
                    }
                    messageRecipients.append("</ul>");
                    b.messageRecipients.setText(Html.fromHtml(messageRecipients.toString()));

                    if (message.attachmentIds != null) {
                        // there are some attachments: attachmentIds, attachmentNames, attachmentSizes
                        ViewGroup insertPoint = b.messageAttachments;

                        FrameLayout.LayoutParams chipLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        chipLayoutParams.setMargins(0, Utils.dpToPx(8), 0, Utils.dpToPx(8));

                        FrameLayout.LayoutParams progressLayoutParams = new FrameLayout.LayoutParams(Utils.dpToPx(18), Utils.dpToPx(18));
                        progressLayoutParams.setMargins(Utils.dpToPx(8), 0, Utils.dpToPx(8), 0);
                        progressLayoutParams.gravity = END | CENTER_VERTICAL;

                        // CREATE VIEWS AND AN OBJECT FOR EVERY ATTACHMENT

                        int attachmentIndex = 0;
                        for (String attachmentName: message.attachmentNames) {
                            long messageId = message.id;
                            long attachmentId = message.attachmentIds.get(attachmentIndex);
                            long attachmentSize = message.attachmentSizes.get(attachmentIndex);
                            // create the parent
                            FrameLayout attachmentLayout = new FrameLayout(b.getRoot().getContext());

                            Chip attachmentChip = new Chip(attachmentLayout.getContext());
                            //attachmentChip.setChipBackgroundColorResource(ThemeUtils.getChipColorRes());
                            attachmentChip.setLayoutParams(chipLayoutParams);
                            attachmentChip.setHeight(Utils.dpToPx(40));
                            // show the file size or not
                            if (attachmentSize == -1)
                                attachmentChip.setText(getString(R.string.messages_attachment_no_size_format, attachmentName));
                            else
                                attachmentChip.setText(getString(R.string.messages_attachment_format, attachmentName, readableFileSize(attachmentSize)));
                            attachmentChip.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                            // create an icon for the attachment
                            IIcon icon = CommunityMaterial.Icon.cmd_file;
                            switch (Utils.getExtensionFromFileName(attachmentName)) {
                                case "txt":
                                    icon = CommunityMaterial.Icon.cmd_file_document;
                                    break;
                                case "doc":
                                case "docx":
                                case "odt":
                                case "rtf":
                                    icon = CommunityMaterial.Icon.cmd_file_word;
                                    break;
                                case "xls":
                                case "xlsx":
                                case "ods":
                                    icon = CommunityMaterial.Icon.cmd_file_excel;
                                    break;
                                case "ppt":
                                case "pptx":
                                case "odp":
                                    icon = CommunityMaterial.Icon.cmd_file_powerpoint;
                                    break;
                                case "pdf":
                                    icon = CommunityMaterial.Icon.cmd_file_pdf;
                                    break;
                                case "mp3":
                                case "wav":
                                case "aac":
                                    icon = CommunityMaterial.Icon.cmd_file_music;
                                    break;
                                case "mp4":
                                case "avi":
                                case "3gp":
                                case "mkv":
                                case "flv":
                                    icon = CommunityMaterial.Icon.cmd_file_video;
                                    break;
                                case "jpg":
                                case "jpeg":
                                case "png":
                                case "bmp":
                                case "gif":
                                    icon = CommunityMaterial.Icon.cmd_file_image;
                                    break;
                                case "zip":
                                case "rar":
                                case "tar":
                                case "7z":
                                    icon = CommunityMaterial.Icon.cmd_file_lock;
                                    break;
                            }
                            attachmentChip.setChipIcon(new IconicsDrawable(activity).color(IconicsColor.colorRes(R.color.colorPrimary)).icon(icon).size(IconicsSize.dp(26)));
                            attachmentChip.setCloseIcon(new IconicsDrawable(activity).icon(CommunityMaterial.Icon.cmd_check).size(IconicsSize.dp(18)).color(IconicsColor.colorInt(Utils.getAttr(activity, android.R.attr.textColorPrimary))));
                            attachmentChip.setCloseIconVisible(false);
                            // set the object's index in the attachmentList as the tag
                            attachmentChip.setTag(attachmentIndex);
                            attachmentChip.setOnClickListener(v -> {
                                if (v.getTag() instanceof Integer) {
                                    downloadAttachment((Integer) v.getTag());
                                }
                            });
                            attachmentLayout.addView(attachmentChip);

                            ProgressBar attachmentProgress = new ProgressBar(attachmentLayout.getContext());
                            attachmentProgress.setLayoutParams(progressLayoutParams);
                            attachmentProgress.setVisibility(View.GONE);
                            attachmentLayout.addView(attachmentProgress);

                            insertPoint.addView(attachmentLayout);
                            // create an object and add to the list
                            Attachment a = new Attachment(App.profileId, messageId, attachmentId, attachmentName, attachmentSize, attachmentLayout, attachmentChip, attachmentProgress);
                            attachmentList.add(a);
                            // check if the file is already downloaded. Show the check icon if necessary and set `downloaded` to true.
                            checkAttachment(a);

                            attachmentIndex++;
                        }
                    }
                    else {
                        // no attachments found
                        b.messageAttachmentsTitle.setVisibility(View.GONE);
                    }
                });

            });
        }*/

        // click to expand subject and sender
        b.messageSubject.setOnClickListener(v -> {
            if (b.messageSubject.getMaxLines() == 2) {
                b.messageSubject.setMaxLines(30);
            }
            else {
                b.messageSubject.setMaxLines(2);
            }
        });
        b.messageSender.setOnClickListener(v -> {
            if (b.messageSender.getMaxLines() == 3) {
                b.messageSender.setMaxLines(30);
            }
            else {
                b.messageSender.setMaxLines(3);
            }
        });

        // message close button
        b.messageClose.setImageDrawable(new IconicsDrawable(activity).icon(CommunityMaterial.Icon2.cmd_window_close).color(IconicsColor.colorInt(Utils.getAttr(activity, android.R.attr.textColorSecondary))).size(IconicsSize.dp(12)));
        b.messageClose.setOnClickListener(v -> {
            activity.navigateUp();
        });

        // enter, exit transitions
        //setExitTransition(new Fade());

        /*View content = b.getRoot();
        content.setAlpha(0f);

        ValueAnimator animator = ObjectAnimator.ofFloat(content, View.ALPHA, 0f, 1f);
        animator.setStartDelay(50);
        animator.setDuration(150);
        animator.start();*/
    }

    private void checkAttachment(Attachment a) {

        File storageDir = Environment.getExternalStoragePublicDirectory("Szkolny.eu");
        storageDir.mkdirs();

        File attachmentDataFile = new File(storageDir, "."+a.profileId+"_"+a.messageId+"_"+a.attachmentId);
        if (attachmentDataFile.exists()) {
            try {
                String attachmentFileName = getStringFromFile(attachmentDataFile);
                File attachmentFile = new File(attachmentFileName);
                if (attachmentFile.exists()) {
                    a.downloaded = attachmentFileName;
                    a.chip.setCloseIconVisible(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                new ErrorDialog(activity, e);
            }
        }
    }

    private void downloadAttachment(int index) {
        Attachment a = attachmentList.get(index);

        if (a.downloaded != null) {
            Utils.openFile(activity, new File(a.downloaded));
            return;
        }

        a.chip.setEnabled(false);
        a.chip.setTextColor(Themes.INSTANCE.getSecondaryTextColor(activity));
        a.progressBar.setVisibility(View.VISIBLE);

        File storageDir = Utils.getStorageDir();

        /*Edziennik.getApi(app, app.profile.getLoginStoreType()).getAttachment(activity, new SyncCallback() {
            @Override
            public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) {

            }

            @Override
            public void onSuccess(Context activityContext, ProfileFull profileFull) {

            }

            @Override
            public void onError(Context activityContext, AppError error) {
                new Handler(activityContext.getMainLooper()).post(() -> {
                    a.progressBar.setVisibility(View.GONE);
                    a.chip.setEnabled(true);
                    a.chip.setTextColor(Themes.INSTANCE.getPrimaryTextColor(activity));
                    a.chip.setCloseIconVisible(false);
                    app.apiEdziennik.guiShowErrorDialog(activity, error, R.string.messages_attachment_cannot_download);
                });
            }

            @Override
            public void onProgress(int progressStep) {

            }

            @Override
            public void onActionStarted(int stringResId) {

            }
        }, app.profile, message, a.attachmentId, builder ->
                builder.callbackThreadMode(im.wangchao.mhttp.ThreadMode.SENDING)
                .callback(new FileCallbackHandler(new File(storageDir, a.attachmentName)) {
                    @Override
                    public void onSuccess(File file, Response response) {
                        AttachmentEvent event = new AttachmentEvent();
                        event.profileId = a.profileId;
                        event.messageId = a.messageId;
                        event.attachmentId = a.attachmentId;
                        event.eventType = AttachmentEvent.TYPE_FINISHED;
                        event.fileName = file.getAbsolutePath();

                        try {
                            File attachmentDataFile = new File(Utils.getStorageDir(), "."+event.profileId+"_"+event.messageId+"_"+event.attachmentId);
                            Utils.writeStringToFile(attachmentDataFile, event.fileName);
                        } catch (IOException e) {
                            event.eventType = AttachmentEvent.TYPE_ERROR;
                            event.exception = e;
                        }
                        finally {
                            EventBus.getDefault().post(event);
                        }
                    }

                    @Override
                    public void onFailure(Response response, Throwable throwable) {
                        AttachmentEvent event = new AttachmentEvent();
                        event.profileId = a.profileId;
                        event.messageId = a.messageId;
                        event.attachmentId = a.attachmentId;
                        event.eventType = AttachmentEvent.TYPE_ERROR;
                        event.exception = new Exception(throwable);
                        EventBus.getDefault().post(event);
                    }

                    @Override
                    public void onProgress(long bytesWritten, long bytesTotal) {
                        AttachmentEvent event = new AttachmentEvent();
                        event.profileId = a.profileId;
                        event.messageId = a.messageId;
                        event.attachmentId = a.attachmentId;
                        event.eventType = AttachmentEvent.TYPE_PROGRESS;
                        event.progress = (float)bytesWritten / (float)bytesTotal * 100.0f;
                        EventBus.getDefault().post(event);
                    }
                })
                .build()
                .enqueue());*/
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void downloadAttachmentEvent(AttachmentEvent event) {
        try {
            for (Attachment a: attachmentList) {
                if (a.profileId == event.profileId
                        && a.messageId == event.messageId
                        && a.attachmentId == event.attachmentId) {
                    if (event.eventType == AttachmentEvent.TYPE_PROGRESS) {
                        // show downloading progress
                        a.chip.setText(getString(R.string.messages_attachment_downloading_format, a.attachmentName, Math.round(event.progress)));
                    }
                    else if (event.eventType == AttachmentEvent.TYPE_FINISHED) {
                        // save the downloaded file name
                        a.downloaded = event.fileName;
                        // set the correct name (and size)
                        if (a.attachmentSize == -1)
                            a.chip.setText(getString(R.string.messages_attachment_no_size_format, a.attachmentName));
                        else
                            a.chip.setText(getString(R.string.messages_attachment_format, a.attachmentName, readableFileSize(a.attachmentSize)));
                        // hide the progress bar and show a tick icon
                        a.progressBar.setVisibility(View.GONE);
                        a.chip.setEnabled(true);
                        a.chip.setTextColor(Themes.INSTANCE.getPrimaryTextColor(activity));
                        a.chip.setCloseIconVisible(true);
                        // open the file
                        Utils.openFile(activity, new File(a.downloaded));
                    }
                    else if (event.eventType == AttachmentEvent.TYPE_ERROR) {
                        a.progressBar.setVisibility(View.GONE);
                        a.chip.setEnabled(true);
                        a.chip.setTextColor(Themes.INSTANCE.getPrimaryTextColor(activity));
                        a.chip.setCloseIconVisible(false);
                        new MaterialDialog.Builder(activity)
                                .title(R.string.messages_attachment_cannot_download)
                                .content(R.string.messages_attachment_cannot_download_text)
                                .positiveText(R.string.ok)
                                .neutralColor(R.string.report)
                                .onNeutral((dialog, which) -> {
                                    new ErrorDialog(activity, event.exception);
                                })
                                .show();
                    }
                }
            }
        }
        catch (Exception e) {
            new ErrorDialog(activity, e);
        }
    }

    private class Attachment {
        int profileId;
        long messageId;
        long attachmentId;
        String attachmentName;
        long attachmentSize;
        FrameLayout parent;
        Chip chip;
        ProgressBar progressBar;
        /**
         * An absolute path of the downloaded file. {@code null} if not downloaded yet.
         */
        String downloaded = null;

        public Attachment(int profileId, long messageId, long attachmentId, String attachmentName, long attachmentSize, FrameLayout parent, Chip chip, ProgressBar progressBar) {
            this.profileId = profileId;
            this.messageId = messageId;
            this.attachmentId = attachmentId;
            this.attachmentName = attachmentName;
            this.attachmentSize = attachmentSize;
            this.parent = parent;
            this.chip = chip;
            this.progressBar = progressBar;
        }
    }

    public static class AttachmentEvent {
        public int profileId;
        public long messageId;
        public long attachmentId;

        public static final int TYPE_PROGRESS = 0;
        public static final int TYPE_FINISHED = 1;
        public static final int TYPE_ERROR = 2;
        public int eventType = TYPE_PROGRESS;

        public float progress = 0.0f;
        public String fileName = null;
        public Exception exception = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            Uri uri = result.getUri();

            File photoFile = new File(uri.getPath());
            File destFile = new File(getContext().getFilesDir(),"teacher_"+message.senderId+".jpg");
            if (destFile.exists()) {
                destFile.delete();
                destFile = new File(getContext().getFilesDir(),"teacher_"+message.senderId+".jpg");
            }


            if (result.getCropRect().width() > 512) {
                Bitmap bigImage = BitmapFactory.decodeFile(uri.getPath());
                Bitmap smallImage = getResizedBitmap(bigImage, 512, 512);
                try (FileOutputStream out = new FileOutputStream(destFile)) {
                    smallImage.compress(Bitmap.CompressFormat.JPEG, 80, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                    Bitmap profileImage;
                    profileImage = BitmapFactory.decodeFile(destFile.getAbsolutePath());
                    profileImage = ThumbnailUtils.extractThumbnail(profileImage, Math.min(profileImage.getWidth(), profileImage.getHeight()), Math.min(profileImage.getWidth(), profileImage.getHeight()));
                    RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(app.getResources(), profileImage);
                    roundDrawable.setCircular(true);
                    b.messageProfileImage.setImageDrawable(roundDrawable);
                    if (app.appConfig.teacherImages == null) {
                        app.appConfig.teacherImages = new HashMap<>();
                    }
                    app.appConfig.teacherImages.put(message.senderId, true);
                    app.saveConfig("teacherImages");
                    if (photoFile.exists()) {
                        photoFile.delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                if (photoFile.renameTo(destFile)) {
                    // success
                    Bitmap profileImage;
                    profileImage = BitmapFactory.decodeFile(destFile.getAbsolutePath());
                    profileImage = ThumbnailUtils.extractThumbnail(profileImage, Math.min(profileImage.getWidth(), profileImage.getHeight()), Math.min(profileImage.getWidth(), profileImage.getHeight()));
                    RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(app.getResources(), profileImage);
                    roundDrawable.setCircular(true);
                    b.messageProfileImage.setImageDrawable(roundDrawable);
                    if (app.appConfig.teacherImages == null) {
                        app.appConfig.teacherImages = new HashMap<>();
                    }
                    app.appConfig.teacherImages.put(message.senderId, true);
                    app.saveConfig("teacherImages");
                }
                else {
                    // not this time
                    Toast.makeText(app, R.string.error_occured, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}

/*if (message.type == TYPE_RECEIVED) {
                        b.messageSender.setText(message.senderFullName);
                    }
                    else if (message.type == TYPE_SENT && message.recipients != null && message.recipients.size() > 0) {
                        StringBuilder senderText = new StringBuilder();
                        boolean first = true;
                        for (MessageRecipientFull recipient: message.recipients) {
                            if (!first) {
                                senderText.append(", ");
                            }
                            first = false;
                            senderText.append(recipient.fullName);
                        }
                        b.messageSender.setText(senderText.toString());
                    }

                    if (message.type == TYPE_RECEIVED) {
                        if (app.appConfig.teacherImages != null && app.appConfig.teacherImages.size() > 0 && app.appConfig.teacherImages.containsKey(message.senderId)) {
                            Bitmap profileImage;
                            profileImage = BitmapFactory.decodeFile(app.getFilesDir().getAbsolutePath()+"/teacher_"+message.senderId+".jpg");
                            profileImage = ThumbnailUtils.extractThumbnail(profileImage, Math.min(profileImage.getWidth(), profileImage.getHeight()), Math.min(profileImage.getWidth(), profileImage.getHeight()));
                            RoundedBitmapDrawable roundDrawable = RoundedBitmapDrawableFactory.create(app.getResources(), profileImage);
                            roundDrawable.setCircular(true);
                            b.messageProfileImage.setImageDrawable(roundDrawable);
                        }
                        else {
                            int color = Colors.stringToMaterialColor(message.senderFullName);
                            b.messageProfileBackground.getDrawable().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
                            b.messageProfileName.setTextColor(ColorUtils.blendARGB(Colors.legibleTextColor(color), color, 0.30f));
                            if (message.senderFullName != null) {
                                String[] nameParts = message.senderFullName.split(" ");
                                b.messageProfileName.setText(nameParts[0].toUpperCase().charAt(0) + "" + nameParts[1].toUpperCase().charAt(0));
                            }
                            else {
                                b.messageProfileName.setText("N");
                            }
                        }

                        View.OnClickListener onClickListener = v -> new MaterialDialog.Builder(activity)
                                .title(R.string.settings_profile_change_title)
                                .items(
                                        getString(R.string.settings_profile_change_image),
                                        getString(R.string.settings_profile_remove_image)
                                )
                                .itemsCallback((dialog, itemView, position, text) -> {
                                    switch (position) {
                                        case 0:
                                            CropImage.activity()
                                                    .setAspectRatio(1, 1)
                                                    //.setMaxCropResultSize(512, 512)
                                                    .setCropShape(CropImageView.CropShape.OVAL)
                                                    .setGuidelines(CropImageView.Guidelines.ON)
                                                    .start(activity, MessagesDetailsFragment.this);
                                            break;
                                        case 1:
                                            if (app.appConfig.teacherImages != null) {
                                                app.appConfig.teacherImages.remove(message.senderId);
                                                app.saveConfig("teacherImages");
                                            }
                                            break;
                                    }
                                })
                                .negativeText(R.string.cancel)
                                .show();
                        b.messageSender.setOnClickListener(onClickListener);
                        b.messageProfileBackground.setOnClickListener(onClickListener);
                    }*/
