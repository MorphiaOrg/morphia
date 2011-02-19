/**
 * 
 */
package com.google.code.morphia.plugin.jrebel;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.logging.Logr;
import com.google.code.morphia.logging.MorphiaLoggerFactory;

/**
 * @author us@thomas-daily.de
 */
public class JRebelExtension
{
    public JRebelExtension(final Morphia m)
    {
        final Logr log = MorphiaLoggerFactory.get(JRebelExtension.class);

        if (JRebelPlugin.alreadyInstanciated)
        {
            new JRebelPlugin().applyTo(m);
            log.info(this.getClass().getSimpleName() + " initialized");
        }
        else
        {
            log.error("JRebelPlugin was not initialized by JRebel. Class-remapping will not happen - the Extension is ignored.");
        }
    }
}
