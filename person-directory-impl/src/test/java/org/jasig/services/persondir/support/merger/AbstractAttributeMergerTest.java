/* Copyright 2004 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/

package org.jasig.services.persondir.support.merger;

import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

/**
 * Abstract test for the IAttributeMerger interface.
 * @author andrew.petro@yale.edu
 * @version $Revision$ $Date$
 */
@SuppressWarnings("deprecation")
public abstract class AbstractAttributeMergerTest extends TestCase {

    /**
     * Test that attempting to merge attributes into a null Map results in
     * an illegal argument exception.
     */
    public void testNullToModify() {
        try {
            getAttributeMerger().mergeAttributes(null, new HashMap<String, List<Object>>());
        } catch (IllegalArgumentException iae) {
            // good
            return;
        }
        fail("Should have thrown IAE on null argument.");
    }
    
    /**
     * Test that attempting to merge attributes into a null Map results in
     * an illegal argument exception.
     */
    public void testNullToConsider() {
        try {
            getAttributeMerger().mergeAttributes(new HashMap<String, List<Object>>(), null);
        } catch (IllegalArgumentException iae) {
            // good
            return;
        }
        fail("Should have thrown IAE on null argument.");
    }
    
    protected abstract IAttributeMerger getAttributeMerger();
    
}