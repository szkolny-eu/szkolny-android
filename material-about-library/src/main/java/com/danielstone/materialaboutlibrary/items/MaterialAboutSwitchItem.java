package com.danielstone.materialaboutlibrary.items;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;

import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.danielstone.materialaboutlibrary.R;
import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder;
import com.danielstone.materialaboutlibrary.util.ViewTypeManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.view.View.GONE;

public class MaterialAboutSwitchItem extends MaterialAboutItem {

    public static final int GRAVITY_TOP = 0;
    public static final int GRAVITY_MIDDLE = 1;
    public static final int GRAVITY_BOTTOM = 2;
    private CharSequence text = null;
    private int textRes = 0;
    private CharSequence subText = null;
    private int subTextRes = 0;
    private CharSequence subTextChecked = null;
    private int subTextCheckedRes = 0;
    private Drawable icon = null;
    private int iconRes = 0;
    private boolean showIcon = true;
    private int iconGravity = GRAVITY_MIDDLE;
    private boolean checked = false;
    private int tag = -1;
    private MaterialAboutItemOnChangeAction onChangeAction = null;

    private MaterialAboutSwitchItem(Builder builder) {
        super();
        this.text = builder.text;
        this.textRes = builder.textRes;

        this.subText = builder.subText;
        this.subTextRes = builder.subTextRes;

        this.subTextChecked = builder.subTextChecked;
        this.subTextCheckedRes = builder.subTextCheckedRes;

        this.icon = builder.icon;
        this.iconRes = builder.iconRes;

        this.showIcon = builder.showIcon;

        this.iconGravity = builder.iconGravity;

        this.checked = builder.checked;

        this.tag = builder.tag;

        this.onChangeAction = builder.onChangeAction;
    }

    public MaterialAboutSwitchItem(CharSequence text, CharSequence subText, Drawable icon, MaterialAboutItemOnChangeAction onChangeAction) {
        this.text = text;
        this.subText = subText;
        this.icon = icon;
        this.onChangeAction = onChangeAction;
    }

    public MaterialAboutSwitchItem(CharSequence text, CharSequence subText, Drawable icon) {
        this.text = text;
        this.subText = subText;
        this.icon = icon;
    }

    public MaterialAboutSwitchItem(int textRes, int subTextRes, int iconRes, MaterialAboutItemOnChangeAction onChangeAction) {
        this.textRes = textRes;
        this.subTextRes = subTextRes;
        this.iconRes = iconRes;
        this.onChangeAction = onChangeAction;
    }

    public MaterialAboutSwitchItem(int textRes, int subTextRes, int iconRes) {
        this.textRes = textRes;
        this.subTextRes = subTextRes;
        this.iconRes = iconRes;
    }

    public static void setupItem(MaterialAboutSwitchItemViewHolder holder, MaterialAboutSwitchItem item, Context context) {
        holder.switchItem = item;

        CharSequence text = item.getText();
        int textRes = item.getTextRes();

        holder.text.setVisibility(View.VISIBLE);
        if (text != null) {
            holder.text.setText(text);
        } else if (textRes != 0) {
            holder.text.setText(textRes);
        } else {
            holder.text.setVisibility(GONE);
        }

        CharSequence subText = item.getSubText();
        int subTextRes = item.getSubTextRes();

        holder.subText.setVisibility(View.VISIBLE);
        if (subText != null) {
            holder.subText.setText(subText);
        } else if (subTextRes != 0) {
            holder.subText.setText(subTextRes);
        } else {
            holder.subText.setVisibility(GONE);
        }

        if (item.shouldShowIcon()) {
            holder.icon.setVisibility(View.VISIBLE);
            Drawable drawable = item.getIcon();
            int drawableRes = item.getIconRes();
            if (drawable != null) {
                holder.icon.setImageDrawable(drawable);
            } else if (drawableRes != 0) {
                holder.icon.setImageResource(drawableRes);
            }
        } else {
            holder.icon.setVisibility(GONE);
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.icon.getLayoutParams();
        switch (item.getIconGravity()) {
            case MaterialAboutSwitchItem.GRAVITY_TOP:
                params.gravity = Gravity.TOP;
                break;
            case MaterialAboutSwitchItem.GRAVITY_MIDDLE:
                params.gravity = Gravity.CENTER_VERTICAL;
                break;
            case MaterialAboutSwitchItem.GRAVITY_BOTTOM:
                params.gravity = Gravity.BOTTOM;
                break;
        }
        holder.icon.setLayoutParams(params);

        int pL = 0, pT = 0, pR = 0, pB = 0;
        if (Build.VERSION.SDK_INT < 21) {
            pL = holder.view.getPaddingLeft();
            pT = holder.view.getPaddingTop();
            pR = holder.view.getPaddingRight();
            pB = holder.view.getPaddingBottom();
        }

        holder.setChecked(item.getChecked());

        if (item.getOnChangeAction() != null) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            holder.view.setBackgroundResource(outValue.resourceId);
        } else {
            holder.view.setBackgroundResource(0);
        }
        holder.setOnChangeAction(item.getOnChangeAction());

        if (Build.VERSION.SDK_INT < 21) {
            holder.view.setPadding(pL, pT, pR, pB);
        }
    }

