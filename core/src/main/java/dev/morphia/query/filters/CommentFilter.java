package dev.morphia.query.filters;

/** @hidden */
public class CommentFilter extends Filter {
    private final String comment;

    /**
     * @param comment the comment text
     * @hidden
     */
    public CommentFilter(String comment) {
        super("$comment");
        this.comment = comment;
    }

    /**
     * @return the comment text
     * @hidden
     */
    public String comment() {
        return comment;
    }
}
