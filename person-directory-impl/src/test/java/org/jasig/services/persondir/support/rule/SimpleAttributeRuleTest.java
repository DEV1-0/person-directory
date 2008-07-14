package org.jasig.services.persondir.support.rule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.jasig.services.persondir.util.Util;

public class SimpleAttributeRuleTest extends TestCase {

	private static final String WHEN_KEY = "eduPersonPrimaryAffiliation";
	private static final String WHEN_PATTERN = "records-staff";
	private static final String SET_UID = "username";
	private static final String SET_KEY = "fax";
	private static final String SET_VALUE = "(480) 555-1212";

	/*
	 * Public API.
	 */
	
	public SimpleAttributeRuleTest() {}

	public void testConstructorParameters() {
        
		// whenKey.
		try {
			new SimpleAttributeRule(null, WHEN_PATTERN, SET_UID, SET_KEY, SET_VALUE);
			fail("IllegalArgumentException should have been thrown with null 'whenKey'.");
		} catch (IllegalArgumentException iae) {
			// expected...
		}

		// whenPattern.
		try {
			new SimpleAttributeRule(WHEN_KEY, null, SET_UID, SET_KEY, SET_VALUE);
			fail("IllegalArgumentException should have been thrown with null 'whenPattern'.");
		} catch (IllegalArgumentException iae) {
			// expected...
		}

		// setKey.
		try {
			new SimpleAttributeRule(WHEN_KEY, WHEN_PATTERN, SET_UID, null, SET_VALUE);
			fail("IllegalArgumentException should have been thrown with null 'setKey'.");
		} catch (IllegalArgumentException iae) {
			// expected...
		}

		// setValue.
		try {
			new SimpleAttributeRule(WHEN_KEY, WHEN_PATTERN, SET_UID, SET_KEY, null);
			fail("IllegalArgumentException should have been thrown with null 'setValue'.");
		} catch (IllegalArgumentException iae) {
			// expected...
		}


	}

	public void testAppliesToParameters() {
        
		AttributeRule r = new SimpleAttributeRule(WHEN_KEY, WHEN_PATTERN, SET_UID, SET_KEY, SET_VALUE);
		
		// null.
		try {
			r.appliesTo(null);
			fail("IllegalArgumentException should have been thrown with null 'userInfo'.");
		} catch (IllegalArgumentException iae) {
			// expected...
		}

		Map<String, List<Object>> m = new HashMap<String, List<Object>>();
		
		// String --> true.
		m.put(WHEN_KEY, Util.list(WHEN_PATTERN));
		assertTrue(r.appliesTo(m));

		// String --> false.
		m.put(WHEN_KEY, Util.list("monkey"));
		assertFalse(r.appliesTo(m));

	}

}