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

import org.switchyard.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ServiceProxyHandler implements ExchangeHandler {
    
    private Object serviceBean;

    public ServiceProxyHandler(Object serviceBean) {
        this.serviceBean = serviceBean;
    }

    public void handle(Exchange exchange) throws HandlerException {

        // TODO: Could obviously build more invocation options here... simple single param for now...

        Object param = exchange.getMessage().getContent();

        if(param != null) {
            Method method = getMethodForParamType(param.getClass());

            if(method != null) {
                try {
                    if(exchange.getPattern() == ExchangePattern.IN_OUT) {
                        Object outMessagePayload = method.invoke(serviceBean, param);
                        Message message = MessageBuilder.newInstance().buildMessage();

                        message.setContent(outMessagePayload);
                        exchange.sendOut(message);
                    } else {
                        method.invoke(serviceBean, param);
                    }
                } catch (IllegalAccessException e) {
                    // Fixme
                    e.printStackTrace();
                    // sendFault...
                } catch (InvocationTargetException e) {
                    // Fixme
                    e.printStackTrace();
                    // sendFault...
                }
            } else {
                // sendFault...
            }
        } else {
            // sendFault...
        }
    }

    private Method getMethodForParamType(Class<? extends Object> paramType) {
        Method[] methods = serviceBean.getClass().getMethods();

        for(Method method : methods) {
            Class<?>[] methodParams = method.getParameterTypes();

            if(methodParams.length == 1) {
                if(methodParams[0].isAssignableFrom(paramType)) {
                    return method;
                }
            }
        }

        return null;
    }
}
