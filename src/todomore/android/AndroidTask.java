package todomore.android;

import org.metawidget.inspector.annotation.UiHidden;

import com.darwinsys.todo.model.Task;

/** 
 * A TodoMore "Todo Task" with an added _id, because with a SyncAdapter
 * we will need to keep these Task things in both databases.
 * @author Ian Darwin
 */
public class AndroidTask extends Task {

	private static final long serialVersionUID = 1247383306369798480L;

	private long _id;

	@UiHidden
	public long get_Id() {
		return _id;
	}
}
