/**
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-10/license-header.txt
 */

package org.jasig.services.persondir.support.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jasig.services.persondir.support.IAdditionalDescriptors;
import org.jasig.services.persondir.support.MultivaluedPersonAttributeUtils;
import org.springframework.web.filter.GenericFilterBean;

/**
 * {@link javax.servlet.Filter} that can provide {@link HttpServletRequest} headers and other properties on the request
 * as person attributes. The filter sets attributes on a {@link IAdditionalDescriptors} which it is configured with. To
 * work correctly the {@link IAdditionalDescriptors} object needs to be a session scoped Spring bean so that each user
 * gets only their own attributes correctly.
 * <br>
 * <br>
 * Configuration:
 * <table border="1">
 *     <tr>
 *         <th align="left">Property</th>
 *         <th align="left">Description</th>
 *         <th align="left">Required</th>
 *         <th align="left">Default</th>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">usernameAttribute</td>
 *         <td>
 *             The name of the attribute from the request (header or property) to use as the username. This is required
 *             so that Person Directory can later associate these attributes with the user correctly during queries.
 *         </td>
 *         <td valign="top">Yes</td>
 *         <td valign="top">null</td>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">headerAttributeMapping</td>
 *         <td>
 *             Set the {@link Map} to use for mapping from a header name to an attribute name or {@link Set} of attribute
 *             names. Header names that are specified but have null mappings will use the column name for the attribute
 *             name. Header names that are not specified as keys in this {@link Map} will be ignored.
 *             <br>
 *             The passed {@link Map} must have keys of type {@link String} and values of type {@link String} or a {@link Set}
 *             of {@link String}.
 *         </td>
 *         <td valign="top">Yes</td>
 *         <td valign="top">null</td>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">additionalDescriptors</td>
 *         <td>
 *             The {@link IAdditionalDescriptors} object to set attributes found on the request into. The provided object
 *             should be a Spring session scoped bean which will allow each user to have their own version attached to
 *             their session.
 *         </td>
 *         <td valign="top">Yes</td>
 *         <td valign="top">null</td>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">remoteUserAttribute</td>
 *         <td>
 *             If specified {@link HttpServletRequest#getRemoteUser()} is called and the returned value is stored as
 *             an attribute using the value specified for this property.
 *         </td>
 *         <td valign="top">No</td>
 *         <td valign="top">null</td>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">remoteAddrAttribute</td>
 *         <td>
 *             If specified {@link HttpServletRequest#getRemoteAddr()} is called and the returned value is stored as
 *             an attribute using the value specified for this property.
 *         </td>
 *         <td valign="top">No</td>
 *         <td valign="top">null</td>
 *     </tr>
 *     <tr>
 *         <td align="right" valign="top">clearExistingAttributes</td>
 *         <td>
 *             If true when attributes are found on the request any existing attributes in the provided {@link IAdditionalDescriptors}
 *             object will cleared and replaced with the new attributes. If false the new attributes overwrite existing
 *             attributes of the same name but attributes in {@link IAdditionalDescriptors} not found on the current request
 *             are not touched.
 *         </td>
 *         <td valign="top">No</td>
 *         <td valign="top">false</td>
 *     </tr>
 * </table>
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class RequestAttributeSourceFilter extends GenericFilterBean {
    private String usernameAttribute;
    private Map<String, Set<String>> headerAttributeMapping;
    private IAdditionalDescriptors additionalDescriptors;
    private String remoteUserAttribute;
    private String remoteAddrAttribute;
    private String remoteHostAttribute;
    private boolean clearExistingAttributes = false;
    
    public String getUsernameAttribute() {
        return usernameAttribute;
    }
    /**
     * The name of the attribute from the request to use as the username
     */
    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    public String getRemoteUserAttribute() {
        return remoteUserAttribute;
    }
    /**
     * If specified {@link HttpServletRequest#getRemoteUser()} is added as an attribute under the provided name
     */
    public void setRemoteUserAttribute(String remoteUserAttribute) {
        this.remoteUserAttribute = remoteUserAttribute;
    }

    public String getRemoteAddrAttribute() {
        return remoteAddrAttribute;
    }
    /**
     * If specified {@link HttpServletRequest#getRemoteAddr()} is added as an attribute under the provided name
     */
    public void setRemoteAddrAttribute(String remoteAddrAttribute) {
        this.remoteAddrAttribute = remoteAddrAttribute;
    }

    public String getRemoteHostAttribute() {
        return remoteHostAttribute;
    }
    /**
     * If specified {@link HttpServletRequest#getRemoteHost()} is added as an attribute under the provided name
     */
    public void setRemoteHostAttribute(String remoteHostAttribute) {
        this.remoteHostAttribute = remoteHostAttribute;
    }

    public IAdditionalDescriptors getAdditionalDescriptors() {
        return additionalDescriptors;
    }
    /**
     * The {@link AdditionalDescriptors} instance to set request attributes on. This should be a Spring session-scoped
     * proxy to allow each session to have its own set of request-populated attributes.
     */
    public void setAdditionalDescriptors(IAdditionalDescriptors additionalDescriptors) {
        this.additionalDescriptors = additionalDescriptors;
    }

    public boolean isClearExistingAttributes() {
        return clearExistingAttributes;
    }
    /**
     * @param clearExistingAttributes If existing all attributes should be cleared when any new attributes are found.
     * Defaults to false. 
     */
    public void setClearExistingAttributes(boolean clearExistingAttributes) {
        this.clearExistingAttributes = clearExistingAttributes;
    }

    public Map<String, Set<String>> getHeaderAttributeMapping() {
        return headerAttributeMapping;
    }
    /**
     * Set the {@link Map} to use for mapping from a header name to an attribute name or {@link Set} of attribute
     * names. Header names that are specified but have null mappings will use the column name for the attribute
     * name. Header names that are not specified as keys in this {@link Map} will be ignored.
     * <br>
     * The passed {@link Map} must have keys of type {@link String} and values of type {@link String} or a {@link Set} 
     * of {@link String}.
     * 
     * @param headerAttributeMapping {@link Map} from column names to attribute names, may not be null.
     * @throws IllegalArgumentException If the {@link Map} doesn't follow the rules stated above.
     * @see MultivaluedPersonAttributeUtils#parseAttributeToAttributeMapping(Map)
     */
    public void setHeaderAttributeMapping(final Map<String, ?> headerAttributeMapping) {
        final Map<String, Set<String>> parsedHeaderAttributeMapping = MultivaluedPersonAttributeUtils.parseAttributeToAttributeMapping(headerAttributeMapping);
        
        if (parsedHeaderAttributeMapping.containsKey("")) {
            throw new IllegalArgumentException("The map from attribute names to attributes must not have any empty keys.");
        }
        
        this.headerAttributeMapping = parsedHeaderAttributeMapping;
    }
    

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
            
            final Map<String, List<Object>> attributes = new LinkedHashMap<String, List<Object>>();
            
            this.addRequestProperties(httpServletRequest, attributes);
    
            this.addRequestHeaders(httpServletRequest, attributes);
            
            final List<Object> usernameAttributes = attributes.get(this.usernameAttribute);
            if (usernameAttributes == null || usernameAttributes.isEmpty() || usernameAttributes.get(0) == null) {
                this.logger.warn("No username found for attribute '" + this.usernameAttribute + "' among " + attributes);
            }
            else {
                final String username = usernameAttributes.get(0).toString();
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Adding attributes for user " + username + ". " + attributes);
                }
                
                this.additionalDescriptors.setName(username);
                
                if (this.clearExistingAttributes) {
                    this.additionalDescriptors.setAttributes(attributes);
                }
                else {
                    this.additionalDescriptors.addAttributes(attributes);
                }
            }
        }
        
        
        chain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Add other properties from the request to the attributes map
     */
    protected void addRequestProperties(final HttpServletRequest httpServletRequest, final Map<String, List<Object>> attributes) {
        if (this.remoteUserAttribute != null) {
            final String remoteUser = httpServletRequest.getRemoteUser();
            attributes.put(this.remoteUserAttribute, list(remoteUser));
        }
        if (this.remoteAddrAttribute != null) {
            final String remoteAddr = httpServletRequest.getRemoteAddr();
            attributes.put(this.remoteAddrAttribute, list(remoteAddr));
        }
        if (this.remoteHostAttribute != null) {
            final String remoteHost = httpServletRequest.getRemoteHost();
            attributes.put(this.remoteHostAttribute, list(remoteHost));
        }
    }

    /**
     * Add request headers to the attributes map
     */
    protected void addRequestHeaders(final HttpServletRequest httpServletRequest, final Map<String, List<Object>> attributes) {
        for (final Map.Entry<String, Set<String>> headerAttributeEntry : this.headerAttributeMapping.entrySet()) {
            final String headerName = headerAttributeEntry.getKey();
            final String value = httpServletRequest.getHeader(headerName);
            
            if (value != null) {
                for (final String attribueName : headerAttributeEntry.getValue()) {
                    attributes.put(attribueName, list(value));
                }
            }
        }
    }

    private List<Object> list(final Object value) {
        return Arrays.asList(value);
    }
}
