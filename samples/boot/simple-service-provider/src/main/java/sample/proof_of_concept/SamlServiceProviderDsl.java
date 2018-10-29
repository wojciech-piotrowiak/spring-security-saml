/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package sample.proof_of_concept;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.saml.SamlTemplateEngine;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.saved_for_later.SamlValidator;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;
import static org.springframework.util.Assert.notNull;

public class SamlServiceProviderDsl extends AbstractHttpConfigurer<SamlServiceProviderDsl, HttpSecurity> {

	public static SamlServiceProviderDsl serviceProvider() {
		return new SamlServiceProviderDsl();
	}

	private String prefix = "/saml/sp";
	private StaticServiceProviderResolver resolver;
	private SamlTransformer samlTransformer;
	private SamlValidator samlValidator;
	private SamlTemplateEngine samlTemplateEngine;

	@Override
	public void init(HttpSecurity builder) throws Exception {
		super.init(builder);
		notNull(prefix, "SAML path prefix must not be null");
		notNull(resolver, "Service Provider Resolver must not be null.");
		notNull(samlTransformer, "SAML transformer must not be null.");
		notNull(samlValidator, "SAML validator must not be null.");
		notNull(samlTemplateEngine, "SAML template engine must not be null.");
		String antPattern = "/" + stripSlashes(prefix);
		builder.antMatcher(antPattern + "/**")
			.csrf().disable()
			.authorizeRequests()
			.antMatchers("/**").permitAll();
	}

	@Override
	public void configure(HttpSecurity builder) throws Exception {
		super.configure(builder);


		SelectIdentityProviderUIFilter selectFilter = new SelectIdentityProviderUIFilter(samlTemplateEngine, resolver);
		selectFilter.setRedirectOnSingleProvider(false); //avoid redirect loop upon logout

		SamlAuthenticationRequestFilter authnFilter = new SamlAuthenticationRequestFilter(
			samlTemplateEngine,
			samlTransformer,
			resolver
		);

		SamlProcessAuthenticationResponseFilter authenticationFilter = new SamlProcessAuthenticationResponseFilter(
			samlTransformer, samlValidator, resolver
		);

		builder.addFilterAfter(selectFilter, BasicAuthenticationFilter.class);
		builder.addFilterAfter(authnFilter, selectFilter.getClass());
		builder.addFilterAfter(authenticationFilter, authnFilter.getClass());

	}

	public SamlServiceProviderDsl setSamlTransformer(SamlTransformer samlTransformer) {
		this.samlTransformer = samlTransformer;
		return this;
	}

	public SamlServiceProviderDsl setSamlValidator(SamlValidator samlValidator) {
		this.samlValidator = samlValidator;
		return this;
	}

	public SamlServiceProviderDsl setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public SamlServiceProviderDsl setServiceProviderResolver(StaticServiceProviderResolver resolver) {
		this.resolver = resolver;
		return this;
	}

	public SamlServiceProviderDsl setSamlTemplateEngine(SamlTemplateEngine samlTemplateEngine) {
		this.samlTemplateEngine = samlTemplateEngine;
		return this;
	}
}