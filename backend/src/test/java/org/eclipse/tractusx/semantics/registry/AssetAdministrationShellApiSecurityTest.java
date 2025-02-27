/********************************************************************************
 * Copyright (c) 2021-2023 Robert Bosch Manufacturing Solutions GmbH
 * Copyright (c) 2021-2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.tractusx.semantics.registry;

import static org.hamcrest.Matchers.*;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.eclipse.tractusx.semantics.aas.registry.model.AssetAdministrationShellDescriptor;
import org.eclipse.tractusx.semantics.aas.registry.model.Key;
import org.eclipse.tractusx.semantics.aas.registry.model.KeyTypes;
import org.eclipse.tractusx.semantics.aas.registry.model.Reference;
import org.eclipse.tractusx.semantics.aas.registry.model.ReferenceTypes;
import org.eclipse.tractusx.semantics.aas.registry.model.SpecificAssetId;
import org.eclipse.tractusx.semantics.aas.registry.model.SubmodelDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.eclipse.tractusx.semantics.registry.TestUtil.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *  This class contains test to verify Authentication and RBAC based Authorization for all API endpoints.
 *  Every API endpoint is tested explicitly.
 */
public class AssetAdministrationShellApiSecurityTest extends AbstractAssetAdministrationShellApi {

    @Nested
    @DisplayName("Authentication Tests")
    class SecurityTests {
        @Test
        public void testWithoutAuthenticationTokenProvidedExpectUnauthorized() throws Exception {
                       mvc.perform(
                            MockMvcRequestBuilders
                                  .get(SINGLE_SHELL_BASE_PATH, UUID.randomUUID())
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        public void testWithAuthenticationTokenProvidedExpectUnauthorized() throws Exception {
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SHELL_BASE_PATH, UUID.randomUUID())
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        public void testWithInvalidAuthenticationTokenConfigurationExpectUnauthorized() throws Exception {
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SHELL_BASE_PATH, UUID.randomUUID())
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.withoutResourceAccess())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SHELL_BASE_PATH, UUID.randomUUID())
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.withoutRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());
        }

    }

    @Nested
    @DisplayName("Shell Authorization Test")
    class ShellCrudTest {
        String shellId;

        @BeforeEach
        public void before() throws Exception{
            AssetAdministrationShellDescriptor shellPayload1 = TestUtil.createCompleteAasDescriptor();
            shellPayload1.setId(UUID.randomUUID().toString());
            performShellCreateRequest(mapper.writeValueAsString(shellPayload1));
            shellId = shellPayload1.getId();

        }

        @Test
        public void testRbacForGetAll() throws Exception {
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    // test with wrong role
                                    .with(jwtTokenFactory.addTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk());
        }

        @Test
        public void testRbacForGetById() throws Exception {
            // get shell by id
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SHELL_BASE_PATH, shellId )
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    // test with wrong role
                                    .with(jwtTokenFactory.deleteTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SHELL_BASE_PATH, getEncodedValue(shellId  ) )
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk());
        }

        @Test
        public void testRbacForCreate() throws Exception {
            AssetAdministrationShellDescriptor shellPayload1 = TestUtil.createCompleteAasDescriptor();
            shellPayload1.setId(UUID.randomUUID().toString());
            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(shellPayload1))
                                    // test with wrong role
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            shellPayload1.setId(UUID.randomUUID().toString());
            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH, mapper.writeValueAsString(shellPayload1) )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(shellPayload1))
                                    .with(jwtTokenFactory.addTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated());
        }

        @Test
        public void testRbacForUpdate() throws Exception {

           AssetAdministrationShellDescriptor testAas = TestUtil.createCompleteAasDescriptor();
           testAas.setId( shellId );

           String shellPayloadForUpdate = mapper.writeValueAsString(testAas);
            mvc.perform(
                            MockMvcRequestBuilders
                                    .put(SINGLE_SHELL_BASE_PATH, getEncodedValue(shellId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(shellPayloadForUpdate)
                                    // test with wrong role
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());


            mvc.perform(
                            MockMvcRequestBuilders
                                    .put(SINGLE_SHELL_BASE_PATH, getEncodedValue(shellId) )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(shellPayloadForUpdate)
                                    .with(jwtTokenFactory.updateTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent());

        }

        @Test
        public void testRbacForDelete() throws Exception {
            mvc.perform(
                            MockMvcRequestBuilders
                                    .delete(SINGLE_SHELL_BASE_PATH, getEncodedValue(shellId) )
                                    // test with wrong role
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .delete(SINGLE_SHELL_BASE_PATH, getEncodedValue(shellId) )
                                    // test with wrong role
                                    .with(jwtTokenFactory.deleteTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent());
        }

    }

    @Nested
    @DisplayName("Submodel Descriptor Authorization Test")
    class SubmodelDescriptorCrudTests {
       private String shellId;
       private String submodelId;
       private String submodelIdAas;

       @BeforeEach
       public void before() {

       }


       @Test
       public void testRbacForGetAll() throws Exception {
           AssetAdministrationShellDescriptor testAas = TestUtil.createCompleteAasDescriptor();
           testAas.setId(UUID.randomUUID().toString());
           performShellCreateRequest(mapper.writeValueAsString(testAas));
           shellId = testAas.getId();

           mvc.perform(
                           MockMvcRequestBuilders
                                   .get(SUB_MODEL_BASE_PATH,  shellId )
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                   .accept(MediaType.APPLICATION_JSON)
                                   // test with wrong role
                                   .with(jwtTokenFactory.addTwin())
                   )
                   .andDo(MockMvcResultHandlers.print())
                   .andExpect(status().isForbidden());

           mvc.perform(
                           MockMvcRequestBuilders
                                   .get(SUB_MODEL_BASE_PATH,  getEncodedValue( shellId ) )
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                   .accept(MediaType.APPLICATION_JSON)
                                   .with(jwtTokenFactory.readTwin())
                   )
                   .andDo(MockMvcResultHandlers.print())
                   .andExpect(status().isOk());
       }

        @Test
        public void testRbacForGetById() throws Exception {

           AssetAdministrationShellDescriptor testAas = TestUtil.createCompleteAasDescriptor();
            testAas.setId(UUID.randomUUID().toString());
          performShellCreateRequest(mapper.writeValueAsString(testAas));
          shellId = testAas.getId();
            submodelIdAas = testAas.getSubmodelDescriptors().get( 0 ).getId();

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SUB_MODEL_BASE_PATH, shellId, submodelIdAas )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    // test with wrong role
                                    .with(jwtTokenFactory.deleteTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SUB_MODEL_BASE_PATH, getEncodedValue( shellId ), getEncodedValue( submodelIdAas ) )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk());
        }

        @Test
        public void testRbacForCreate() throws Exception {

           SubmodelDescriptor testSubmodelDescriptor = TestUtil.createSubmodel();
           testSubmodelDescriptor.setId( UUID.randomUUID().toString() );
           String submodelPayloadForCreate = mapper.writeValueAsString(testSubmodelDescriptor);

            AssetAdministrationShellDescriptor testAas = TestUtil.createCompleteAasDescriptor();
            testAas.setId(UUID.randomUUID().toString() );
            performShellCreateRequest(mapper.writeValueAsString(testAas));
            shellId = testAas.getId();


            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SUB_MODEL_BASE_PATH, shellId )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(submodelPayloadForCreate )
                                    // test with wrong role
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            testSubmodelDescriptor.setId(UUID.randomUUID().toString());


            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SUB_MODEL_BASE_PATH, getEncodedValue( shellId ))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content( submodelPayloadForCreate )
                                    .with(jwtTokenFactory.addTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated());

        }

        @Test
        public void testRbacForUpdate() throws Exception {

            AssetAdministrationShellDescriptor testAas = TestUtil.createCompleteAasDescriptor();
            testAas.setId(UUID.randomUUID().toString() );
            performShellCreateRequest(mapper.writeValueAsString(testAas));
            shellId = testAas.getId();
            submodelIdAas = testAas.getSubmodelDescriptors().get( 0 ).getId();

           SubmodelDescriptor testSubmodelDescriptor = TestUtil.createSubmodel();
           testSubmodelDescriptor.setId(   submodelIdAas );



           String submodelPayloadForCreate = mapper.writeValueAsString(testSubmodelDescriptor);

            mvc.perform(
                            MockMvcRequestBuilders
                                    .put(SINGLE_SUB_MODEL_BASE_PATH, shellId, submodelId )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(submodelPayloadForCreate)
                                    // test with wrong role
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .put(SINGLE_SUB_MODEL_BASE_PATH, getEncodedValue( shellId ), getEncodedValue(submodelIdAas))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(submodelPayloadForCreate)
                                    .with(jwtTokenFactory.updateTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent());
        }

        @Test
        public void testRbacForDelete() throws Exception {
            AssetAdministrationShellDescriptor testAas = TestUtil.createCompleteAasDescriptor();
            testAas.setId(UUID.randomUUID().toString() );
            performShellCreateRequest(mapper.writeValueAsString(testAas));
            shellId = testAas.getId();
            submodelIdAas = testAas.getSubmodelDescriptors().get( 0 ).getId();
            mvc.perform(
                            MockMvcRequestBuilders
                                    .delete(SINGLE_SUB_MODEL_BASE_PATH, shellId, submodelIdAas)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    // test with wrong role
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .delete(SINGLE_SUB_MODEL_BASE_PATH, getEncodedValue( shellId ), getEncodedValue( submodelIdAas ))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.deleteTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent());
        }

    }

    @Nested
    @DisplayName("SpecificAssetIds Crud Test")
    class SpecificAssetIdsCrudTest {
        String shellId;

        @BeforeEach
        public void before() throws Exception{
            AssetAdministrationShellDescriptor testAas = TestUtil.createCompleteAasDescriptor();
            testAas.setId(UUID.randomUUID().toString());
            performShellCreateRequest(mapper.writeValueAsString(testAas));

            shellId = testAas.getId();
        }

        @Test
        public void testRbacForGet() throws Exception {
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_LOOKUP_SHELL_BASE_PATH, shellId)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.deleteTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_LOOKUP_SHELL_BASE_PATH, getEncodedValue( shellId ))
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk());
        }

        @Test
        public void testRbacForCreate() throws Exception {
            ArrayNode specificAssetIds = emptyArrayNode()
                    .add(specificAssetId("key1", "value1"))
                    .add(specificAssetId("key2", "value2"));

            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SINGLE_LOOKUP_SHELL_BASE_PATH, shellId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(toJson(specificAssetIds))
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SINGLE_LOOKUP_SHELL_BASE_PATH, getEncodedValue( shellId ))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(toJson(specificAssetIds))
                                    .with(jwtTokenFactory.addTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated());
        }

        @Test
        public void testRbacForDelete() throws Exception {
            mvc.perform(
                            MockMvcRequestBuilders
                                    .delete(SINGLE_LOOKUP_SHELL_BASE_PATH, shellId)
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .delete(SINGLE_LOOKUP_SHELL_BASE_PATH, getEncodedValue( shellId ))
                                    .with(jwtTokenFactory.deleteTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Lookup Authorization Test")
    class LookupTest {

        @Test
        public void testRbacForLookupByAssetIds() throws Exception {

            SpecificAssetId specificAssetId = TestUtil.createSpecificAssetId();
           String encodedObject = Base64.getUrlEncoder().encodeToString(serialize( specificAssetId));

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(LOOKUP_SHELL_BASE_PATH)
                                    .queryParam("assetIds", encodedObject)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.addTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());
    mvc.perform(
                    MockMvcRequestBuilders
                            .get(LOOKUP_SHELL_BASE_PATH)
                            .param("assetIds",encodedObject)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .queryParam("limit",  "10")
                            .accept(MediaType.APPLICATION_JSON_VALUE)
                            .with(jwtTokenFactory.readTwin())
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk());
        }

    }

    @Nested
    @DisplayName("Custom AAS API Authorization Tests")
    class CustomAASApiTest {

        //TODO: Test will be ignored, because the new api does not provided batch, fetch and query. This will be come later in version 0.3.1
        // @Test
        public void testRbacCreateShellInBatch() throws Exception {
            ObjectNode shell = createShell();
            ArrayNode batchShellBody = emptyArrayNode().add(shell);

            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH + "/batch")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(toJson(batchShellBody))
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH + "/batch")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(toJson(batchShellBody))
                                    .with(jwtTokenFactory.addTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated());
        }

//        @Test - don't have /fetch
        public void testRbacForFetchShellsByIds() throws Exception {
            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH + "/fetch")
                                    .content(toJson(emptyArrayNode()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.deleteTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden());

            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH + "/fetch")
                                    .content(toJson(emptyArrayNode()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.readTwin())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(0)));
        }
    }

    /**
     * The specificAssetId#externalSubjectId indicates which tenant is allowed to see the specificAssetId and
     * find a Shell.
     *
     * Given:
     *  - Company A creates an AAS with multiple with: 1. one specificAssetId without externalSubjectId,
     *                                                 2. one with externalSubjectId = Company B
     *                                                 3. one with externalSubjectId = Company C
     *
     *   - Rules: When Company A requests the AAS, all specificAssetIds 1,2 and are shown. Company A is the owner of the AAS.
     *               The AAS Registry has an environment property "owningTenantId" that is compared with the tenantId from the token.
     *            When Company B requests the AAS, only specificAssetIds 1 and 2 are shown.
     *            When Company C requests the AAS, only specificAssetIds 1 and 3 are shown.
     *
     *            The same logic applies also to the lookup endpoints.
     *
     */
    @Nested
    @DisplayName("Tenant based specificAssetId visibility test")
    class TenantBasedVisibilityTest {

        @Test
        public void testGetAllShellsWithDefaultClosedFilteredSpecificAssetIdsByTenantId() throws Exception {
            AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
            shellPayload.setId(UUID.randomUUID().toString());
            List<SpecificAssetId> shellpayloadSpecificAssetIDs = shellPayload.getSpecificAssetIds();
            shellpayloadSpecificAssetIDs.forEach( specificAssetId -> specificAssetId.setExternalSubjectId( null ) );
            shellPayload.setSpecificAssetIds( shellpayloadSpecificAssetIDs );

            performShellCreateRequest(mapper.writeValueAsString(shellPayload));


            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .queryParam("pageSize", "100")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").exists());

            // test with tenant two
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                                    .queryParam("pageSize", "100")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.tenantTwo().allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").exists())
                    .andExpect(jsonPath("$.result[*].specificAssetIds[*].value", not(hasItems("identifier1ValueExample", "identifier2ValueExample","tenantThreeAssetIdValue"))));
        }

        @Test
        public void testGetShellWithFilteredSpecificAssetIdsByTenantId() throws Exception {

            AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
            shellPayload.setSpecificAssetIds(null);
            SpecificAssetId asset1 = TestUtil.createSpecificAssetId("CustomerPartId","tenantTwoAssetIdValue",List.of(jwtTokenFactory.tenantTwo().getTenantId()));
            SpecificAssetId asset2 = TestUtil.createSpecificAssetId("CustomerPartId2","tenantThreeAssetIdValue",List.of(jwtTokenFactory.tenantThree().getTenantId()));
            SpecificAssetId asset3 = TestUtil.createSpecificAssetId("MaterialNumber","withoutTenantAssetIdValue",List.of(jwtTokenFactory.tenantTwo().getTenantId()));
            // Define specificAsset with wildcard which not allowed. (Only manufacturerPartId is defined in application.yml)
            SpecificAssetId asset4 = TestUtil.createSpecificAssetId("BPID","ignoreWildcard",List.of(getExternalSubjectIdWildcardPrefix()));
           // Define specificAsset with wildcard which is allowed. (Only manufacturerPartId is defined in application.yml)
           SpecificAssetId asset5 = TestUtil.createSpecificAssetId("manufacturerPartId","wildcardAllowed",List.of(getExternalSubjectIdWildcardPrefix()));

            shellPayload.setSpecificAssetIds(List.of(asset1,asset2,asset3,asset4,asset5));


            shellPayload.setId(UUID.randomUUID().toString());
            performShellCreateRequest(mapper.writeValueAsString(shellPayload));

            String shellId = shellPayload.getId();
           String encodedShellId = getEncodedValue(shellId  );
           // Owner of tenant has access to all specificAssetIds
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SHELL_BASE_PATH, encodedShellId)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(shellId)))
                    .andExpect(jsonPath("$.specificAssetIds[*].value", containsInAnyOrder("tenantTwoAssetIdValue","tenantThreeAssetIdValue", "withoutTenantAssetIdValue","ignoreWildcard","wildcardAllowed")));

            // test with tenant two
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(SINGLE_SHELL_BASE_PATH, encodedShellId)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.tenantTwo().allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(shellId)))
                    .andExpect(jsonPath("$.specificAssetIds[*].value", hasItems("tenantTwoAssetIdValue", "withoutTenantAssetIdValue","wildcardAllowed")))
                    .andExpect(jsonPath("$.specificAssetIds[*].value", not(hasItems("tenantThreeAssetIdValue","ignoreWildcard"))));
        }

         //TODO: Test will be ignored, because the new api does not provided batch, fetch and query. This will be come later in version 0.3.1
        //@Test
        public void testFetchShellsWithFilteredSpecificAssetIdsByTenantId() throws Exception {
            ObjectNode shellPayload = createBaseIdPayload("example", "example");
            String tenantTwoAssetIdValue = "tenantTwofgkj129293";
            String tenantThreeAssetIdValue = "tenantThree543412394";
            String withoutTenantAssetIdValue = "withoutTenant329347192jf18";
            shellPayload.set("specificAssetIds", emptyArrayNode()
                    .add(specificAssetId("CustomerPartId", tenantTwoAssetIdValue,  jwtTokenFactory.tenantTwo().getTenantId()))
                    .add(specificAssetId("CustomerPartId", tenantThreeAssetIdValue, jwtTokenFactory.tenantThree().getTenantId()))
                    .add(specificAssetId("MaterialNumber",withoutTenantAssetIdValue))
            );
            performShellCreateRequest(toJson(shellPayload));
            String shellId = getId(shellPayload);

            ArrayNode queryPayload = emptyArrayNode().add(shellId);
            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH + "/fetch")
                                    .content(toJson(queryPayload))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[*].identification", hasItem(shellId)))
                    .andExpect(jsonPath("$.items[*].specificAssetIds[*].value", hasItems(tenantTwoAssetIdValue,tenantThreeAssetIdValue, withoutTenantAssetIdValue)));

            // test with tenant two
            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SHELL_BASE_PATH + "/fetch")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(toJson(queryPayload))
                                    .with(jwtTokenFactory.tenantTwo().allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[*].identification", hasItem(shellId)))
                    .andExpect(jsonPath("$.items[*].specificAssetIds[*].value", hasItems(tenantTwoAssetIdValue, withoutTenantAssetIdValue)))
                    .andExpect(jsonPath("$.items[*].specificAssetIds[*].value", not(hasItem(tenantThreeAssetIdValue))));
        }



        @Test
        public void testGetSpecificAssetIdsFilteredByTenantId() throws Exception {
            AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
            shellPayload.setId(UUID.randomUUID().toString());
            performShellCreateRequest(mapper.writeValueAsString(shellPayload));

            // Update specificIds only with one specificAssetId for tenantOne
            SpecificAssetId specificAssetId = new SpecificAssetId();
            Reference externalSubjectId = new Reference();
            Key key = new Key();
            key.setType(KeyTypes.SUBMODEL);
            key.setValue(jwtTokenFactory.tenantOne().getTenantId());
            externalSubjectId.setKeys(List.of(key));
            externalSubjectId.setType(ReferenceTypes.EXTERNALREFERENCE);
            specificAssetId.setName("findExternal_1_tenantOne");
            specificAssetId.setValue("value_1:tenantOne");
            specificAssetId.setExternalSubjectId(externalSubjectId);

            String shellId = shellPayload.getId();
            mvc.perform(
                            MockMvcRequestBuilders
                                    .post(SINGLE_LOOKUP_SHELL_BASE_PATH, getEncodedValue( shellId ))
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .accept(MediaType.APPLICATION_JSON)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(mapper.writeValueAsString(List.of(specificAssetId)))
                                    .with(jwtTokenFactory.allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated())
                    .andExpect(content().json(mapper.writeValueAsString(List.of(specificAssetId))));

           String encodedObject = Base64.getUrlEncoder().encodeToString(serialize( specificAssetId));

           mvc.perform(
                            MockMvcRequestBuilders
                                    .get(LOOKUP_SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .queryParam("assetIds", encodedObject)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result", hasSize(1)))
                    .andExpect(jsonPath("$.result", contains(shellPayload.getId())));

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(LOOKUP_SHELL_BASE_PATH)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                                    .queryParam("assetIds", encodedObject)
                                    .with(jwtTokenFactory.tenantTwo().allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result", hasSize(0)));
        }

        @Test
        public void testFindExternalShellIdsBySpecificAssetIdsWithTenantBasedVisibilityExpectSuccess() throws Exception {
            // the keyPrefix ensures that this test can run against a persistent database multiple times
            String keyPrefix = UUID.randomUUID().toString();
            // first shell
            AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
            shellPayload.setSpecificAssetIds(null);
            shellPayload.setId(UUID.randomUUID().toString());
            SpecificAssetId asset1 = TestUtil.createSpecificAssetId(keyPrefix + "findExternal_2","value_2",null);
            SpecificAssetId asset2 = TestUtil.createSpecificAssetId(keyPrefix + "findExternal_2_1","value_2_1",List.of(jwtTokenFactory.tenantTwo().getTenantId()));
            SpecificAssetId asset3 = TestUtil.createSpecificAssetId(keyPrefix + "findExternal_2_2","value_2_2",List.of(jwtTokenFactory.tenantThree().getTenantId()));

            shellPayload.setSpecificAssetIds(List.of(asset1,asset2,asset3));

            performShellCreateRequest(mapper.writeValueAsString(shellPayload));

            SpecificAssetId sa1 = TestUtil.createSpecificAssetId(keyPrefix + "findExternal_2_1","value_2_1",List.of(jwtTokenFactory.tenantTwo().getTenantId()));
            SpecificAssetId sa2 = TestUtil.createSpecificAssetId(keyPrefix + "findExternal_2_2","value_2_2",List.of(jwtTokenFactory.tenantThree().getTenantId()));

           String encodedSa1 = Base64.getUrlEncoder().encodeToString(serialize( sa1));
           String encodedSa2 = Base64.getUrlEncoder().encodeToString(serialize( sa2));
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(LOOKUP_SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                    .queryParam("assetIds", encodedSa1)
                                  .queryParam("assetIds", encodedSa2)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result", hasSize(1)))
                    // ensure that only three results match
                    .andExpect(jsonPath("$.result", contains(shellPayload.getId())));

            // test with tenantTwo assetId included

            SpecificAssetId specificAssetIdsWithTenantTwoIncluded = TestUtil.createSpecificAssetId(keyPrefix + "findExternal_2_2","value_2_2",null);
           String encodedSaWithTenantTwoIncluded = Base64.getUrlEncoder().encodeToString(serialize( specificAssetIdsWithTenantTwoIncluded));
            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(LOOKUP_SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                                    .queryParam("assetIds", encodedSaWithTenantTwoIncluded)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.tenantTwo().allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                  .andExpect(jsonPath("$.result", hasSize(0)));

            // Test lookup with one assetId for tenant two and one without tenantId

            mvc.perform(
                            MockMvcRequestBuilders
                                    .get(LOOKUP_SHELL_BASE_PATH)
                                    .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                                  .queryParam("assetIds", encodedSa1)
                                  .queryParam("assetIds", encodedSa2)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(jwtTokenFactory.tenantTwo().allRoles())
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result", hasSize(1)))
                    // ensure that only three results match
                    .andExpect(jsonPath("$.result", contains(shellPayload.getId())));
        }

       @Test
       public void testFindExternalShellIdsBySpecificAssetIdsWithTenantBasedVisibilityAndWildcardExpectSuccess() throws Exception {
          // the keyPrefix ensures that this test can run against a persistent database multiple times
          String keyPrefix = UUID.randomUUID().toString();
          AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
          shellPayload.setSpecificAssetIds(null);
          shellPayload.setId(UUID.randomUUID().toString());

          // asset1 is only visible for the owner because the externalSubjectId = null
          SpecificAssetId asset1 = TestUtil.createSpecificAssetId(keyPrefix + "defaultClosed","value_1",null);
          // asset2 is visible for everyone, because externalSubjectId = PUBLIC_READABLE and specificAssetKey is manufacturerPartId (which is in the list of allowedTypes via application.yml)
          SpecificAssetId asset2 = TestUtil.createSpecificAssetId("manufacturerPartId","value_2",List.of(getExternalSubjectIdWildcardPrefix()));
          // asset3 is visible only for the owner, because externalSubjectId = PUBLIC_READABLE but specificAssetKey is bpId (which is not in the list of allowedTypes via application.yml)
          SpecificAssetId asset3 = TestUtil.createSpecificAssetId("bpId","value_3",List.of(getExternalSubjectIdWildcardPrefix()));
          // asset3 is visible for tenantTwo and tenantThree
          SpecificAssetId asset4 = TestUtil.createSpecificAssetId(keyPrefix + "tenantTwo_tenantThree","value_3",List.of(jwtTokenFactory.tenantTwo().getTenantId(),jwtTokenFactory.tenantThree().getTenantId()));
          // asset4 is visible for tenantTwo, because externalSubjectId = tenantTwo
          SpecificAssetId asset5 = TestUtil.createSpecificAssetId("tenantTwo","value_2_private",List.of(jwtTokenFactory.tenantTwo().getTenantId()));

          shellPayload.setSpecificAssetIds(List.of(asset1,asset2,asset3,asset4,asset5));

          performShellCreateRequest(mapper.writeValueAsString(shellPayload));

          SpecificAssetId sa1 = TestUtil.createSpecificAssetId( keyPrefix+ "defaultClosed","value_1",null);
          SpecificAssetId sa2 = TestUtil.createSpecificAssetId( "manufacturerPartId","value_2",null);
          SpecificAssetId sa3 = TestUtil.createSpecificAssetId( "bpId","value_3",null);
          SpecificAssetId sa4 = TestUtil.createSpecificAssetId(keyPrefix + "tenantTwo_tenantThree","value_3",null);
          SpecificAssetId sa5 = TestUtil.createSpecificAssetId("tenantTwo","value_2_private",null);

          String encodedSa1 = Base64.getUrlEncoder().encodeToString(serialize( sa1));
          String encodedSa2 = Base64.getUrlEncoder().encodeToString(serialize( sa2));
          String encodedSa3 = Base64.getUrlEncoder().encodeToString(serialize( sa3));
          String encodedSa4 = Base64.getUrlEncoder().encodeToString(serialize( sa4));
          String encodedSa5 = Base64.getUrlEncoder().encodeToString(serialize( sa5));

          // Make request with bpn of the owner
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(LOOKUP_SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                            .queryParam("assetIds", encodedSa1)
                            .queryParam("assetIds", encodedSa2)
                            .queryParam("assetIds", encodedSa3)
                            .queryParam("assetIds", encodedSa4)
                            .queryParam("assetIds", encodedSa5)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result", contains(shellPayload.getId())));

          // test with tenantTwo: returns shellId because the specificAssetIds matched
          // sa2 = manufacturerPartId (public for everyone)
          // sa5 = match bpn of tenantTwo
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(LOOKUP_SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                            .queryParam("assetIds", encodedSa2)
                            .queryParam("assetIds", encodedSa5)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.tenantTwo().allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result", contains(shellPayload.getId())));

          // test with tenantTwo: returns no shellId because the specificAssetId sa3 is set to public but the key is not in the list of public allowed types.
          // sa2 = manufacturerPartId (public for everyone)
          // sa3 = visible only for owner because key is not in the list of public allowed types.
          // sa5 = match bpn of tenantTwo
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(LOOKUP_SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                            .queryParam("assetIds", encodedSa2)
                            .queryParam("assetIds", encodedSa3)
                            .queryParam("assetIds", encodedSa5)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.tenantTwo().allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));

          // test with tenantThree: returns no shellId because the specificAssetId sa5 is only visible for tenantTwo
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(LOOKUP_SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantThree().getTenantId() )
                            .queryParam("assetIds", encodedSa2)
                            .queryParam("assetIds", encodedSa5)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.tenantThree().allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));
       }

       @Test
       public void testFindExternalShellIdsBySpecificAssetIdsWithDefaultClosedTenantBasedVisibilityExpectSuccess() throws Exception {
          // the keyPrefix ensures that this test can run against a persistent database multiple times
          String keyPrefix = UUID.randomUUID().toString();
          AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
          shellPayload.setSpecificAssetIds(null);
          shellPayload.setId(UUID.randomUUID().toString());

          // asset1 is only visible for the owner because the externalSubjectId = null (owner is TENANT_ONE)
          SpecificAssetId asset1 = TestUtil.createSpecificAssetId(keyPrefix + "defaultClosed","value_1",null);
          // asset2 is visible for everyone, because externalSubjectId = PUBLIC_READABLE
          SpecificAssetId asset2 = TestUtil.createSpecificAssetId(keyPrefix + "public_visible","value_2",List.of(getExternalSubjectIdWildcardPrefix()));
          shellPayload.setSpecificAssetIds(List.of(asset1,asset2));

          performShellCreateRequest(mapper.writeValueAsString(shellPayload));

          SpecificAssetId sa1 = TestUtil.createSpecificAssetId(keyPrefix + "defaultClosed","value_1",null);
          SpecificAssetId sa2 = TestUtil.createSpecificAssetId(keyPrefix + "public_visible","value_2",null);

          String encodedSa1 = Base64.getUrlEncoder().encodeToString(serialize( sa1));
          String encodedSa2 = Base64.getUrlEncoder().encodeToString(serialize( sa2));

          mvc.perform(
                      MockMvcRequestBuilders
                            .get(LOOKUP_SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                            .queryParam("assetIds", encodedSa1)
                            .queryParam("assetIds", encodedSa2)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result", contains(shellPayload.getId())));

          // test with tenantTwo: returns no shellId because specificAssetId sa1 is only visible for Owner.
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(LOOKUP_SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                            .queryParam("assetIds", encodedSa1)
                            .queryParam("assetIds", encodedSa2)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.tenantTwo().allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(0)));
       }
    }

    /**
    * The specificAssetId#externalSubjectId indicates which tenant is allowed to see the shell with all properties or not.
    */
   @Nested
   @DisplayName("Tenant based Shell visibility test")
   class TenantBasedShellVisibilityTest {

      @BeforeEach
       public void before() {
          shellRepository.deleteAll();
       }

       @Test
       public void testGetAllShellsByOwningTenantId() throws Exception {
          AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
          shellPayload.setId(UUID.randomUUID().toString());
          List<SpecificAssetId> shellpayloadSpecificAssetIDs = shellPayload.getSpecificAssetIds();
          // Make all specificAssetIds to closed with externalSubjectId==null.
          shellpayloadSpecificAssetIDs.forEach( specificAssetId -> specificAssetId.setExternalSubjectId( null ) );
          shellPayload.setSpecificAssetIds( shellpayloadSpecificAssetIDs );

          performShellCreateRequest(mapper.writeValueAsString(shellPayload));

         // Request with owner TenantId (TENANT_ONE) returns one shell
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].description[*]").isNotEmpty())
                .andExpect(jsonPath("$.result[0].idShort",is(shellPayload.getIdShort())));

          // Request with TenantId (TENANT_TWO) returns no shells, because the shell not includes the externalSubjectId of Tenant_two as specificId
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result", hasSize(0)));
       }

       @Test
       public void testGetAllShellsWithPublicAccessByTenantId() throws Exception {
          // the keyPrefix ensures that this test can run against a persistent database multiple times
          String keyPrefix = UUID.randomUUID().toString();
          AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
          shellPayload.setSpecificAssetIds(null);
          shellPayload.setId(UUID.randomUUID().toString());

          // asset1 is only visible for the owner because the externalSubjectId = null
          SpecificAssetId asset1 = TestUtil.createSpecificAssetId(keyPrefix + "defaultClosed","value_1",null);
          // asset2 is visible for everyone, because externalSubjectId = PUBLIC_READABLE and specificAssetKey is manufacturerPartId (which is in the list of allowedTypes via application.yml)
          SpecificAssetId asset2 = TestUtil.createSpecificAssetId("manufacturerPartId","value_2",List.of(getExternalSubjectIdWildcardPrefix()));
          // asset3 is visible for tenantTwo, because externalSubjectId = tenantTwo
          SpecificAssetId asset3 = TestUtil.createSpecificAssetId("tenantTwo","value_2_public",List.of(jwtTokenFactory.tenantTwo().getTenantId()));

          shellPayload.setSpecificAssetIds(List.of(asset1,asset2,asset3));
          performShellCreateRequest(mapper.writeValueAsString(shellPayload));

          // Request with TenantId (TENANT_TWO) returns one shell with extend visibility of shell-properties, because tenantId is included in the specificAssetIds
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].description[*]").isNotEmpty())
                .andExpect(jsonPath("$.result[0].idShort",is(shellPayload.getIdShort())))
                .andExpect(jsonPath("$.result[0].id",is( shellPayload.getId() )))
                .andExpect(jsonPath("$.result[0].submodelDescriptors[*]").exists())
                .andExpect(jsonPath("$.result[0].specificAssetIds[*]").exists());

          // Request with TenantId (TENANT_THREE) returns one shell with only public visible shell-properties
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SHELL_BASE_PATH)
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantThree().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result", hasSize(1)))
                .andExpect(jsonPath("$.result[0].description[*]").doesNotExist())
                .andExpect(jsonPath("$.result[0].idShort").doesNotExist())
                .andExpect(jsonPath("$.result[0].id",is( shellPayload.getId() )))
                .andExpect(jsonPath("$.result[0].submodelDescriptors[*]").exists())
                .andExpect(jsonPath("$.result[0].specificAssetIds[*]").exists());
       }

       @Test
       public void testGetShellByExternalIdByOwningTenantId() throws Exception {
          AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
          shellPayload.setId(UUID.randomUUID().toString());
          List<SpecificAssetId> shellpayloadSpecificAssetIDs = shellPayload.getSpecificAssetIds();
          // Make all specificAssetIds to closed with externalSubjectId==null.
          shellpayloadSpecificAssetIDs.forEach( specificAssetId -> specificAssetId.setExternalSubjectId( null ) );
          shellPayload.setSpecificAssetIds( shellpayloadSpecificAssetIDs );

          performShellCreateRequest(mapper.writeValueAsString(shellPayload));


          // Request with owner TenantId (TENANT_ONE) returns one shell
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SINGLE_SHELL_BASE_PATH, getEncodedValue( shellPayload.getId() ))
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantOne().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description[*]").isNotEmpty())
                .andExpect(jsonPath("$.idShort",is(shellPayload.getIdShort())))
                .andExpect(jsonPath("$.globalAssetId",is(shellPayload.getGlobalAssetId())));

          // Request with TenantId (TENANT_TWO) returns no shell, because the shell not includes the externalSubjectId of Tenant_two as specificId
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SINGLE_SHELL_BASE_PATH, getEncodedValue( shellPayload.getId() ))
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
       }

       @Test
       public void testGetAllShellByExternalIdWithPublicAccessByTenantId() throws Exception {
          // the keyPrefix ensures that this test can run against a persistent database multiple times
          String keyPrefix = UUID.randomUUID().toString();
          AssetAdministrationShellDescriptor shellPayload = TestUtil.createCompleteAasDescriptor();
          shellPayload.setSpecificAssetIds(null);
          shellPayload.setId(UUID.randomUUID().toString());

          // asset1 is only visible for the owner because the externalSubjectId = null
          SpecificAssetId asset1 = TestUtil.createSpecificAssetId(keyPrefix + "defaultClosed","value_1",null);
          // asset2 is visible for everyone, because externalSubjectId = PUBLIC_READABLE and specificAssetKey is manufacturerPartId (which is in the list of allowedTypes via application.yml)
          SpecificAssetId asset2 = TestUtil.createSpecificAssetId("manufacturerPartId","value_2",List.of(getExternalSubjectIdWildcardPrefix()));
          // asset3 is visible for tenantTwo, because externalSubjectId = tenantTwo
          SpecificAssetId asset3 = TestUtil.createSpecificAssetId("tenantTwo","value_2_public",List.of(jwtTokenFactory.tenantTwo().getTenantId()));

          shellPayload.setSpecificAssetIds(List.of(asset1,asset2,asset3));
          performShellCreateRequest(mapper.writeValueAsString(shellPayload));

          // Request with TenantId (TENANT_TWO) returns one shell with extend visibility of shell-properties, because tenantId is included in the specificAssetIds
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SINGLE_SHELL_BASE_PATH, getEncodedValue( shellPayload.getId() ))
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantTwo().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description[*]").isNotEmpty())
                .andExpect(jsonPath("$.globalAssetId",is(shellPayload.getGlobalAssetId())))
                .andExpect(jsonPath("$.idShort",is(shellPayload.getIdShort())))
                .andExpect(jsonPath("$.id",is( shellPayload.getId() )))
                .andExpect(jsonPath("$.submodelDescriptors[*]").exists())
                .andExpect(jsonPath("$.specificAssetIds[*]").exists());

          // Request with TenantId (TENANT_THREE) returns one shell with only public visible shell-properties
          mvc.perform(
                      MockMvcRequestBuilders
                            .get(SINGLE_SHELL_BASE_PATH, getEncodedValue( shellPayload.getId() ))
                            .header( EXTERNAL_SUBJECT_ID_HEADER, jwtTokenFactory.tenantThree().getTenantId() )
                            .queryParam("pageSize", "100")
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwtTokenFactory.allRoles())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description[*]").doesNotExist())
                .andExpect(jsonPath("$.idShort").doesNotExist())
                .andExpect(jsonPath("$.globalAssetId").doesNotExist())
                .andExpect(jsonPath("$.id",is( shellPayload.getId() )))
                .andExpect(jsonPath("$.submodelDescriptors[*]").exists())
                .andExpect(jsonPath("$.specificAssetIds[*]").exists());
       }
   }
   @Nested
   @DisplayName( "Description Authentication Tests" )
   class DescriptionApiTest {

      @Test
      public void testGetDescriptionOnlyDeleteRoleExpectForbidden() throws Exception {
         mvc.perform(
                     MockMvcRequestBuilders
                           .get( "/api/v3.0/description" )
                           .accept( MediaType.APPLICATION_JSON )
                           .with( jwtTokenFactory.deleteTwin() )
               )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isForbidden());
      }

      @Test
      public void testGetDescriptionNoRoleExpectForbidden() throws Exception {
         mvc.perform(
                     MockMvcRequestBuilders
                           .get( "/api/v3.0/description" )
                           .accept( MediaType.APPLICATION_JSON )
                           .with( jwtTokenFactory.withoutRoles() )
               )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isForbidden());
      }

      @Test
      public void testGetDescriptionReadRoleExpectSuccess() throws Exception {
         mvc.perform(
                     MockMvcRequestBuilders
                           .get( "/api/v3.0/description" )
                           .accept( MediaType.APPLICATION_JSON )
                           .with( jwtTokenFactory.readTwin() )
               )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isOk());
      }

      @Test
      public void testGetDescriptionReadRoleExpectUnauthorized() throws Exception {
         mvc.perform(
                     MockMvcRequestBuilders
                           .get( "/api/v3.0/description" )
                           .accept( MediaType.APPLICATION_JSON )
               )
               .andDo( MockMvcResultHandlers.print() )
               .andExpect(status().isUnauthorized());
      }
   }
}
