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
import org.switchyard.ExchangeHandler;
import org.switchyard.HandlerException;
import org.switchyard.Message;
import org.switchyard.cdi.transform.factory.TransformFactory;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class TransformHandler implements ExchangeHandler {

    private TransformFactory transformSpecFactory;

    public TransformHandler(TransformFactory transformSpecFactory) {
        this.transformSpecFactory = transformSpecFactory;
    }

    public void handleMessage(Exchange exchange) throws HandlerException {
        if(exchange != null) {
            Transform transform = transformSpecFactory.getTransform(exchange);
            if(transform != null) {
                Message message = exchange.getMessage();
                Object newPayload = transform.transform(message.getContent());
                message.setContent(newPayload);
            }
        }
    }

    public void handleFault(Exchange exchange) {
        // TODO: Anything ??
    }
}