    public static MaterialAboutItemViewHolder getViewHolder(View view) {
        return new MaterialAboutSwitchItemViewHolder(view);
    }

    public static class MaterialAboutSwitchItemViewHolder extends MaterialAboutItemViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        public final View view;
        public final ImageView icon;
        public final TextView text;
        public final TextView subText;
        public final SwitchCompat switchView;
        private MaterialAboutItemOnChangeAction onChangeAction;
        private MaterialAboutSwitchItem switchItem;

        MaterialAboutSwitchItemViewHolder(View view) {
            super(view);
            this.view = view;
            icon = view.findViewById(R.id.mal_item_image);
            text = view.findViewById(R.id.mal_item_text);
            subText = view.findViewById(R.id.mal_action_item_subtext);
            switchView = view.findViewById(R.id.mal_switch);
        }

        public void setOnChangeAction(MaterialAboutItemOnChangeAction onChangeAction) {
            this.onChangeAction = onChangeAction;
            view.setOnClickListener(onChangeAction != null ? this : null);
            switchView.setOnCheckedChangeListener(this);
        }

        public void setChecked(boolean checked) {
            switchView.setOnCheckedChangeListener(null);
            switchView.setChecked(checked);
            switchView.setOnCheckedChangeListener(this);
            switchItem.setChecked(checked);
            updateSubText(checked);
        }

        public void updateSubText(boolean checked) {
            if (checked && switchItem.subTextChecked != null) {
                subText.setText(switchItem.subTextChecked);
            }
            else if (checked && switchItem.subTextCheckedRes != 0) {
                subText.setText(switchItem.subTextCheckedRes);
            }
            else if (switchItem.subText != null) {
                subText.setText(switchItem.subText);
            }
            else if (switchItem.subTextRes != 0) {
                subText.setText(switchItem.subTextRes);
            }
        }

