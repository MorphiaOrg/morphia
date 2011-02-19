/**
 * 
 */
package com.google.code.morphia.plugin.jrebel;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.MorphiaExtension;
import com.google.code.morphia.logging.MorphiaLoggerFactory;

/**
 * @author us@thomas-daily.de
 */
public class JRebelExtension implements MorphiaExtension
{
    public void applyTo(final Morphia m)
    {
        final JRebelPlugin instance = JRebelPlugin.getInstance();
        if (instance != null)
        {
            instance.applyTo(m);
        }
        else
        {
            MorphiaLoggerFactory
                    .get(JRebelExtension.class)
                    .error("JRebelPlugin was not initialized by JRebel. Class-remapping will not happen - the Extension is ignored.");
        }
    }
}
