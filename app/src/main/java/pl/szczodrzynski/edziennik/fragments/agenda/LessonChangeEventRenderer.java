package pl.szczodrzynski.edziennik.fragments.agenda;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.github.tibolte.agendacalendarview.render.EventRenderer;

import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.datamodels.Lesson;

public class LessonChangeEventRenderer extends EventRenderer<LessonChangeEvent> {
    @Override
    public void render(View view, LessonChangeEvent event) {
        CardView card = view.findViewById(R.id.lesson_change_card);
        TextView changeText = view.findViewById(R.id.lesson_change_text);
        TextView changeCount = view.findViewById(R.id.lesson_change_count);
        card.setCardBackgroundColor(event.getColor());
        changeText.setTextColor(event.getTextColor());
        changeCount.setTextColor(event.getTextColor());
        changeCount.setText(String.valueOf(event.getLessonChangeCount()));
    }

    @Override
    public int getEventLayout() {
        return R.layout.agenda_event_lesson_change;
    }
}
