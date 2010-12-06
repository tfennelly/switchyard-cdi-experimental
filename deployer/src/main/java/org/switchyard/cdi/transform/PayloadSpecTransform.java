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

package org.switchyard.cdi.transform;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class PayloadSpecTransform implements Transform {

    private PayloadSpec from;
    private PayloadSpec to;
    private Object transformer;
    private Method transformMethod;

    protected PayloadSpecTransform() {
    }

    public PayloadSpecTransform(PayloadSpec from, PayloadSpec to, Object transformer, Method transformMethod) {
        // TODO: Add assertion checks...

        this.from = from;
        this.to = to;
        this.transformer = transformer;
        this.transformMethod = transformMethod;
    }

    public PayloadSpec getFrom() {
        return from;
    }

    public PayloadSpec getTo() {
        return to;
    }

    public Object getTransformer() {
        return transformer;
    }

    public Method getTransformMethod() {
        return transformMethod;
    }

    public Object transform(Object payload) {
        Method transformMethod = getTransformMethod();
        Class<?>[] transformParams = transformMethod.getParameterTypes();

        try {
            if(transformParams.length == 1) {
                return transformMethod.invoke(getTransformer(), payload);
            } else {
                Class<?> toType = transformParams[1];

                if(toType == Writer.class) {
                    StringWriter outputWriter = new StringWriter();
                    try {
                        transformMethod.invoke(getTransformer(), payload, outputWriter);
                        outputWriter.flush();
                        return outputWriter.toString();
                    } finally {
                        try {
                            outputWriter.close();
                        } catch (IOException e) {
                            // unexpected on a StringWriter
                        }
                    }
                } else {
                    // TODO: Support others ??
                }
            }
        } catch (IllegalAccessException e) {
            // TODO: sendFault ...
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO: sendFault ...
            e.printStackTrace();
        }

        return null;
    }
}
