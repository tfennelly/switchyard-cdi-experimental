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
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class ServiceProxyHandler implements ExchangeHandler {

    private static final String OPERATION_NAME = "OPERATION_NAME";

    private Object serviceBean;
    private Class<? extends Object> serviceClass;
    private Method[] serviceMethods;

    public ServiceProxyHandler(Object serviceBean) {
        this.serviceBean = serviceBean;
        serviceClass = serviceBean.getClass();
        serviceMethods = serviceClass.getMethods();
    }

    public void handleMessage(Exchange exchange) throws HandlerException {
        handle(exchange);
    }

    public void handleFault(Exchange exchange) {
        handle(exchange);
    }

    public static void setOperationName(Exchange exchange, String name) {
        exchange.getContext(Scope.MESSAGE).setProperty(OPERATION_NAME, name);
    }

    private String getOperationName(Exchange exchange) {
        return (String) exchange.getContext(Scope.MESSAGE).getProperty(OPERATION_NAME);
    }

    private void handle(Exchange exchange) {
        Invocation invocation = getInvocation(exchange);

        if(invocation != null) {
            try {
                if(exchange.getPattern() == ExchangePattern.IN_OUT) {
                    Object outMessagePayload = invocation.method.invoke(serviceBean, invocation.args);
                    Message message = MessageBuilder.newInstance().buildMessage();

                    message.setContent(outMessagePayload);
                    exchange.send(message);
                } else {
                    invocation.method.invoke(serviceBean, invocation.args);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                // TODO: sendFault...
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                // TODO: sendFault...
            }
        } else {
            // TODO: sendFault...
        }
    }

    private Invocation getInvocation(Exchange exchange) {

        Object[] args = getArgs(exchange);

        if(args != null) {
            String operationName = getOperationName(exchange);
            List<Method> candidateMethods = getCandidateMethods(args);

            // TODO: CDI may have a funky way of resolving the target method, that I missed in the spec...
            if(operationName != null) {
                for(Method candidateMethod : candidateMethods) {
                    if(candidateMethod.getName().equals(operationName)) {
                        return new Invocation(candidateMethod, args);
                    }
                }
            } else if(!candidateMethods.isEmpty()) {
                // TODO: What if there are multiple impls?
                return new Invocation(candidateMethods.get(0), args);
            }
        }

        return null;
    }

    private Object[] getArgs(Exchange exchange) {
        Object paramPayload = exchange.getMessage().getContent();

        if(paramPayload != null) {
            if(paramPayload.getClass().isArray()) {
                return (Object[]) paramPayload;
            } else {
                return new Object[] {paramPayload};
            }
        }

        return null;
    }

    private List<Method> getCandidateMethods(Object[] args) {
        List<Method> candidateMethods = new ArrayList<Method>();

        for(Method serviceMethod : serviceMethods) {
            if(serviceMethod.getDeclaringClass() == Object.class) {
                continue;
            }

            Class<?>[] serviceMethodArgTypes = serviceMethod.getParameterTypes();

            if(serviceMethodArgTypes.length == args.length) {
                for(int i = 0; i < args.length; i++) {
                    Object arg = args[i];

                    if(arg == null) {
                        if(serviceMethodArgTypes[i].isPrimitive()) {
                            // Null is matchable s long as it's not a primitive...
                            continue;
                        }
                    } else if(arg.getClass() != serviceMethodArgTypes[i]) {
                        // must be an exact type match
                        continue;
                    }

                    candidateMethods.add(serviceMethod);
                }
            }
        }
        
        return candidateMethods;
    }

    private class Invocation {
        private Method method;
        private Object[] args;

        private Invocation(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

}
