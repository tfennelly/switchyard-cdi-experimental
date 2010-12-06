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

package org.switchyard.cdi.transform.specfactory;

import org.switchyard.Exchange;
import org.switchyard.cdi.BeanServiceMetadata;
import org.switchyard.cdi.transform.PayloadSpec;
import org.switchyard.cdi.transform.TransformRegistry;
import org.switchyard.cdi.transform.TransformSpec;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class BeanInvocationTransformSpecFactory implements TransformSpecFactory {

    private TransformRegistry transformRegistry;
    private BeanServiceMetadata beanServiceMetadata;

    // TODO:  There's an issue here and we're not addressing it yet....
    // I think the transformation requirements need to be handled on a per handler chain basis.
    //
    // I think:
    //
    // 1.  The send/consumer side needs to transform (to the provider type) before sending because it
    //     knows about its own format and the format supported by the provider e.g. Java to "XMLA".
    //
    // 2.  The receive/provider side needs to transform from it's supported interface type,
    //     to its internal/canonical/service representation of this data e.g. "XMLA" to Java.
    //
    // And the above is just talking about the IN aspects of an exchange.  You'd have a similar
    // scenario for the OUT leg of an IN_OUT exchange.
    //

    public BeanInvocationTransformSpecFactory(BeanServiceMetadata beanServiceMetadata, TransformRegistry transformRegistry) {
        this.beanServiceMetadata = beanServiceMetadata;
        this.transformRegistry = transformRegistry;
    }

    public TransformSpec getTransformSpec(Exchange exchange) {
        String operationName = BeanServiceMetadata.getOperationName(exchange);

        if(operationName != null) {
            List<Method> candidateMethods = beanServiceMetadata.getCandidateMethods(operationName);

            // Operation name must resolve to exactly one bean method...
            if(candidateMethods.size() != 1) {
                // TODO: sendFault ??? ...
                return null;
            }

            Method operationMethod = candidateMethods.get(0);
            PayloadSpec fromSpec = PayloadSpec.getInPayloadSpec(exchange); // The data format sent by consumer

            if(fromSpec == null) {
                return null;
            }

            Class<?>[] operationArgs = operationMethod.getParameterTypes();
            if(operationArgs.length == 1) {
                PayloadSpec toSpec = PayloadSpec.toPayloadSpec(operationArgs[0]);

                return transformRegistry.get(fromSpec, toSpec);
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
}
