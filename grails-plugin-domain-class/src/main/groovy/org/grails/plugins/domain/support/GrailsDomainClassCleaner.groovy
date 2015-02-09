package org.grails.plugins.domain.support

import groovy.util.logging.Commons
import org.grails.datastore.gorm.GormEntity

import java.lang.reflect.Modifier

import grails.core.GrailsApplication
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent


/**
 * Clears static Grails "instance api" instances from domain classes when 
 * ApplicationContext's ContextClosedEvent is received. 
 * 
 * 
 * @author Lari Hotari
 *
 */
@Commons
class GrailsDomainClassCleaner implements ApplicationListener<ContextClosedEvent>, ApplicationContextAware  {
    protected GrailsApplication grailsApplication
    protected ApplicationContext applicationContext
    
    public GrailsDomainClassCleaner(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    public void onApplicationEvent(ContextClosedEvent event) {
        if(event.applicationContext == this.applicationContext || this.applicationContext == null) {
            clearAllStaticApiInstances()
        }
    }

    protected clearAllStaticApiInstances() {
        for (dc in grailsApplication.domainClasses) {
            def clz = dc.clazz
            if(GormEntity.isAssignableFrom(clz)) {
                try {
                    clz.initInternalApi null
                } catch (e) {
                    log.warn("Error clearing instance api property in ${clz.name}", e)
                }
                try {
                    clz.initInternalStaticApi null
                } catch (e) {
                    log.warn("Error clearing static api property in ${clz.name}", e)
                }
            } else {
                clearStaticApiInstances(clz)
            }
        }
    }

    protected clearStaticApiInstances(Class clazz) {
        clazz.metaClass.getProperties().each { MetaProperty metaProperty ->
            if(Modifier.isStatic(metaProperty.getModifiers()) && metaProperty.name ==~ /^(instance|static).+Api$/) {
                log.info("Clearing static property ${metaProperty.name} in ${clazz.name}")
                try {
                    metaProperty.setProperty(clazz, null)
                } catch (e) {
                    log.warn("Error clearing static property ${metaProperty.name} in ${clazz.name}", e)
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext
    }
}