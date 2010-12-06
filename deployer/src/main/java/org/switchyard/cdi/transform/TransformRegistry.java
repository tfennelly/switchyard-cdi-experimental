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

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
@ApplicationScoped
public class TransformRegistry {

    private List<PayloadSpecTransform> transforms = new CopyOnWriteArrayList<PayloadSpecTransform>();

    public void add(Object transformer) {
        for(Method method : transformer.getClass().getMethods()) {
            add(transformer, method);
        }
    }

    public Transform get(PayloadSpec from, PayloadSpec to) {
        for(PayloadSpecTransform transform : transforms) {
            if(transform.getFrom().equals(from) && transform.getTo().equals(to)) {
                return transform;
            }
        }

        return null;
    }

    private void add(Object transformer, Method transformMethod) {
        if(transformMethod.getDeclaringClass() == Object.class) {
            // ignore...
            return;
        }

        Class<?>[] params = transformMethod.getParameterTypes();
        Class<?> returnType = transformMethod.getReturnType();
        Annotation[][] paramAnnos = transformMethod.getParameterAnnotations();

        // Check make sure it's a Transform method...
        if(params.length == 0) {
            // Not a Transform method...
            return;
        }
        From fromAnno = getAnnotation(From.class, paramAnnos[0]);
        if(fromAnno == null) {
            // Not a Transform method...
            return;
        }
        if(params.length == 1 && returnType == Void.class) {
            // TODO: Log/Throw... this is an impl error... specifies a From, but no To...
            return;
        }

        // Create the 'from' and 'to' PayloadSpec instances...
        PayloadSpec fromSpec = PayloadSpec.toPayloadSpec(fromAnno.value(), params[0]);
        PayloadSpec toSpec = null;
        if(params.length == 2) {
            To toAnno = getAnnotation(To.class, paramAnnos[1]);
            if(toAnno == null) {
                // TODO: Log/Throw... this is an impl error... specifies a From, but no To on the second arg...
                return;
            }
            toSpec = PayloadSpec.toPayloadSpec(toAnno.value(), params[1]);
        } else {
            toSpec = PayloadSpec.toPayloadSpec(returnType);
        }

        // Add a Transform for the transform method...
        if(get(fromSpec, toSpec) != null) {
            throw new IllegalArgumentException("Duplicate transform specification for '" + fromSpec + "' to '" + toSpec + "'.");
        }
        transforms.add(new PayloadSpecTransform(fromSpec, toSpec, transformer, transformMethod));
    }

    private <T extends Annotation> T getAnnotation(Class<T> anno, Annotation[] paramAnno) {
        for(Annotation annotation : paramAnno) {
            if(anno.isAssignableFrom(annotation.getClass())) {
                return anno.cast(annotation);
            }
        }

        return null;
    }

    public Object transformObject(Object object, PayloadSpec toSpec) {
        PayloadSpec fromSpec = PayloadSpec.toPayloadSpec(object.getClass());
        return transformObject(object, fromSpec, toSpec);
    }

    public Object transformObject(Object object, PayloadSpec fromSpec, PayloadSpec toSpec) {
        if(!toSpec.equals(fromSpec)) {
            // Not the same... transformation required...
            Transform transform = get(fromSpec, toSpec);

            if(transform ==  null) {
                // TODO: sendFault ... need to define a transformation ...
                return object;
            }

            return transform.execute(object);
        }

        return object;
    }
}
