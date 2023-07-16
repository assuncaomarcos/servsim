package me.marcosassuncao.servsim.event;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Abstract implementation of an event.
 *
 * @param <T> the event type
 * @param <S> the event subject
 *
 * @author Marcos Dias de Assuncao
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractEvent<T extends Enum, S> implements Event<T, S> {
    /**
     * The time the event takes place.
     */
    private final long time;

    /**
     * The type of event.
     */
    private final T type;

    /**
     * The subject or event body.
     */
    private final S subject;

    /**
     * Creates an event of a given type.
     *
     * @param type    the event type
     * @param subject event subject
     * @param time    time of occurrence of the event
     */
    protected AbstractEvent(final T type, final S subject, final long time) {
        this.type = type;
        this.subject = subject;
        this.time = time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long time() {
        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T type() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public S subject() {
        return subject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", time)
                .add("type", type)
                .add("subject", subject)
                .toString();
    }
}
