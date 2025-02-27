/*******************************************************************************
 * Copyright (c) 2024 Robert Bosch Manufacturing Solutions GmbH and others
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
 *
 ******************************************************************************/
package org.eclipse.tractusx.semantics.accesscontrol.api.model;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SpecificAssetIdTest {

   public static Stream<Arguments> nullProvider() {
      return Stream.<Arguments> builder()
            .add( Arguments.of( null, null ) )
            .add( Arguments.of( "name", null ) )
            .add( Arguments.of( null, "value" ) )
            .build();
   }

   @ParameterizedTest
   @MethodSource( "nullProvider" )
   void testConstructorCalledWithNullExpectException( String name, String value ) {
      assertThatThrownBy( () -> new SpecificAssetId( name, value ) )
            .isExactlyInstanceOf( NullPointerException.class );
   }

   @Test
   void testConstructorCalledWithValidDataExpectSuccess() {
      final String name = "name";
      final String value = "value";

      SpecificAssetId actual = new SpecificAssetId( name, value );

      assertThat( actual.name() ).isEqualTo( name );
      assertThat( actual.value() ).isEqualTo( value );
   }

}