package pl.szczodrzynski.edziennik.ui.modules.messages;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.afollestad.materialdialogs.MaterialDialog;
import com.hootsuite.nachos.ChipConfiguration;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;
import com.hootsuite.nachos.chip.ChipInfo;
import com.hootsuite.nachos.chip.ChipSpan;
import com.hootsuite.nachos.chip.ChipSpanChipCreator;
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer;
import com.hootsuite.nachos.validator.IllegalCharacterIdentifier;
import com.mikepenz.iconics.IconicsColor;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.IconicsSize;
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher;
import pl.szczodrzynski.edziennik.databinding.ActivityComposeMessageBinding;
import pl.szczodrzynski.edziennik.utils.Colors;
import pl.szczodrzynski.edziennik.utils.Themes;

public class MessagesComposeActivity extends AppCompatActivity {

    private static final String TAG = "MessageCompose";
    private App app;
    private ActivityComposeMessageBinding b;
    private List<Teacher> teachers = new ArrayList<>();
    private ActionBar actionBar;
    private MessagesComposeInfo composeInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App)getApplication();
        setTheme(Themes.INSTANCE.getAppTheme());
        b = DataBindingUtil.inflate(getLayoutInflater(), R.layout.activity_compose_message, null, false);
        setContentView(b.getRoot());

        /*composeInfo = Edziennik.getApi(app, app.profile.getLoginStoreType()).getComposeInfo(app.profile);

        Toolbar toolbar = b.toolbar;
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.messages_compose_title);
        }

        List<Teacher> categories = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            categories.add(new Teacher(-1, -1*i, Teacher.typeString(this, i), ""));
        }

        Edziennik.getApi(app, app.profile.getLoginStoreType()).getRecipientList(this, new SyncCallback() {
            @Override public void onLoginFirst(List<Profile> profileList, LoginStore loginStore) { }
            @Override public void onSuccess(Context activityContext, ProfileFull profileFull) { }
            @Override public void onProgress(int progressStep) { }
            @Override public void onActionStarted(int stringResId) { }
            @Override
            public void onError(Context activityContext, AppError error) {
                new Handler(activityContext.getMainLooper()).post(() -> {
                    app.apiEdziennik.guiShowErrorDialog(MessagesComposeActivity.this, error, R.string.messages_recipient_list_download_error);
                });
            }
        }, app.profile, teacherList -> {
            teachers.clear();
            for (Teacher teacher: teacherList) {
                if (teacher.loginId != null)
                    teachers.add(teacher);
            }
            teachers.addAll(categories);
            MessagesComposeSuggestionAdapter adapter = new MessagesComposeSuggestionAdapter(this, teachers);
            //ArrayAdapter<Teacher> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, teachers);
            b.nachoTextView.setAdapter(adapter);
        });*/
        /*app.db.teacherDao().getAllTeachers(App.profileId).observe(this, teachers -> {

        });*/

        /*int[][] states = new int[][] {
                new int[] {}
        };
        int[] colors = new int[] {
                getResources().getColor(ThemeUtils.getChipColorRes())
        };*/
        //ColorStateList chipStateList = new ColorStateList(states, colors);

        b.nachoTextView.setChipTokenizer(new SpanChipTokenizer<>(this, new ChipSpanChipCreator() {
            @Override
            public ChipSpan createChip(@NonNull Context context, @NonNull CharSequence text, Object data) {
                Teacher teacher = (Teacher) data;
                if (teacher.id <= 0) {
                    int type = (int) (teacher.id * -1);
                    List<Teacher> category = new ArrayList<>();
                    List<String> categoryNames = new ArrayList<>();
                    for (Teacher teacher1: teachers) {
                        if (teacher1.isType(type)) {
                            category.add(teacher1);
                            categoryNames.add(teacher1.getFullName());
                        }
                    }
                    new MaterialDialog.Builder(MessagesComposeActivity.this)
                            .title(R.string.messages_compose_recipients_title)
                            .content(getString(R.string.messages_compose_recipients_text_format, Teacher.typeString(MessagesComposeActivity.this, type)))
                            .items(categoryNames)
                            .itemsCallbackMultiChoice(null, ((dialog, which, text1) -> {
                                List<ChipInfo> chipInfoList = new ArrayList<>();
                                for (int index: which) {
                                    Teacher selected = category.get(index);
                                    selected.image = MessagesUtils.getProfileImage(48, 24, 16, 12, 1, selected.getFullName());
                                    chipInfoList.add(new ChipInfo(selected.getFullName(), selected));
                                }
                                b.nachoTextView.addTextWithChips(chipInfoList);
                                return true;
                            }))
                            .positiveText(R.string.ok)
                            .negativeText(R.string.cancel)
                            .show();
                    return null;
                }
                ChipSpan chipSpan = new ChipSpan(context, text, new BitmapDrawable(context.getResources(), teacher.image), teacher);
                chipSpan.setIconBackgroundColor(Colors.stringToMaterialColor(teacher.getFullName()));
                return chipSpan;
            }

            @Override
            public void configureChip(@NonNull ChipSpan chip, @NonNull ChipConfiguration chipConfiguration) {
                super.configureChip(chip, chipConfiguration);
                //chip.setBackgroundColor(chipStateList);
                chip.setTextColor(Themes.INSTANCE.getPrimaryTextColor(MessagesComposeActivity.this));
            }
        }, ChipSpan.class));


        b.nachoTextView.setIllegalCharacterIdentifier(new IllegalCharacterIdentifier() {
            @Override
            public boolean isCharacterIllegal(Character c) {
                return c.toString().matches("[\\n;:_ ]");
            }
        });
        //b.nachoTextView.addChipTerminator('\n', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_ALL);
        //b.nachoTextView.addChipTerminator(' ', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR);
        //b.nachoTextView.addChipTerminator(';', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_CURRENT_TOKEN);
        //b.nachoTextView.setNachoValidator(new ChipifyingNachoValidator());
        //b.nachoTextView.enableEditChipOnTouch(false, false);
        //b.nachoTextView.disableEditChipOnTouch();
        b.nachoTextView.setOnChipClickListener(new NachoTextView.OnChipClickListener() {
            @Override
            public void onChipClick(Chip chip, MotionEvent motionEvent) {
                Toast.makeText(app, "onChipClick: " + chip.getText(), Toast.LENGTH_SHORT).show();
            }
        });
        b.nachoTextView.setOnChipRemoveListener(new NachoTextView.OnChipRemoveListener() {
            @Override
            public void onChipRemove(Chip chip) {
                Log.d(TAG, "onChipRemoved: " + chip.getText());
                b.nachoTextView.setSelection(b.nachoTextView.getText().length());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_compose, menu);

        menu.findItem(R.id.action_send).setIcon(
                new IconicsDrawable(this, CommunityMaterial.Icon2.cmd_send)
                        .actionBar()
                        .color(IconicsColor.colorInt(Color.WHITE))
                        .size(IconicsSize.dp(20))
        );
        menu.findItem(R.id.action_attachment).setIcon(
                new IconicsDrawable(this, CommunityMaterial.Icon.cmd_attachment)
                        .actionBar()
                        .color(IconicsColor.colorInt(Color.WHITE))
                        .size(IconicsSize.dp(20))
        );
        menu.findItem(R.id.action_attachment).setVisible(composeInfo.maxAttachmentNumber != 0);
        return true;
    }
}
