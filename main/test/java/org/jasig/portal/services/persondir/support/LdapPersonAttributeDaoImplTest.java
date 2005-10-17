/* Copyright 2004 The JA-SIG Collaborative.  All rights reserved.
*  See license distributed with this file and
*  available online at http://www.uportal.org/license.html
*/

package org.jasig.portal.services.persondir.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.jasig.portal.services.persondir.IPersonAttributeDao;

/**
 * Testcase for LdapPersonAttributeDao.
 * @author andrew.petro@yale.edu
 * @author Eric Dalquist <a href="mailto:edalquist@unicon.net">edalquist@unicon.net</a>
 * @version $Revision$ $Date$
 */
public class LdapPersonAttributeDaoImplTest 
    extends AbstractPersonAttributeDaoTest {
    
    LdapContext ldapContext;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://mrfrumble.its.yale.edu:389/o=yale.edu");
        this.ldapContext = (LdapContext)new InitialLdapContext(env, null);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        this.ldapContext = null;
    }

    /**
     * Test for a query with a single attribute. 
     * 
     * This testcase will cease to work on that fateful day when Andrew
     * no longer appears in Yale University LDAP.
     */
    public void testSingleAttrQuery() {
        final String queryAttr = "uid";
        final List queryAttrList = new LinkedList();
        queryAttrList.add(queryAttr);
        
        LdapPersonAttributeDaoImpl impl = new LdapPersonAttributeDaoImpl();
        
        Map ldapAttribsToPortalAttribs = new HashMap();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setLdapAttributesToPortalAttributes(ldapAttribsToPortalAttribs);
        
        impl.setLdapContext(ldapContext);
        
        impl.setQuery("(uid={0})");
        
        impl.setQueryAttributes(queryAttrList);
        
        Map queryMap = new HashMap();
        queryMap.put(queryAttr, "awp9");
        
        Map attribs = impl.getUserAttributes(queryMap);
        assertEquals("andrew.petro@yale.edu", attribs.get("email"));
    }
    
    /**
     * Test case for a query that needs multiple attributes to complete and
     * more attributes than are needed to complete are passed to it.
     */
    public void testMultiAttrQuery() {
        final String queryAttr1 = "uid";
        final String queryAttr2 = "alias";
        final List queryAttrList = new LinkedList();
        queryAttrList.add(queryAttr1);
        queryAttrList.add(queryAttr2);
        
        LdapPersonAttributeDaoImpl impl = new LdapPersonAttributeDaoImpl();
        
        Map ldapAttribsToPortalAttribs = new HashMap();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setLdapAttributesToPortalAttributes(ldapAttribsToPortalAttribs);
        
        impl.setLdapContext(ldapContext);
        
        impl.setQuery("(&(uid={0})(alias={1}))");
        
        impl.setQueryAttributes(queryAttrList);
        
        Map queryMap = new HashMap();
        queryMap.put(queryAttr1, "awp9");
        queryMap.put(queryAttr2, "andrew.petro");
        queryMap.put("email", "edalquist@unicon.net");
        
        Map attribs = impl.getUserAttributes(queryMap);
        assertEquals("andrew.petro@yale.edu", attribs.get("email"));
    }
    
    /**
     * A query that needs mulitple attributes to complete but the needed
     * attributes aren't passed to it.
     */
    public void testInsufficientAttrQuery() {
        final String queryAttr1 = "uid";
        final String queryAttr2 = "alias";
        final List queryAttrList = new LinkedList();
        queryAttrList.add(queryAttr1);
        queryAttrList.add(queryAttr2);
        
        LdapPersonAttributeDaoImpl impl = new LdapPersonAttributeDaoImpl();
        
        Map ldapAttribsToPortalAttribs = new HashMap();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setLdapAttributesToPortalAttributes(ldapAttribsToPortalAttribs);
        
        impl.setLdapContext(ldapContext);
        
        impl.setQuery("(&(uid={0})(alias={1}))");
        
        impl.setQueryAttributes(queryAttrList);
        
        Map queryMap = new HashMap();
        queryMap.put(queryAttr1, "awp9");
        queryMap.put("email", "edalquist@unicon.net");
        
        Map attribs = impl.getUserAttributes(queryMap);
        assertNull(attribs);
    }
    
    /**
     * Test proper reporting of declared attribute names.
     */
    public void testAttributeNames() {
        LdapPersonAttributeDaoImpl impl = new LdapPersonAttributeDaoImpl();
        
        Map ldapAttribsToPortalAttribs = new HashMap();
        ldapAttribsToPortalAttribs.put("mail", "email");
        ldapAttribsToPortalAttribs.put("shirtColor", "dressShirtColor");
        
        Set surNameAttributeNames = new HashSet();
        surNameAttributeNames.add("surName");
        surNameAttributeNames.add("lastName");
        surNameAttributeNames.add("familyName");
        surNameAttributeNames.add("thirdName");
        ldapAttribsToPortalAttribs.put("lastName", surNameAttributeNames);
        
        impl.setLdapAttributesToPortalAttributes(ldapAttribsToPortalAttribs);
        
        Set expectedAttributeNames = new HashSet();
        expectedAttributeNames.addAll(surNameAttributeNames);
        expectedAttributeNames.add("email");
        expectedAttributeNames.add("dressShirtColor");
        
        assertEquals(expectedAttributeNames, impl.getPossibleUserAttributeNames());
    }

    protected IPersonAttributeDao getPersonAttributeDaoInstance() {
        final String queryAttr = "uid";
        final List queryAttrList = new LinkedList();
        queryAttrList.add(queryAttr);
        
        LdapPersonAttributeDaoImpl impl = new LdapPersonAttributeDaoImpl();
        
        Map ldapAttribsToPortalAttribs = new HashMap();
        ldapAttribsToPortalAttribs.put("mail", "email");
        
        impl.setLdapAttributesToPortalAttributes(ldapAttribsToPortalAttribs);
        
        impl.setLdapContext(ldapContext);
        
        impl.setQuery("(uid={0})");
        
        impl.setQueryAttributes(queryAttrList);
        
        return impl;
    }
}