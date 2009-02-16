/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-9/license-header.txt
 */
package org.jasig.services.persondir.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jasig.services.persondir.AbstractPersonAttributeDaoTest;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.util.Util;


/**
 * Testcase for StubPersonAttributeDao.
 * @version $Revision$ $Date$
 */
public class StubPersonAttributeDaoTest 
    extends AbstractPersonAttributeDaoTest {

    private StubPersonAttributeDao testInstance;
    private Map<String, List<Object>> backingMap;
    
    
    @Override
    protected void setUp() throws Exception {
        Map<String, List<Object>> map = new HashMap<String, List<Object>>();
        map.put("shirtColor", Util.list("blue"));
        map.put("phone", Util.list("777-7777"));
        
        this.backingMap = map;
        
        this.testInstance = new StubPersonAttributeDao();
        this.testInstance.setBackingMap(map);
        
        super.setUp();
    }
    
    /**
     * Test that when the backing map is set properly reports possible 
     * attribute names and when the map is not set returns null for
     * possible attribute names.
     */
    public void testGetPossibleUserAttributeNames() {
        Set<String> expectedAttributeNames = new HashSet<String>();
        expectedAttributeNames.add("shirtColor");
        expectedAttributeNames.add("phone");
        Set<String> possibleAttributeNames = this.testInstance.getPossibleUserAttributeNames();
        assertEquals(expectedAttributeNames, possibleAttributeNames);
        
        StubPersonAttributeDao nullBacking = new StubPersonAttributeDao();
        assertEquals(Collections.EMPTY_SET, nullBacking.getPossibleUserAttributeNames());
    }

    public void testGetUserAttributesMap() {
        assertEquals(this.backingMap, this.testInstance.getMultivaluedUserAttributes(new HashMap<String, List<Object>>()));

    }

    public void testGetUserAttributesString() {
        assertEquals(this.backingMap, this.testInstance.getMultivaluedUserAttributes("wombat"));
    }

    @Override
    protected IPersonAttributeDao getPersonAttributeDaoInstance() {
        return this.testInstance;
    }

}

