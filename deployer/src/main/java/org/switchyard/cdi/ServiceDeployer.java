/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.switchyard.cdi;

import org.switchyard.cdi.transform.TransformHandler;
import org.switchyard.cdi.transform.TransformRegistry;
import org.switchyard.cdi.transform.TransformSpecifier;
import org.switchyard.cdi.transform.factory.BeanInvocationTransformFactory;
import org.switchyard.internal.DefaultHandlerChain;
import org.switchyard.internal.ServiceDomains;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import javax.xml.namespace.QName;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@ApplicationScoped
public class ServiceDeployer implements Extension {

    private List<ClientProxyBean> createdProxyBeans = new ArrayList<ClientProxyBean>();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        TransformRegistry transformRegistry = getTransformRegistry(beanManager);
        Set<Bean<?>> allBeans = beanManager.getBeans(Object.class, new AnnotationLiteral<Any>() {});

        for(Bean<?> bean : allBeans) {
            Set<InjectionPoint> injectionPoints = bean.getInjectionPoints();

            // Create proxies for the relevant injection points...
            for(InjectionPoint injectionPoint : injectionPoints) {
                for(Annotation qualifier : injectionPoint.getQualifiers()) {
                    if(Service.class.isAssignableFrom(qualifier.annotationType())) {
                        Member member = injectionPoint.getMember();
                        if(member instanceof Field) {
                            Class<?> memberType = ((Field) member).getType();
                            if(memberType.isInterface()) {
                                addInjectableClientProxyBean((Field) member, (Service) qualifier, injectionPoint.getQualifiers(), beanManager, abd);
                            }
                        }
                    }
                }
            }

            // Create Service Proxy ExchangeHandlers and register them as Services, for all @Service beans...
            if(isServiceBean(bean)) {
                Class<?> serviceType = bean.getBeanClass();
                Service serviceAnnotation = serviceType.getAnnotation(Service.class);

                registerESBServiceProxyHandler(bean, serviceType, serviceAnnotation, beanManager, transformRegistry);
                if(serviceType.isInterface()) {
                    addInjectableClientProxyBean(bean, serviceType, serviceAnnotation, beanManager, abd);
                }
            }

            // Add the transform methods to the TransformRegistry...
            if(isTransformerBean(bean)) {
                CreationalContext creationalContext = beanManager.createCreationalContext(bean);
                Object transformBeanInst =  beanManager.getReference(bean, Object.class, creationalContext);

                transformRegistry.add(transformBeanInst);
            }
        }
    }

    private void registerESBServiceProxyHandler(Bean<?> serviceBean, Class<?> serviceType, Service serviceAnnotation, BeanManager beanManager, TransformRegistry transformRegistry) {
        QName serviceQName = toServiceQName(serviceAnnotation, serviceType.getSimpleName());
        CreationalContext creationalContext = beanManager.createCreationalContext(serviceBean);

        // Register the Service in the ESB domain...
        Object beanRef = beanManager.getReference(serviceBean, Object.class, creationalContext);

        // TODO: Should the TransformHandler be one of the system handlers?
        DefaultHandlerChain handlerChain = new DefaultHandlerChain();
        BeanServiceMetadata serviceMetadata = new BeanServiceMetadata(serviceType);

        handlerChain.addLast("transform", new TransformHandler(new BeanInvocationTransformFactory(serviceMetadata, transformRegistry)));
        handlerChain.addLast("serviceProxy", new ServiceProxyHandler(beanRef, serviceMetadata));
        
        ServiceDomains.getDomain().registerService(serviceQName, handlerChain);
    }

    private void addInjectableClientProxyBean(Bean<?> serviceBean, Class<?> serviceType, Service serviceAnnotation, BeanManager beanManager, AfterBeanDiscovery abd) {
        QName serviceQName = toServiceQName(serviceAnnotation, serviceBean.getBeanClass().getSimpleName());

        addClientProxyBean(serviceQName, serviceType, null, abd);
    }

    private void addInjectableClientProxyBean(Field injectionPointField, Service serviceAnnotation, Set<Annotation> qualifiers, BeanManager beanManager, AfterBeanDiscovery abd) {
        QName serviceQName = toServiceQName(serviceAnnotation, injectionPointField.getType().getSimpleName());

        addClientProxyBean(serviceQName, injectionPointField.getType(), qualifiers, abd);
    }

    private void addClientProxyBean(QName serviceQName, Class<?> beanClass, Set<Annotation> qualifiers, AfterBeanDiscovery abd) {
        // Check do we already have a proxy for this service interface...
        for(ClientProxyBean clientProxyBean : createdProxyBeans) {
            if(serviceQName.equals(clientProxyBean.getServiceQName()) && beanClass == clientProxyBean.getBeanClass()) {
                // ignore... we already have a proxy ...
                return;
            }
        }

        ClientProxyBean clientProxyBean = new ClientProxyBean(serviceQName, beanClass, qualifiers);
        createdProxyBeans.add(clientProxyBean);
        abd.addBean(clientProxyBean);
    }

    private boolean isServiceBean(Bean<?> bean) {
        return bean.getBeanClass().isAnnotationPresent(Service.class);
    }

    private boolean isTransformerBean(Bean<?> bean) {
        return bean.getBeanClass().isAnnotationPresent(TransformSpecifier.class);
    }

    private QName toServiceQName(Service serviceAnnotation, String defaultName) {
        String serviceName = serviceAnnotation.value();

        // TODO: Could use the bean class package name as the namespace component of the Service QName
        if(!serviceName.equals("")) {
            return new QName(serviceName);
        } else {
            return new QName(defaultName);
        }
    }

    private TransformRegistry getTransformRegistry(BeanManager beanManager) {
        Set<Bean<?>> transformRegistryBeans = beanManager.getBeans(TransformRegistry.class);

        if(!transformRegistryBeans.isEmpty()) {
            Bean regBean = transformRegistryBeans.iterator().next();
            CreationalContext creationalContext = beanManager.createCreationalContext(regBean);
            return (TransformRegistry) beanManager.getReference(regBean, TransformRegistry.class, creationalContext);
        }

        throw new IllegalStateException("Unexpected Exception.  Failed to get a reference to the TransformRegistry bean.");        
    }
}
