/**
 * 
 */
package com.google.code.morphia.plugin.jrebel;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.MorphiaExtension;
import com.google.code.morphia.utils.Assert;

public class JRebelMorphiaExtension implements MorphiaExtension
{
    public void applyTo(final Morphia m)
    {
        final JRebelPlugin instance = JRebelPlugin.getInstance();
        Assert.isNotNull(instance, "jRebelPlugin");
        instance.applyTo(m);
    }
}
