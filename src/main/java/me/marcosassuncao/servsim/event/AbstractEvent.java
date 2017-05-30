package me.marcosassuncao.servsim.event;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Abstract implementation of an event
 * 
 * @author Marcos Dias de Assuncao
 *
 * @param <T> the event type
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractEvent<T extends Enum, S> implements Event<T, S> {
	private long time;
    private T type;
    private S subject;
    
    /**
     * Creates an event of a given type.
     * @param type the event type
     * @param subject event subject
     * @param time time of occurrence of the event
     */
    protected AbstractEvent(T type, S subject, long time) {
        this.type = type;
        this.subject = subject;
        this.time = time;
    }

	@Override
	public long time() {
		return time;
	}

	@Override
	public T type() {
		return type;
	}
	
	@Override
	public S subject() {
		return subject;
	}
	
    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", time)
                .add("type", type)
                .add("subject", subject)
                .toString();
    }
}
