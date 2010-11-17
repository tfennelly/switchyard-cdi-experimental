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

package org.switchyard.cdi.omservice;

import org.junit.Assert;
import org.junit.Test;
import org.switchyard.*;
import org.switchyard.cdi.AbstractCDITest;
import org.switchyard.cdi.omservice.service.OrderRequest;
import org.switchyard.cdi.omservice.service.OrderResponse;
import org.switchyard.internal.ServiceDomains;

import javax.xml.namespace.QName;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class OMInOutTest extends AbstractCDITest {

    @Test
    public void test() {
        ServiceDomain domain = ServiceDomains.getDomain();

        // Consume the OM service...
        MockHandler responseConsumer = new MockHandler();
        Exchange exchange = domain.createExchange(new QName("OrderManagementService"), ExchangePattern.IN_OUT, responseConsumer);

        Message inMessage = MessageBuilder.newInstance().buildMessage();
        inMessage.setContent(new OrderRequest("D123"));

        exchange.send(inMessage);

        // wait, since this is async
        responseConsumer.waitForMessage();

        OrderResponse response = (OrderResponse) responseConsumer._messages.poll().getMessage().getContent();
        Assert.assertEquals("D123", response.orderId);
    }
}
