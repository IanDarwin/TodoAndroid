package todomore.android.metawidget;

import com.darwinsys.todo.model.Status;

/**
 * MetaWidget needs a converter for each enum type!?
 */
public class StatusConverter extends EnumConverter<Status> {

    public StatusConverter() {

        super(Status.class);
    }
}

