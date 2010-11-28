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
import org.switchyard.cdi.transform.PayloadSpec;
import org.switchyard.cdi.transform.TransformRegistry;

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
    private TransformRegistry transformRegistry;
    private List<Method> publicServiceMethods = new ArrayList<Method>();

    public ServiceProxyHandler(Object serviceBean, TransformRegistry transformRegistry) {
        this.serviceBean = serviceBean;
        this.transformRegistry = transformRegistry;
        serviceClass = serviceBean.getClass();

        Method[] serviceMethods = serviceClass.getMethods();
        for(Method serviceMethod : serviceMethods) {
            if(serviceMethod.getDeclaringClass() != Object.class) {
                publicServiceMethods.add(serviceMethod);
            }
        }
    }

    public void handleMessage(Exchange exchange) throws HandlerException {
        handle(exchange);
    }

    public void handleFault(Exchange exchange) {
        handle(exchange);
    }

    public static void setOperationName(Exchange exchange, String name) {
        exchange.getContext(Scope.EXCHANGE).setProperty(OPERATION_NAME, name);
    }

    private String getOperationName(Exchange exchange) {
        return (String) exchange.getContext(Scope.EXCHANGE).getProperty(OPERATION_NAME);
    }

    private void handle(Exchange exchange) {
        Invocation invocation = getInvocation(exchange);

        if(invocation != null) {
            try {
                if(exchange.getPattern() == ExchangePattern.IN_OUT) {
                    Object responseObject = invocation.method.invoke(serviceBean, invocation.args);
                    Message message = MessageBuilder.newInstance().buildMessage();

                    PayloadSpec outPayloadSpec = PayloadSpec.getOutPayloadSpec(exchange);
                    if(outPayloadSpec != null) {
                        responseObject = transformRegistry.transformObject(responseObject, outPayloadSpec);
                    }

                    message.setContent(responseObject);
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
            System.out.println("Unable to resolve invocation parameters.");
            // TODO: sendFault...
        }
    }

    private Invocation getInvocation(Exchange exchange) {

        String operationName = getOperationName(exchange);

        if(operationName != null) {
            List<Method> candidateMethods = getCandidateMethods(operationName);

            // Operation name must resolve to exactly one bean method...
            if(candidateMethods.size() != 1) {
                // TODO: sendFault ??? ...
                return null;
            }

            Method operationMethod = candidateMethods.get(0);
            PayloadSpec exchangePayloadSpec = PayloadSpec.getInPayloadSpec(exchange);

            if(exchangePayloadSpec == null) {
                return new Invocation(operationMethod, exchange.getMessage().getContent());
            }

            Class<?>[] operationArgs = operationMethod.getParameterTypes();
            if(operationArgs.length == 1) {
                PayloadSpec toPayloadSpec = PayloadSpec.toPayloadSpec(operationArgs[0]);

                Object transformedPayload = transformRegistry.transformObject(exchange.getMessage().getContent(), exchangePayloadSpec, toPayloadSpec);

                return new Invocation(operationMethod, transformedPayload);
            } else {
                System.out.println("Unsupported... multi-args not yet suppoted....");
                // TODO: sendFault ... don't support multi-args yet ...
            }
        } else {
            System.out.println("Operation name not specified on exchange.");
            // TODO: Operation name not specified... sendFault  ...
        }

        return null;
    }

    private List<Method> getCandidateMethods(String name) {
        List<Method> candidateMethods = new ArrayList<Method>();

        for(Method serviceMethod : publicServiceMethods) {
            if(serviceMethod.getName().equals(name)) {
                candidateMethods.add(serviceMethod);
            }
        }

        return candidateMethods;
    }

    private static class Invocation {
        private Method method;
        private Object[] args;

        private Invocation(Method method, Object arg) {
            this.method = method;
            this.args = castArg(arg);
        }

        private static Object[] castArg(Object arg) {
            if(arg.getClass().isArray()) {
                return (Object[].class).cast(arg);
            } else {
                return new Object[] {arg};
            }
        }
    }

}
