package dev.morphia.aggregation.stages;

import java.time.LocalDateTime;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.FullDocumentBeforeChange;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;

import org.bson.Document;

import static java.lang.String.format;

/**
 * Returns a Change Stream cursor on a collection, a database, or an entire cluster. Must be used as the first stage in an aggregation
 * pipeline.
 *
 * @since 2.3
 */
public class ChangeStream extends Stage {
    private Boolean allChangesForCluster;
    private FullDocument fullDocument = FullDocument.DEFAULT;
    private FullDocumentBeforeChange fullDocumentBeforeChange = FullDocumentBeforeChange.DEFAULT;
    private Document resumeAfter;
    private Document startAfter;
    private LocalDateTime startAtOperationTime;

    /**
     * Defines a Change Stream cursor on a collection, a database, or an entire cluster. Must be used as the first stage in an aggregation
     * pipeline.
     *
     * @since 2.3
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    protected ChangeStream() {
        super("$changeStream");
    }

    /**
     * Returns a Change Stream cursor on a collection, a database, or an entire cluster. Must be used as the first stage in an aggregation
     * pipeline.
     *
     * @return the new ChangeStream stage
     * @aggregation.stage $changeStream
     * @mongodb.server.release 5.0
     * @since 2.3
     */
    public static ChangeStream changeStream() {
        return new ChangeStream();
    }

    /**
     * @return internal
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public Boolean allChangesForCluster() {
        return allChangesForCluster;
    }

    /**
     * Sets whether the change stream should include all changes in the cluster. May only be opened on the admin database.
     *
     * @param allChangesForCluster true to get all changes
     * @return this
     */
    public ChangeStream allChangesForCluster(Boolean allChangesForCluster) {
        this.allChangesForCluster = allChangesForCluster;
        return this;
    }

    /**
     * @return internal
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public FullDocument fullDocument() {
        return fullDocument;
    }

    /**
     * Specifies whether change notifications include a copy of the full document when modified by update operations.
     *
     * @param fullDocument the option to apply
     * @return this
     */
    public ChangeStream fullDocument(FullDocument fullDocument) {
        this.fullDocument = fullDocument;
        return this;
    }

    /**
     * @return internal
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public FullDocumentBeforeChange fullDocumentBeforeChange() {
        return fullDocumentBeforeChange;
    }

    /**
     * Include the full document from before the change.
     *
     * @param fullDocumentBeforeChange the option to apply
     * @return this
     */
    public ChangeStream fullDocumentBeforeChange(FullDocumentBeforeChange fullDocumentBeforeChange) {
        this.fullDocumentBeforeChange = fullDocumentBeforeChange;
        return this;
    }

    /**
     * @return internal
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public Document resumeAfter() {
        return resumeAfter != null ? new Document(resumeAfter) : null;
    }

    /**
     * Specifies a resume token as the logical starting point for the change stream. Cannot be used with startAfter or
     * startAtOperationTime fields.
     *
     * @param resumeAfter the token to resume after
     * @return this
     */
    public ChangeStream resumeAfter(Document resumeAfter) {
        this.resumeAfter = new Document(resumeAfter);
        return this;
    }

    /**
     * @return internal
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public Document startAfter() {
        return startAfter != null ? new Document(startAfter) : null;
    }

    /**
     * Specifies a resume token as the logical starting point for the change stream. Cannot be used with resumeAfter or
     * startAtOperationTime fields.
     *
     * @param startAfter the token to start after
     * @return this
     */
    public ChangeStream startAfter(Document startAfter) {
        this.startAfter = new Document(startAfter);
        return this;
    }

    /**
     * @return internal
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Nullable
    public LocalDateTime startAtOperationTime() {
        return startAtOperationTime;
    }

    /**
     * Specifies a time as the logical starting point for the change stream. Cannot be used with resumeAfter or startAfter fields.
     *
     * @param startAtOperationTime the time to start after
     * @return this
     */
    public ChangeStream startAtOperationTime(LocalDateTime startAtOperationTime) {
        this.startAtOperationTime = startAtOperationTime;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String toString() {
        return format("ChangeStream{allChangesForCluster=%s, fullDocument=%s, fullDocumentBeforeChange=%s, resumeAfter=%s, "
                + "startAfter=%s, startAtOperationTime=%s}", allChangesForCluster, fullDocument, fullDocumentBeforeChange,
                resumeAfter, startAfter, startAtOperationTime);
    }
}
