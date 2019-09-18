package pl.szczodrzynski.edziennik.messages;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.datamodels.Teacher;

public class MessagesComposeSuggestionAdapter extends ArrayAdapter<Teacher> {

    private Context context;
    private List<Teacher> teacherList;
    private ArrayList<Teacher> originalList = null;
    private ArrayFilter mFilter;
    private final Object mLock = new Object();

    MessagesComposeSuggestionAdapter(@NonNull Context context, List<Teacher> teacherList) {
        super(context, 0, teacherList);
        this.context = context;
        this.teacherList = teacherList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(context).inflate(R.layout.messages_compose_suggestion_item, parent, false);

        Teacher teacher = teacherList.get(position);

        TextView name = listItem.findViewById(R.id.name);
        TextView type = listItem.findViewById(R.id.type);
        ImageView image = listItem.findViewById(R.id.image);

        teacher.image = MessagesUtils.getProfileImage(48, 24, 16, 12, 1, teacher.getFullName());

        if (teacher.id <= 0) {
            name.setText(Teacher.typeString(context, (int) (teacher.id * -1)));
            type.setText(R.string.teachers_browse_category);
            image.setImageBitmap(null);
        }
        else {
            if (teacher.displayName == null)
                name.setText(teacher.getFullName());
            else
                name.setText(Html.fromHtml(teacher.displayName));
            type.setText(teacher.getType(context));
            image.setImageBitmap(teacher.image);
        }

        return listItem;
    }

    @Override
    public int getCount() {
        return teacherList.size();
    }

    @Nullable
    @Override
    public Teacher getItem(int position) {
        return teacherList.get(position);
    }

    @Override
    public int getPosition(@Nullable Teacher item) {
        return teacherList.indexOf(item);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    private class TeacherWeighted extends Teacher {
        public int weight;

        public TeacherWeighted(Teacher teacher, int weight) {
            super(teacher.profileId, teacher.id, teacher.name, teacher.surname, teacher.loginId);
            this.weight = weight;
            this.image = teacher.image;
            this.type = teacher.type;
            this.typeDescription = teacher.typeDescription;
        }

        @NonNull
        @Override
        public String toString() {
            return getFullName();
        }
    }
    private Comparator<? super Teacher> comparator = (o1, o2) -> ((TeacherWeighted) o1).weight - ((TeacherWeighted) o2).weight;

    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final FilterResults results = new FilterResults();

            if (originalList == null) {
                synchronized (mLock) {
                    originalList = new ArrayList<>(teacherList);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                ArrayList<Teacher> list;
                synchronized (mLock) {
                    list = new ArrayList<>(originalList);
                }
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();

                ArrayList<Teacher> values;
                synchronized (mLock) {
                    values = new ArrayList<>(originalList);
                }

                int count = values.size();
                ArrayList<Teacher> newValues = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    Teacher teacher = values.get(i);
                    String teacherFullName = teacher.getFullName().toLowerCase();
                    teacher.displayName = teacherFullName.replace(prefixString, "<b>"+prefixString+"</b>");

                    // First match against the whole, non-splitted value
                    boolean found = false;
                    if (teacherFullName.startsWith(prefixString)) {
                        newValues.add(new TeacherWeighted(teacher, 1));
                        found = true;
                    } else {
                        // check if prefix matches any of the words
                        String[] words = teacherFullName.split(" ");
                        for (String word : words) {
                            if (word.startsWith(prefixString)) {
                                newValues.add(new TeacherWeighted(teacher, 2));
                                found = true;
                                break;
                            }
                        }
                    }
                    // finally check if the prefix matches any part of the name
                    if (!found && teacherFullName.contains(prefixString)) {
                        newValues.add(new TeacherWeighted(teacher, 3));
                    }

                }

                Collections.sort(newValues, comparator);

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            teacherList = (List<Teacher>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}