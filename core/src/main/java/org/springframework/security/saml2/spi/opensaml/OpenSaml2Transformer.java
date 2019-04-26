/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.security.saml2.spi.opensaml;

import java.time.Clock;

import org.springframework.security.saml2.spi.DefaultSaml2Transformer;
import org.springframework.security.saml2.spi.Saml2JavaAbstraction;

public class OpenSaml2Transformer extends DefaultSaml2Transformer {

	public OpenSaml2Transformer() {
		this(new OpenSaml2Implementation(Clock.systemUTC()).init());
	}

	public OpenSaml2Transformer(Saml2JavaAbstraction implementation) {
		super(implementation);
	}
}