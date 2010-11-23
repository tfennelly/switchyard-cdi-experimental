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

import org.switchyard.internal.ServiceDomains;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import javax.xml.namespace.QName;
import java.util.Set;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@ApplicationScoped
public class ServiceDeployer implements Extension {

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        Set<Bean<?>> serviceBeans = beanManager.getBeans(Object.class, new ServiceAnnotationLiteral());

        for(Bean<?> serviceBean : serviceBeans) {
            Class<?> serviceType = getServiceType(serviceBean.getBeanClass());
            ESBService serviceAnnotation = serviceType.getAnnotation(ESBService.class);

            registerESBServiceProxyHandler(serviceBean, serviceAnnotation, beanManager);
            if(serviceType.isInterface()) {
                addInjectableClientProxyBean(serviceBean, serviceType, serviceAnnotation, beanManager, abd);
            }
        }
    }

    private void registerESBServiceProxyHandler(Bean<?> serviceBean, ESBService serviceAnnotation, BeanManager beanManager) {
        QName serviceQName = toServiceQName(serviceBean, serviceAnnotation);
        CreationalContext creationalContext = beanManager.createCreationalContext(serviceBean);

        // Register the Service in the ESB domain...
        Object beanRef = beanManager.getReference(serviceBean, Object.class, creationalContext);
        ServiceDomains.getDomain().registerService(serviceQName, new ServiceProxyHandler(beanRef));
    }

    private void addInjectableClientProxyBean(Bean<?> serviceBean, Class<?> serviceType, ESBService serviceAnnotation, BeanManager beanManager, AfterBeanDiscovery abd) {
        QName serviceQName = toServiceQName(serviceBean, serviceAnnotation);

        abd.addBean(new ClientProxyBean(serviceQName, serviceType));
    }

    private Class<?> getServiceType(Class<?> annotatedClass) {
        if(!annotatedClass.isAnnotationPresent(ESBService.class)) {
            Class<?>[] implementedInterfaces = annotatedClass.getInterfaces();
            Class<?> implementorType;

            for(Class<?> implementedInterface : implementedInterfaces) {
                implementorType = getServiceType(implementedInterface);
                if(implementorType != null) {
                    return implementorType;
                }
            }

            Class<?> superClass = annotatedClass.getSuperclass();
            if(superClass != null) {
                implementorType = getServiceType(superClass);
                if(implementorType != null) {
                    return implementorType;
                }
            }

            return null;
        }

        return annotatedClass;
    }

    private QName toServiceQName(Bean<?> serviceBean, ESBService serviceAnnotation) {
        String serviceName = serviceAnnotation.value();

        // TODO: Could use the bean class package name as the namespace component of the ESBService QName
        if(!serviceName.equals("")) {
            return new QName(serviceName);
        } else {
            return new QName(serviceBean.getBeanClass().getSimpleName());
        }
    }

    private class ServiceAnnotationLiteral extends AnnotationLiteral<ESBService> implements ESBService {
        public String value() {
            // TODO: Will this filter unnamed Services only?
            return "";
        }
    }
}
