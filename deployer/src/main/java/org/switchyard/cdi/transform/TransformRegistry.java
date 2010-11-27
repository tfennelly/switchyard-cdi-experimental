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

    private List<TransformSpec> transforms = new CopyOnWriteArrayList<TransformSpec>();

    public void add(Object transformer) {
        for(Method method : transformer.getClass().getMethods()) {
            add(transformer, method);
        }
    }

    public TransformSpec get(PayloadSpec from, PayloadSpec to) {
        for(TransformSpec transform : transforms) {
            if(transform.from.equals(from) && transform.to.equals(to)) {
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

        // Add a TransformSpec for the transform method...
        if(get(fromSpec, toSpec) != null) {
            throw new IllegalArgumentException("Duplicate transform specification for '" + fromSpec + "' to '" + toSpec + "'.");
        }
        transforms.add(new TransformSpec(fromSpec, toSpec, transformer, transformMethod));
    }

    private <T extends Annotation> T getAnnotation(Class<T> anno, Annotation[] paramAnno) {
        for(Annotation annotation : paramAnno) {
            if(anno.isAssignableFrom(annotation.getClass())) {
                return anno.cast(annotation);
            }
        }

        return null;
    }

    /**
     * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
     */
    public static class TransformSpec {

        private PayloadSpec from;
        private PayloadSpec to;
        private Object transformer;
        private Method transformMethod;

        public TransformSpec(PayloadSpec from, PayloadSpec to, Object transformer, Method transformMethod) {
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
    }
}
