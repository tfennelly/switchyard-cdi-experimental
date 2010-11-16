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

import org.switchyard.ServiceDomain;
import org.switchyard.cdi.Service;
import org.switchyard.internal.ServiceDomains;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Set;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@ApplicationScoped
public class ServiceDeployer implements Extension {

    public void deployServices(@Observes AfterDeploymentValidation afterValid, BeanManager beanManager) {
        Set<Bean<?>> serviceBeans = beanManager.getBeans(Object.class, new ServiceAnnotationLiteral());

        for(Bean<?> serviceBean : serviceBeans) {
            CreationalContext creationalContext = beanManager.createCreationalContext(serviceBean);
            Service serviceAnnotation = serviceBean.getBeanClass().getAnnotation(Service.class);
            String serviceName = serviceAnnotation.value();
            QName serviceQName;

            // TODO: Could use the bean class package name as the namespace component of the Service QName
            if(!serviceName.equals("")) {
                serviceQName = new QName(serviceName);
            } else {
                serviceQName = new QName(serviceBean.getBeanClass().getSimpleName());
            }

            // Register the Service in the ESB domain...
            Object beanRef = beanManager.getReference(serviceBean, Object.class, creationalContext);
            ServiceDomains.getDomain().registerService(serviceQName, new ServiceProxyHandler(beanRef));
        }
    }

    @Service
    private class ServiceAnnotationLiteral extends AnnotationLiteral<Service> implements Service {
        public String value() {
            // TODO: Will this filter unnamed Services only?
            return "";
        }
    }
}
