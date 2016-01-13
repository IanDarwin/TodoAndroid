package todomore.android.metawidget;

import com.darwinsys.todo.model.Priority;

/**
 * MetaWidget needs a converter for each enum type!?
 */
public class PriorityConverter extends EnumConverter<Priority> {

    public PriorityConverter() {

        super(Priority.class);
    }
}

