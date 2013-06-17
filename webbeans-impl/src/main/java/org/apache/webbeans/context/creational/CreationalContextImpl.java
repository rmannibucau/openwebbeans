/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.context.creational;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

/** {@inheritDoc} */
public class CreationalContextImpl<T> implements CreationalContext<T>, Serializable
{
    //Default serial id
    private static final long serialVersionUID = 1L;

    /**
     * The delegate object to be injected into delegate injection points
     */
    private transient T delegate;
    
    /**
     * The injection point object to be injected into injection points of type InjectionPoint
     */
    private transient Stack<InjectionPoint> injectionPoints = null;

    /**
     * Contextual bean dependent instances
     *
     * <p><b>ATTENTION</b> This variable gets initiated lazily!</p>
     */
    private List<DependentCreationalContext<?>> dependentObjects = null;

    /**
     * Contains the currently created bean
     */
    private Bean<T> bean = null;
    
    /**
     * Contains the currently created contextual, may be a bean, interceptor or decorator 
     */
    private Contextual<T> contextual = null;

    private WebBeansContext webBeansContext;
    
    /**
     * This flag will get set to <code>true</code> to prevent recursive loops while destroying
     * the CreationContext.
     */
    private boolean destroying = false;

    /**
     * Package private
     */
    CreationalContextImpl(Contextual<T> contextual, WebBeansContext webBeansContext)
    {
        if (contextual instanceof Bean)
        {
            this.bean = (Bean<T>)contextual;
        }
        this.contextual = contextual;
        this.webBeansContext = webBeansContext;
    }
    
    public WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }

    public T getDelegate()
    {
        return this.delegate;
    }

    /**
     * @return the previously set delegate instance
     */
    public T putDelegate(T delegate)
    {
        T oldValue = this.delegate;
        this.delegate = delegate;
        return oldValue;
    }

    public InjectionPoint getInjectionPoint()
    {
        if (injectionPoints == null || injectionPoints.isEmpty())
        {
            return null;
        }
        return injectionPoints.peek();
    }

    public void putInjectionPoint(InjectionPoint injectionPoint)
    {
        if (injectionPoints == null)
        {
            injectionPoints = new Stack<InjectionPoint>();
        }
        injectionPoints.push(injectionPoint);
    }

    public InjectionPoint removeInjectionPoint()
    {
        if (injectionPoints == null || injectionPoints.isEmpty())
        {
            return null;
        }
        return injectionPoints.pop();
    }

    /**
     * Save this incomplete instance.
     * 
     * @param incompleteInstance incomplete bean instance
     */
    @Override
    public void push(T incompleteInstance)
    {
        //No-action
    }
     
    /**
     * Adds given dependent instance to the map.
     * 
     * @param dependent dependent contextual
     * @param instance dependent instance
     */
    public <K> void addDependent(Contextual<K> dependent, Object instance)
    {
        Asserts.assertNotNull(dependent,"dependent parameter cannot be null");

        if(instance != null)
        {
            DependentCreationalContext<K> dependentCreational = new DependentCreationalContext<K>(dependent);
            dependentCreational.setInstance(instance);

            synchronized(this)
            {
                if (dependentObjects == null)
                {
                    dependentObjects = new ArrayList<DependentCreationalContext<?>>();
                }

                if (dependent == bean)
                {
                    dependentObjects.add(0, dependentCreational);
                }
                else
                {
                    dependentObjects.add(dependentCreational);
                }
            }
        }
    }

    public boolean containsDependent(Contextual<?> contextual, Object instance)
    {
        if (dependentObjects == null)
        {
            return false;
        }
        synchronized (this)
        {
            for (DependentCreationalContext<?> dependentCreationalContext: dependentObjects)
            {
                if (dependentCreationalContext.getContextual().equals(contextual) && dependentCreationalContext.getInstance() == instance)
                {
                    return true;
                }
            }
            return false;
        }
    }

    public <X> void destroyDependent(X instance)
    {
        if (dependentObjects == null)
        {
            return;
        }
        synchronized (this)
        {
            for (Iterator<DependentCreationalContext<?>> i = dependentObjects.iterator(); i.hasNext();)
            {
                DependentCreationalContext<?> dependentContext = i.next();
                if (dependentContext.getInstance() == instance)
                {
                    Contextual<X> dependentContextual = (Contextual<X>)dependentContext.getContextual();
                    CreationalContext<X> creationalContext = (CreationalContext<X>)this;
                    dependentContextual.destroy(instance, creationalContext);
                    i.remove();
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void removeAllDependents()
    {
        if (dependentObjects == null || destroying)
        {
            return;
        }
        
        destroying = true;

        synchronized(this)
        {
            if (dependentObjects != null)
            {
                // this is kind of an emergency valve...
                int maxRemoval = dependentObjects.size() * 3;
                while (!dependentObjects.isEmpty() && maxRemoval > 0)
                {
                    // we don't use an iterator because the destroyal might register a 
                    // fresh PreDestroy interceptor as dependent object...
                    DependentCreationalContext<T> dependent = (DependentCreationalContext<T>) dependentObjects.get(0);
                    dependentObjects.remove(0);
                    dependent.getContextual().destroy((T) dependent.getInstance(), this);
                    maxRemoval--;
                }
                    
                if (maxRemoval == 0)
                {
                    throw new WebBeansException("infinite loop detected while destroying bean " + bean);
                }
            }
        }

        dependentObjects = null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        removeAllDependents();
    }
    
    public Bean<T> getBean()
    {
        return bean;
    }

    public Bean<T> putBean(Bean<T> newBean)
    {
        Bean<T> oldBean = bean;
        bean = newBean;
        return oldBean;
    }
    
    public Contextual<T> getContextual()
    {
        return contextual;
    }

    public Contextual<T> putContextual(Contextual<T> newContextual)
    {
        Contextual<T> oldContextual = contextual;
        contextual = newContextual;
        return oldContextual;
    }

    /**
     * Write Object. 
     */
    private void writeObject(ObjectOutputStream s)
    throws IOException
    {
        s.writeObject(dependentObjects);

        String id = WebBeansUtil.getPassivationId(bean);
        if (bean != null && id != null)
        {
            s.writeObject(id);
        }
        else
        {
            s.writeObject(null);
        }
    }


    /**
     * Read object. 
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
        webBeansContext = WebBeansContext.currentInstance();
        dependentObjects = (List<DependentCreationalContext<?>>)s.readObject();

        String id = (String) s.readObject();
        if (id != null)
        {
            bean = (Bean<T>) webBeansContext.getBeanManagerImpl().getPassivationCapableBean(id);
        }

    }

    @Override
    public String toString()
    {

        final StringBuilder sb = new StringBuilder("CreationalContext{name=");

        sb.append(bean.getBeanClass().getSimpleName());

        return sb.append("}").toString();
    }
}
