package org.jasig.services.persondir.core.config;

import java.util.LinkedHashSet;
import java.util.Set;

import net.sf.ehcache.Ehcache;

import org.jasig.services.persondir.PersonDirectory;
import org.jasig.services.persondir.core.PersonDirectoryImpl;
import org.jasig.services.persondir.spi.CriteriaSearchableAttributeSource;
import org.jasig.services.persondir.spi.SimpleAttributeSource;
import org.jasig.services.persondir.spi.SimpleSearchableAttributeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * Class for working out the details of configuring PD, eventually this will be called by the spring
 * namespace handler based on the XML configuration
 * 
 * @author Eric Dalquist
 */
final class PersonDirectoryConfigBuilder
        extends AbstractConfigBuilder<PersonDirectoryBuilder>
        implements PersonDirectoryConfig, PersonDirectoryBuilder {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    private Set<AbstractAttributeSourceConfigBuilder<?, ?>> sourceBuilders = new LinkedHashSet<AbstractAttributeSourceConfigBuilder<?, ?>>();
    private Set<AttributeSourceConfig<?>> sourceConfigs;
    
    private final String primaryIdAttribute;
    private volatile String mergeCacheName;
    private volatile Ehcache mergeCache;

    PersonDirectoryConfigBuilder(String primaryIdAttribute) {
        super(PersonDirectoryBuilder.class);
        
        Assert.notNull(primaryIdAttribute, "primaryIdAttribute can not be null");
        
        this.primaryIdAttribute = primaryIdAttribute;
    }
    
    @Override
    protected void doResolveConfiguration() {
        final BeanFactory beanFactory = this.getBeanFactory();
        
        //Initialize and copy all attribute sources
        final Builder<AttributeSourceConfig<?>> sourceConfigsBuilder = ImmutableSet.builder();
        for (final AbstractAttributeSourceConfigBuilder<?, ?> configBuilder : this.sourceBuilders) {
            configBuilder.resolveConfiguration(beanFactory);
            sourceConfigsBuilder.add(configBuilder);
        }
        this.sourceBuilders = null;
        this.sourceConfigs = sourceConfigsBuilder.build();
        
        //Resolve merge cache
        if (this.mergeCache == null && this.mergeCacheName != null) {
            this.mergeCache = this.getBeanFactory().getBean(this.mergeCacheName, Ehcache.class);
        }
        else if (this.mergeCacheName == null && this.mergeCache != null) {
            this.mergeCacheName = this.mergeCache.getName();
        }
    }

    @Override
    public final PersonDirectoryBuilder setMergeCacheName(String cacheName) {
        final BeanFactory beanFactory = this.getBeanFactory();
        if (this.mergeCache != null && beanFactory == null) {
            this.logger.warn("Overwriting mergeCache property of '" + this.mergeCache.getName() + "' on source 'TODO' with mergeCacheName of: '" + cacheName);
            this.mergeCache = null;
        }
        this.mergeCacheName = cacheName;
        if (beanFactory != null) {
            this.mergeCache = beanFactory.getBean(this.mergeCacheName, Ehcache.class);
        }
        return this.getThis();
    }
    
    @Override
    public final PersonDirectoryBuilder setMergeCache(Ehcache cache) {
        final BeanFactory beanFactory = this.getBeanFactory();
        if (this.mergeCacheName != null && beanFactory == null) {
            this.logger.warn("Overwriting mergeCacheName property of '" + this.mergeCacheName + "' on source 'TODO' with mergeCache of: '" + cache.getName());
            this.mergeCacheName = null;
        }
        this.mergeCache = cache;
        if (beanFactory != null) {
            this.mergeCacheName = this.mergeCache.getName();
        }
        return this.getThis();
    }
    
    @Override
    public SimpleAttributeSourceBuilder addAttributeSource(SimpleAttributeSource source) {
        final SimpleAttributeSourceConfigBuilder sourceBuilder = new SimpleAttributeSourceConfigBuilder(source);
        sourceBuilders.add(sourceBuilder);
        return sourceBuilder;
    }
    
    @Override
    public SimpleSearchableAttributeSourceBuilder addAttributeSource(SimpleSearchableAttributeSource source) {
        final SimpleSearchableAttributeSourceConfigBuilder sourceBuilder = new SimpleSearchableAttributeSourceConfigBuilder(source);
        sourceBuilders.add(sourceBuilder);
        return sourceBuilder;
    }
    
    @Override
    public CriteriaSearchableAttributeSourceBuilder addAttributeSource(CriteriaSearchableAttributeSource source) {
        final CriteriaSearchableAttributeSourceConfigBuilder sourceBuilder = new CriteriaSearchableAttributeSourceConfigBuilder(source);
        sourceBuilders.add(sourceBuilder);
        return sourceBuilder;
    }
    
    @Override
    public PersonDirectory build(BeanFactory beanFactory) {
        this.resolveConfiguration(beanFactory);
        return new PersonDirectoryImpl(this);
    }
    
    @Override
    public Set<AttributeSourceConfig<?>> getSourceConfigs() {
        if (this.sourceConfigs == null) {
            throw new IllegalStateException("resolveConfiguration(BeanFactory) must be called first.");
        }
        return this.sourceConfigs;
    }

    @Override
    public String getPrimaryIdAttribute() {
        return this.primaryIdAttribute;
    }

    @Override
    public String getMergeCacheName() {
        return this.mergeCacheName;
    }

    @Override
    public Ehcache getMergeCache() {
        return this.mergeCache;
    }
}
