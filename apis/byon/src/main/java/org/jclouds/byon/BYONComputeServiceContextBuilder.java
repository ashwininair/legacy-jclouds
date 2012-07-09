/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.byon;

import java.util.List;
import java.util.Properties;

import org.jclouds.byon.config.BYONComputeServiceContextModule;
import org.jclouds.byon.config.ConfiguresNodeStore;
import org.jclouds.byon.config.YamlNodeStoreModule;
import org.jclouds.compute.StandaloneComputeServiceContextBuilder;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.inject.Module;

/**
 * 
 * @author Adrian Cole
 */
@SuppressWarnings("rawtypes")
public class BYONComputeServiceContextBuilder extends StandaloneComputeServiceContextBuilder<Supplier> {

   public BYONComputeServiceContextBuilder(Properties props) {
      super(Supplier.class, props);
   }

   @Override
   protected void addContextModule(List<Module> modules) {
      modules.add(new BYONComputeServiceContextModule());
      addNodeStoreModuleIfNotPresent(modules);
   }

   protected void addNodeStoreModuleIfNotPresent(List<Module> modules) {
      if (!Iterables.any(modules, new Predicate<Module>() {
         public boolean apply(Module input) {
            return input.getClass().isAnnotationPresent(ConfiguresNodeStore.class);
         }

      })) {
         addNodeStoreModule(modules);
      }
   }

   protected void addNodeStoreModule(List<Module> modules) {
      modules.add(new YamlNodeStoreModule());
   }
}