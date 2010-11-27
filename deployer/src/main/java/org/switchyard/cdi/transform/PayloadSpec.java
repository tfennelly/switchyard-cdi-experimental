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

import org.switchyard.Exchange;
import org.switchyard.Scope;

import java.io.Serializable;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class PayloadSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String IN_PAYLOAD_SPEC_KEY  = PayloadSpec.class.getName() + "#IN";
    public static final String OUT_PAYLOAD_SPEC_KEY = PayloadSpec.class.getName() + "#OUT";

    private String value;

    public PayloadSpec(String value) {
        // TODO: Add assertion checks...

        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj == this) {
            return true;
        }
        if(obj instanceof PayloadSpec) {
            PayloadSpec payloadSpec = (PayloadSpec) obj;
            return (payloadSpec.value.equals(value));
        }

        return false;
    }

    @Override
    public String toString() {
        return value;
    }

    public static void setInPayloadSpec(Exchange exchange, String payloadSpec) {
        exchange.getContext(Scope.EXCHANGE).setProperty(IN_PAYLOAD_SPEC_KEY, new PayloadSpec(payloadSpec));
    }

    public static PayloadSpec getInPayloadSpec(Exchange exchange) {
        return (PayloadSpec) exchange.getContext(Scope.EXCHANGE).getProperty(IN_PAYLOAD_SPEC_KEY);
    }

    public static void setOutPayloadSpec(Exchange exchange, String payloadSpec) {
        exchange.getContext(Scope.EXCHANGE).setProperty(OUT_PAYLOAD_SPEC_KEY, new PayloadSpec(payloadSpec));
    }

    public static PayloadSpec getOutPayloadSpec(Exchange exchange) {
        return (PayloadSpec) exchange.getContext(Scope.EXCHANGE).getProperty(OUT_PAYLOAD_SPEC_KEY);
    }

    public static PayloadSpec toPayloadSpec(Class<?> type) {
        return new PayloadSpec(type.getName());
    }
    
    public static PayloadSpec toPayloadSpec(String payloadSpec, Class<?> type) {
        if(payloadSpec.equals("")) {
            // Default mime type to Java...
            payloadSpec = type.getName();
        }

        return new PayloadSpec(payloadSpec);
    }
}
