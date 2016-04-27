package todomore.android;

import org.metawidget.inspector.annotation.UiHidden;

import com.darwinsys.todo.model.Task;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 
 * A TodoMore "Todo Task" with an added _id, because with a SyncAdapter
 * we will need to keep these Task things in both databases.
 * @author Ian Darwin
 */
public class AndroidTask extends Task {

	private static final long serialVersionUID = 1247383306369798480L;

	/** "id" is the field in the remote database, "_id" is the field in the local db */
	private long _id;
	
	public AndroidTask() {
		// empty
	}
	
	public AndroidTask(String name, String project, String context) {
		super(name, project, context);
	}

	@UiHidden @JsonIgnore // Remote doesn't grok our id, what if 2 devices?
	public long get_Id() {
		return _id;
	}

	public void set_Id(long _id) {
		this._id = _id;
	}
	
	/** Convenience */
	@UiHidden  @JsonIgnore
	public long getRemoteId() {
		return getId();
	}
	public void setRemoteId(long id) {
		setId(id);
	}
}
