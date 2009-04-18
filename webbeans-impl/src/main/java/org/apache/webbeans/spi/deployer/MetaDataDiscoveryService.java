/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.spi.deployer;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.apache.webbeans.exception.WebBeansDeploymentException;

/**
 * This SPI is for abstracting the class scanning.  
 *
 * In a production environment Many different modules need to perform 
 * class scanning (EJB, JSF, JPA, ...). This SPI allows us to only have one 
 * central class scanner for the whole application server
 * which only performs the scanning once at startup of each WebApp.
 */
public interface MetaDataDiscoveryService
{
    /**
     * initialise the class scanner
     * @param object
     */
    public void init(Object object);

    /**
     * Perform the actual class scanning.
     * @throws WebBeansDeploymentException
     */
    public void scan() throws WebBeansDeploymentException;
    
    /**
     * @return the locations of the beans.xml files. 
     */
    public Set<URL> getWebBeansXmlLocations();
    
    /**
     * Get all scanned classes and all annotations used by each very class.
     * @return key is the list of scanned classes, Set is a list of annotations used by that class.
     */
    Map<String, Set<String>> getClassIndex();

    /**
     * Get all used annotations with all classes which usess this annotation somehow.   
     * @return key is the fully qualified string name of a annotation class, Set is a 
     * list of classes that use that annotation somehow.
     */
    Map<String, Set<String>> getAnnotationIndex();
    
}
