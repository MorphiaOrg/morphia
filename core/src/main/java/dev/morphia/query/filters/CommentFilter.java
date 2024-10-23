package dev.morphia.query.filters;

public class CommentFilter extends Filter {
    private final String comment;

    public CommentFilter(String comment) {
        super("$comment");
        this.comment = comment;
    }

    public String comment() {
        return comment;
    }
}
