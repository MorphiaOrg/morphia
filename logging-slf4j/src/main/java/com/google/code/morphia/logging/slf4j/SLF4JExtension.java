/**
 * 
 */
package com.google.code.morphia.logging.slf4j;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.MorphiaExtension;
import com.google.code.morphia.logging.MorphiaLoggerFactory;

/**
 * @author us@thomas-daily.de
 */
public class SLF4JExtension implements MorphiaExtension
{
    public void applyTo(final Morphia m)
    {
        MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);
    }

}
