package dev.morphia.rewrite.refaster;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Provides metadata for a Refaster template class, including a human-readable name and description.
 */
@Target(ElementType.TYPE)
public @interface TemplateDescriptor {
    /**
     * Returns the human-readable name of the template.
     *
     * @return the template name
     */
    String name();

    /**
     * Returns a description of what the template does.
     *
     * @return the template description
     */
    String description();
}
