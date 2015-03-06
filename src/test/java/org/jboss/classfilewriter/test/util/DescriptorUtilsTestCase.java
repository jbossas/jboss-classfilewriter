package org.jboss.classfilewriter.test.util;

import org.jboss.classfilewriter.util.DescriptorUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stuart Douglas
 */
public class DescriptorUtilsTestCase {

    @Test
    public void testDescriptorUtils() {
        Assert.assertArrayEquals(new String[]{"Lorg/xnio/OptionsMap;"}, DescriptorUtils.parameterDescriptors("(Lorg/xnio/OptionsMap;)V"));
    }

}
