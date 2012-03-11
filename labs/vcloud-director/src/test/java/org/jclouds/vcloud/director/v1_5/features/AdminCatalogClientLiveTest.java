/*
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
package org.jclouds.vcloud.director.v1_5.features;

import static com.google.common.base.Objects.equal;
import static org.jclouds.vcloud.director.v1_5.VCloudDirectorLiveTestConstants.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.net.URI;

import org.jclouds.vcloud.director.v1_5.VCloudDirectorException;
import org.jclouds.vcloud.director.v1_5.domain.AdminCatalog;
import org.jclouds.vcloud.director.v1_5.domain.Checks;
import org.jclouds.vcloud.director.v1_5.domain.Error;
import org.jclouds.vcloud.director.v1_5.domain.Owner;
import org.jclouds.vcloud.director.v1_5.domain.PublishCatalogParams;
import org.jclouds.vcloud.director.v1_5.domain.Reference;
import org.jclouds.vcloud.director.v1_5.domain.ReferenceType;
import org.jclouds.vcloud.director.v1_5.internal.BaseVCloudDirectorClientLiveTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests live behavior of {@link AdminCatalogClient}.
 * 
 * @author danikov
 */
@Test(groups = { "live", "admin", "catalog" }, singleThreaded = true, testName = "CatalogClientLiveTest")
public class AdminCatalogClientLiveTest extends BaseVCloudDirectorClientLiveTest {
   
   public static final String CATALOG = "admin catalog";

   /*
    * Convenience references to API clients.
    */

   private AdminCatalogClient catalogClient;

   /*
    * Shared state between dependant tests.
    */
   private ReferenceType<?> catalogRef;
   private AdminCatalog catalog;
   private Owner owner;

   @BeforeClass(inheritGroups = true)
   public void setupRequiredClients() {
      catalogClient = context.getApi().getAdminCatalogClient();
      catalogRef = Reference.builder()
         .href(URI.create("https://vcloudbeta.bluelock.com/api/admin/catalog/7212e451-76e1-4631-b2de-ba1dfd8080e4"))
         .build();
   }

   @Test(testName = "GET /admin/catalog/{id}")
   public void testGetCatalog() {
      assertNotNull(catalogRef, String.format(REF_REQ_LIVE, "Catalog"));
      catalog = catalogClient.getCatalog(catalogRef.getHref());
      
      Checks.checkAdminCatalog(catalog);
   }
   
   @Test(testName = "GET /admin/catalog/{id}/owner",
         dependsOnMethods = { "testGetCatalog" })
   public void testGetCatalogOwner() {
      owner = catalogClient.getOwner(catalog.getHref());
      Checks.checkOwner(owner);
   }
   
   @Test(testName = "PUT /admin/catalog/{id}/owner",
         dependsOnMethods = { "testGetCatalog" })
   public void updateCatalogOwner() {
      Owner oldOwner = owner;
      Owner newOwner = Owner.builder() // TODO auto-find a new owner?
            .type("application/vnd.vmware.vcloud.owner+xml")
            .user(Reference.builder()
                  .type("application/vnd.vmware.admin.user+xml")
                  .name("adk@cloudsoftcorp.com")
                  .href(URI.create("https://vcloudbeta.bluelock.com/api/admin/user/e9eb1b29-0404-4c5e-8ef7-e584acc51da9"))
                  .build())
            .build();
      
      try {
         catalogClient.setOwner(catalog.getHref(), newOwner);
         owner = catalogClient.getOwner(catalog.getHref());
         Checks.checkOwner(owner);
         assertTrue(equal(owner, newOwner), String.format(OBJ_FIELD_UPDATABLE, CATALOG, "owner"));
      } finally {
         catalogClient.setOwner(catalog.getHref(), oldOwner);
         owner = catalogClient.getOwner(catalog.getHref());
      }
   }
   
   @Test(testName = "PUT /admin/catalog/{id}", dependsOnMethods = { "testGetCatalogOwner" })
   public void testUpdateCatalog() {
      String oldName = catalog.getName();
      String newName = "new "+oldName;
      String oldDescription = catalog.getDescription();
      String newDescription = "new "+oldDescription;
      // TODO: can we update/manage catalogItems directly like this? or does it just do a merge (like metadata)
//      CatalogItems oldCatalogItems = catalog.getCatalogItems();
//      CatalogItems newCatalogItems = CatalogItems.builder().build();
      
      try {
         catalog = catalog.toBuilder()
               .name(newName)
               .description(newDescription)
//               .catalogItems(newCatalogItems)
               .build();
         
         catalog = catalogClient.updateCatalog(catalog.getHref(), catalog);
         
         assertTrue(equal(catalog.getName(), newName), String.format(OBJ_FIELD_UPDATABLE, CATALOG, "name"));
         assertTrue(equal(catalog.getDescription(), newDescription),
               String.format(OBJ_FIELD_UPDATABLE, CATALOG, "description"));
//         assertTrue(equal(catalog.getCatalogItems(), newCatalogItems), String.format(OBJ_FIELD_UPDATABLE, CATALOG, "catalogItems"));
         
         //TODO negative tests?
         
         Checks.checkAdminCatalog(catalog);
      } finally {
         catalog = catalog.toBuilder()
               .name(oldName)
               .description(oldDescription)
//               .catalogItems(oldCatalogItems)
               .build();
         
         catalog = catalogClient.updateCatalog(catalog.getHref(), catalog);
      }
   }
   
   @Test(testName = "POST /admin/catalog/{id}/action/publish",
         dependsOnMethods = { "testUpdateCatalog" }, enabled = false )
   public void testPublishCatalog() {
      assertTrue(!catalog.isPublished(), String.format(OBJ_FIELD_EQ, 
            CATALOG, "isPublished", false, catalog.isPublished()));
      
      PublishCatalogParams params = PublishCatalogParams.builder()
         .isPublished(true)
         .build();
      
      catalogClient.publishCatalog(catalogRef.getHref(), params);
      catalog = catalogClient.getCatalog(catalogRef.getHref());
      
      assertTrue(catalog.isPublished(), String.format(OBJ_FIELD_EQ, 
            CATALOG, "isPublished", true, catalog.isPublished()));
      
   }
   
   @Test(testName = "DELETE /admin/catalog/{id}",
         dependsOnMethods = { "testPublishCatalog" }, enabled = false )
   public void testDeleteCatalog() {
      catalogClient.deleteCatalog(catalogRef.getHref());
      
      Error expected = Error.builder()
            .message("???")
            .majorErrorCode(403)
            .minorErrorCode("ACCESS_TO_RESOURCE_IS_FORBIDDEN")
            .build();
      
      try {
         catalog = catalogClient.getCatalog(catalogRef.getHref());
         fail("Should give HTTP 403 error");
      } catch (VCloudDirectorException vde) {
         assertEquals(vde.getError(), expected);
         catalog = null;
      } catch (Exception e) {
         fail("Should have thrown a VCloudDirectorException");
      }
      
      if (catalog != null) { // guard against NPE on the .toStrings
         assertNull(catalog, String.format(OBJ_DEL, CATALOG, catalog.toString()));
      }
   }
}