/**
 * 
 */
package com.google.code.morphia.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.morphia.TestBase;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class ReflectionUtilsTest extends TestBase
{

    /**
     * Test method for
     * {@link com.google.code.morphia.utils.ReflectionUtils#implementsInterface(java.lang.Class, java.lang.Class)}
     * .
     */
    @Test
	public void testImplementsInterface()
    {
        Assert.assertTrue(ReflectionUtils.implementsInterface(ArrayList.class, List.class));
        Assert.assertTrue(ReflectionUtils.implementsInterface(ArrayList.class, Collection.class));
        Assert.assertFalse(ReflectionUtils.implementsInterface(Set.class, List.class));
    }

}
