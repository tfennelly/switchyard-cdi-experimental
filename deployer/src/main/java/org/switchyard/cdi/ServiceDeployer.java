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
import javax.inject.Qualifier;
import javax.xml.namespace.QName;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@ApplicationScoped
public class ServiceDeployer implements Extension {

    public void processAnnotatedType(@Observes ProcessAnnotatedType pat, BeanManager bm) {
        AnnotatedType type = pat.getAnnotatedType();
        Class<?> annotatedClass = type.getJavaClass();

        if(getServiceType(annotatedClass) != null) {
            pat.setAnnotatedType(new ServiceImplAnnotatedType(type));
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        Set<Bean<?>> serviceBeans = beanManager.getBeans(Object.class, new ServiceImplementationInst());

        for(Bean<?> serviceBean : serviceBeans) {
            Class<?> serviceType = getServiceType(serviceBean.getBeanClass());
            Service serviceAnnotation = serviceType.getAnnotation(Service.class);

            registerESBServiceProxyHandler(serviceBean, serviceAnnotation, beanManager);
            if(serviceType.isInterface()) {
                addInjectableClientProxyBean(serviceBean, serviceType, serviceAnnotation, beanManager, abd);
            }
        }
    }

    private void registerESBServiceProxyHandler(Bean<?> serviceBean, Service serviceAnnotation, BeanManager beanManager) {
        QName serviceQName = toServiceQName(serviceBean, serviceAnnotation);
        CreationalContext creationalContext = beanManager.createCreationalContext(serviceBean);

        // Register the Service in the ESB domain...
        Object beanRef = beanManager.getReference(serviceBean, Object.class, creationalContext);
        ServiceDomains.getDomain().registerService(serviceQName, new ServiceProxyHandler(beanRef));
    }

    private void addInjectableClientProxyBean(Bean<?> serviceBean, Class<?> serviceType, Service serviceAnnotation, BeanManager beanManager, AfterBeanDiscovery abd) {
        QName serviceQName = toServiceQName(serviceBean, serviceAnnotation);

        abd.addBean(new ClientProxyBean(serviceQName, serviceType));
    }

    private Class<?> getServiceType(Class<?> annotatedClass) {
        if(!annotatedClass.isAnnotationPresent(Service.class)) {
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

    private QName toServiceQName(Bean<?> serviceBean, Service serviceAnnotation) {
        String serviceName = serviceAnnotation.value();

        // TODO: Could use the bean class package name as the namespace component of the Service QName
        if(!serviceName.equals("")) {
            return new QName(serviceName);
        } else {
            return new QName(serviceBean.getBeanClass().getSimpleName());
        }
    }

    private class ServiceAnnotationLiteral extends AnnotationLiteral<Service> implements Service {
        public String value() {
            // TODO: Will this filter unnamed Services only?
            return "";
        }
    }

    private class ServiceImplAnnotatedType implements AnnotatedType {

        private AnnotatedType serviceType;
        private Service serviceAnnotation;
        private ServiceImplementationInst serviceImplAnnotation;

        private ServiceImplAnnotatedType(AnnotatedType serviceType) {
            this.serviceType = serviceType;
            serviceAnnotation = serviceType.getAnnotation(Service.class);
            serviceImplAnnotation = new ServiceImplementationInst(serviceAnnotation);
        }

        public Class getJavaClass() {
            return serviceType.getJavaClass();
        }

        public Set getConstructors() {
            return serviceType.getConstructors();
        }

        public Set getMethods() {
            return serviceType.getMethods();
        }

        public Set getFields() {
            return serviceType.getFields();
        }

        public Type getBaseType() {
            return serviceType.getBaseType();
        }

        public Set<Type> getTypeClosure() {
            return serviceType.getTypeClosure();
        }

        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            if(Service.class.isAssignableFrom(annotationType)) {
                // Hide the Service annotation...
                return null;
            } else if(ServiceImplementation.class.isAssignableFrom(annotationType)) {
                return annotationType.cast(serviceImplAnnotation);
            }

            return serviceType.getAnnotation(annotationType);
        }

        public Set<Annotation> getAnnotations() {
            Set<Annotation> annotationSet = new LinkedHashSet<Annotation>(serviceType.getAnnotations());

            // Remove the Service annotation and add a ServiceImplementation annotation...
            annotationSet.remove(serviceAnnotation);
            annotationSet.add(serviceImplAnnotation);

            return annotationSet;
        }

        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return serviceType.isAnnotationPresent(annotationType);
        }
    }

    @Qualifier
    @Target({ TYPE })
    @Retention(RUNTIME)
    @Documented
    private @interface ServiceImplementation {
        String value() default "";
    }

    private class ServiceImplementationInst extends AnnotationLiteral<ServiceImplementation> implements ServiceImplementation {

        private Service serviceAnnotation;

        private ServiceImplementationInst() {
        }

        private ServiceImplementationInst(Service serviceAnnotation) {
            this.serviceAnnotation = serviceAnnotation;
        }

        public String value() {
            if(serviceAnnotation == null) {
                // TODO: Will this filter unnamed Services only?
                return "";
            }

            return serviceAnnotation.value();
        }

        public Service getServiceAnnotation() {
            return serviceAnnotation;
        }
    }
}
