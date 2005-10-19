/* Copyright 2005 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/

package org.jasig.portal.services.persondir.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Eric Dalquist <a href="mailto:edalquist@unicon.net">edalquist@unicon.net</a>
 * @version $Revision$
 */
public class CachingPersonAttributeDaoTest extends AbstractDefaultQueryPersonAttributeDaoTest {
    private static final String defaultAttr = "uid"; 

    private ComplexStubPersonAttributeDao stubDao;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        this.stubDao = new ComplexStubPersonAttributeDao();
        
        super.setUp();
        
        Map user1 = new HashMap();
        user1.put("phone", "777-7777");
        user1.put("displayName", "Display Name");
        
        Map user2 = new HashMap();
        user2.put("phone", "888-8888");
        user2.put("displayName", "");
        
        Map user3 = new HashMap();
        user3.put("phone", "666-6666");
        user3.put("givenName", "Howard");
        
        Map daoBackingMap = new HashMap();
        
        daoBackingMap.put("edalquist", user1);
        daoBackingMap.put("awp9", user2);
        daoBackingMap.put("erider", user3);
        
        this.stubDao.setBackingMap(daoBackingMap);
        this.stubDao.setDefaultAttributeName(defaultAttr);
        
        super.setUp();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        this.stubDao = null;
        
        super.tearDown();
    }
    
    
    public void testCacheStats() {
        CachingPersonAttributeDaoImpl dao = new CachingPersonAttributeDaoImpl();
        dao.setCachedPersonAttributesDao(this.stubDao);
        dao.setDefaultAttributeName(defaultAttr);
        dao.setUserInfoCache(new HashMap());
        
        assertEquals("Query count incorrect", 0, dao.getQueries());
        assertEquals("Miss count incorrect", 0, dao.getMisses());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Query count incorrect", 1, dao.getQueries());
        assertEquals("Miss count incorrect", 1, dao.getMisses());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Query count incorrect", 2, dao.getQueries());
        assertEquals("Miss count incorrect", 1, dao.getMisses());
        
        dao.getUserAttributes("nobody");
        assertEquals("Query count incorrect", 3, dao.getQueries());
        assertEquals("Miss count incorrect", 2, dao.getMisses());
        
        dao.getUserAttributes("awp9");
        assertEquals("Query count incorrect", 4, dao.getQueries());
        assertEquals("Miss count incorrect", 3, dao.getMisses());
        
        dao.getUserAttributes("nobody");
        assertEquals("Query count incorrect", 5, dao.getQueries());
        assertEquals("Miss count incorrect", 4, dao.getMisses());
        
        dao.getUserAttributes("awp9");
        assertEquals("Query count incorrect", 6, dao.getQueries());
        assertEquals("Miss count incorrect", 4, dao.getMisses());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Query count incorrect", 7, dao.getQueries());
        assertEquals("Miss count incorrect", 4, dao.getMisses());
    }
    
    public void testCaching() {
        Map cacheMap = new HashMap();
        
        CachingPersonAttributeDaoImpl dao = new CachingPersonAttributeDaoImpl();
        dao.setCachedPersonAttributesDao(this.stubDao);
        dao.setDefaultAttributeName(defaultAttr);
        dao.setUserInfoCache(cacheMap);
        
        assertEquals("Incorrect number of items in cache", 0, cacheMap.size());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Incorrect number of items in cache", 1, cacheMap.size());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Incorrect number of items in cache", 1, cacheMap.size());
        
        dao.getUserAttributes("nobody");
        assertEquals("Incorrect number of items in cache", 2, cacheMap.size());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Incorrect number of items in cache", 2, cacheMap.size());
        
        
        Map queryMap = new HashMap();
        queryMap.put(defaultAttr, "edalquist");
        queryMap.put("name.first", "Eric");
        queryMap.put("name.last", "Dalquist");
        
        dao.getUserAttributes(queryMap);
        assertEquals("Incorrect number of items in cache", 2, cacheMap.size());
    }
    

    
    public void testMulipleAttributeKeys() {
        Map cacheMap = new HashMap();
        
        Set keyAttrs = new HashSet();
        keyAttrs.add("name.first");
        keyAttrs.add("name.last");
        
        CachingPersonAttributeDaoImpl dao = new CachingPersonAttributeDaoImpl();
        dao.setCachedPersonAttributesDao(this.stubDao);
        dao.setCacheKeyAttributes(keyAttrs);
        dao.setUserInfoCache(cacheMap);
        
        assertEquals("Incorrect number of items in cache", 0, cacheMap.size());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Incorrect number of items in cache", 1, cacheMap.size());
        
        dao.getUserAttributes("nobody");
        assertEquals("Incorrect number of items in cache", 1, cacheMap.size());
        
        dao.getUserAttributes("edalquist");
        assertEquals("Incorrect number of items in cache", 1, cacheMap.size());
        
        Map queryMap1 = new HashMap();
        queryMap1.put(defaultAttr, "edalquist");
        queryMap1.put("name.first", "Eric");
        queryMap1.put("name.last", "Dalquist");
        
        dao.getUserAttributes(queryMap1);
        assertEquals("Incorrect number of items in cache", 2, cacheMap.size());
        
        
        Map queryMap2 = new HashMap();
        queryMap2.put("name.first", "John");
        queryMap2.put("name.last", "Doe");
        
        dao.getUserAttributes(queryMap2);
        assertEquals("Incorrect number of items in cache", 3, cacheMap.size());
        
        
        dao.getUserAttributes(queryMap1);
        assertEquals("Incorrect number of items in cache", 3, cacheMap.size());
    }


    protected AbstractDefaultQueryPersonAttributeDao getAbstractDefaultQueryPersonAttributeDao() {
        CachingPersonAttributeDaoImpl dao = new CachingPersonAttributeDaoImpl();
        dao.setCachedPersonAttributesDao(this.stubDao);
        dao.setDefaultAttributeName(defaultAttr);
        dao.setUserInfoCache(new HashMap());
        
        return dao;
    }
}
