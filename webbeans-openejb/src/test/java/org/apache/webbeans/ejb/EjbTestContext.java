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
package org.apache.webbeans.ejb;

import org.apache.openejb.OpenEJB;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.ejb.component.OpenEjbBean;
import org.apache.webbeans.plugins.PluginLoader;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.portable.events.generics.GProcessAnnotatedType;

public abstract class EjbTestContext
{    
    
    protected EjbTestContext(String name)
    {
        BeanManagerImpl.getManager();
    }
 
    
    protected static void initEjb()
    {
        try
        {
            PluginLoader.getInstance().startUp();
            
            System.out.println("INIT EJB");
            
            OpenEJB.init(System.getProperties());
            System.setProperty("EjbPlugin.test", "true");
            
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    protected static void destroyEjb()
    {
        try
        {
            System.out.println("DESTROY EJB");
            
            OpenEJB.destroy();
            
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    protected <T> OpenEjbBean<T> defineEjbBean(Class<T> ejbClass)
    {
        EjbPlugin plugin = new EjbPlugin();
       return (OpenEjbBean<T>)plugin.defineSessionBean(ejbClass, 
               new GProcessAnnotatedType(AnnotatedElementFactory.getInstance().newAnnotatedType(ejbClass)));
    }
}