package org.switchyard.cdi.transform;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public interface Transform {
    Object transform(Object payload);
}