        @Override
        public void onClick(View v) {
            switchView.toggle();
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            updateSubText(isChecked);
            if (onChangeAction != null) {
                if (!onChangeAction.onChange(isChecked, switchItem.getTag())) {
                    setChecked(!isChecked);
                }
                else {
                    switchItem.setChecked(isChecked);
                }
            }
            else {
                setChecked(!isChecked);
            }
        }
    }

    @Override
    public int getType() {
        return ViewTypeManager.ItemType.SWITCH_ITEM;
    }

    @Override
    public String getDetailString() {
        return "MaterialAboutSwitchItem{" +
                "text=" + text +
                ", textRes=" + textRes +
                ", subText=" + subText +
                ", subTextRes=" + subTextRes +
                ", subTextChecked=" + subTextChecked +
                ", subTextCheckedRes=" + subTextCheckedRes +
                ", icon=" + icon +
                ", iconRes=" + iconRes +
                ", showIcon=" + showIcon +
                ", iconGravity=" + iconGravity +
                ", checked=" + checked +
                ", tag=" + tag +
                ", onChangeAction=" + onChangeAction +
                '}';
    }

    public MaterialAboutSwitchItem(MaterialAboutSwitchItem item) {
        this.id = item.getId();
        this.text = item.getText();
        this.textRes = item.getTextRes();
        this.subText = item.getSubText();
        this.subTextRes = item.getSubTextRes();
        this.subTextChecked = item.getSubTextChecked();
        this.subTextCheckedRes = item.getSubTextCheckedRes();
        this.icon = item.getIcon();
        this.iconRes = item.getIconRes();
        this.showIcon = item.showIcon;
        this.iconGravity = item.iconGravity;
        this.checked = item.checked;
        this.tag = item.tag;
        this.onChangeAction = item.onChangeAction;
    }

    @Override
    public MaterialAboutItem clone() {
        return new MaterialAboutSwitchItem(this);
    }

    public int getTag() {
        return tag;
    }
    public MaterialAboutSwitchItem setTag(int tag) {
        this.tag = tag;
        return this;
    }

    public CharSequence getText() {
        return text;
    }
    public MaterialAboutSwitchItem setText(CharSequence text) {
        this.textRes = 0;
        this.text = text;
        return this;
    }

    public int getTextRes() {
        return textRes;
    }

    public MaterialAboutSwitchItem setTextRes(int textRes) {
        this.text = null;
        this.textRes = textRes;
        return this;
    }

    public CharSequence getSubText() {
        return subText;
    }

    public MaterialAboutSwitchItem setSubText(CharSequence subText) {
        this.subTextRes = 0;
        this.subText = subText;
        return this;
    }

    public int getSubTextRes() {
        return subTextRes;
    }

    public MaterialAboutSwitchItem setSubTextRes(int subTextRes) {
        this.subText = null;
        this.subTextRes = subTextRes;
        return this;
    }

    public CharSequence getSubTextChecked() {
        return subTextChecked;
    }

    public MaterialAboutSwitchItem setSubTextChecked(CharSequence subTextChecked) {
        this.subTextCheckedRes = 0;
        this.subTextChecked = subTextChecked;
        return this;
    }

    public int getSubTextCheckedRes() {
        return subTextCheckedRes;
    }

    public MaterialAboutSwitchItem setSubTextCheckedRes(int subTextCheckedRes) {
        this.subTextChecked = null;
        this.subTextCheckedRes = subTextCheckedRes;
        return this;
    }

    public Drawable getIcon() {
        return icon;
    }

    public MaterialAboutSwitchItem setIcon(Drawable icon) {
        this.iconRes = 0;
        this.icon = icon;
        return this;
    }

    public int getIconRes() {
        return iconRes;
    }

    public MaterialAboutSwitchItem setIconRes(int iconRes) {
        this.icon = null;
        this.iconRes = iconRes;
        return this;
    }

    public boolean shouldShowIcon() {
        return showIcon;
    }

    public MaterialAboutSwitchItem setShouldShowIcon(boolean showIcon) {
        this.showIcon = showIcon;
        return this;
    }

    @IconGravity
    public int getIconGravity() {
        return iconGravity;
    }

    public MaterialAboutSwitchItem setIconGravity(int iconGravity) {
        this.iconGravity = iconGravity;
        return this;
    }

    public boolean getChecked() {
        return checked;
    }

    public MaterialAboutSwitchItem setChecked(boolean checked) {
        //Log.d("MaterialItem", "Setting item "+getText()+" to checked "+checked);
        this.checked = checked;
        return this;
    }

    public MaterialAboutItemOnChangeAction getOnChangeAction() {
        return onChangeAction;
    }

    public MaterialAboutSwitchItem setOnChangeAction(MaterialAboutItemOnChangeAction onChangeAction) {
        this.onChangeAction = onChangeAction;
        return this;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({GRAVITY_TOP, GRAVITY_MIDDLE, GRAVITY_BOTTOM})
    public @interface IconGravity {
    }

    public static class Builder {

        MaterialAboutItemOnChangeAction onChangeAction = null;
        private CharSequence text = null;
        @StringRes
        private int textRes = 0;
        private CharSequence subText = null;
        @StringRes
        private int subTextRes = 0;
        private CharSequence subTextChecked = null;
        @StringRes
        private int subTextCheckedRes = 0;
        private Drawable icon = null;
        @DrawableRes
        private int iconRes = 0;
        private boolean showIcon = true;
        @IconGravity
        private int iconGravity = GRAVITY_MIDDLE;
        private boolean checked = false;
        private int tag = -1;

        public Builder tag(int tag) {
            this.tag = tag;
            return this;
        }

        public Builder text(CharSequence text) {
            this.text = text;
            this.textRes = 0;
            return this;
        }

        public Builder text(@StringRes int text) {
            this.textRes = text;
            this.text = null;
            return this;
        }

        public Builder subText(CharSequence subText) {
            this.subText = subText;
            this.subTextRes = 0;
            return this;
        }

        public Builder subText(@StringRes int subTextRes) {
            this.subText = null;
            this.subTextRes = subTextRes;
            return this;
        }

        public Builder subTextHtml(String subTextHtml) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                this.subText = Html.fromHtml(subTextHtml, Html.FROM_HTML_MODE_LEGACY);
            } else {
                //noinspection deprecation
                this.subText = Html.fromHtml(subTextHtml);
            }
            this.subTextRes = 0;
            return this;
        }

        public Builder subTextChecked(CharSequence subTextChecked) {
            this.subTextChecked = subTextChecked;
            this.subTextCheckedRes = 0;
            return this;
        }

        public Builder subTextChecked(@StringRes int subTextCheckedRes) {
            this.subTextChecked = null;
            this.subTextCheckedRes = subTextCheckedRes;
            return this;
        }

        public Builder subTextCheckedHtml(String subTextCheckedHtml) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                this.subTextChecked = Html.fromHtml(subTextCheckedHtml, Html.FROM_HTML_MODE_LEGACY);
            } else {
                //noinspection deprecation
                this.subTextChecked = Html.fromHtml(subTextCheckedHtml);
            }
            this.subTextCheckedRes = 0;
            return this;
        }

        public Builder icon(Drawable icon) {
            this.icon = icon;
            this.iconRes = 0;
            return this;
        }

        public Builder icon(@DrawableRes int iconRes) {
            this.icon = null;
            this.iconRes = iconRes;
            return this;
        }

        public Builder showIcon(boolean showIcon) {
            this.showIcon = showIcon;
            return this;
        }

        public Builder setIconGravity(@IconGravity int iconGravity) {
            this.iconGravity = iconGravity;
            return this;
        }

        public Builder setOnChangeAction(MaterialAboutItemOnChangeAction onChangeAction) {
            this.onChangeAction = onChangeAction;
            return this;
        }

        public Builder checked(boolean isChecked) {
            this.checked = isChecked;
            return this;
        }

        public MaterialAboutSwitchItem build() {
            return new MaterialAboutSwitchItem(this);
        }
    }
}
