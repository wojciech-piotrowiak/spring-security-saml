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

package org.springframework.security.saml.serviceprovider;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.saml.SamlTemplateEngine;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.SamlValidator;
import org.springframework.security.saml.registration.HostedServiceProviderConfiguration;
import org.springframework.security.saml.serviceprovider.spi.DefaultServiceProviderResolver;
import org.springframework.security.saml.serviceprovider.filters.SamlAuthenticationRequestFilter;
import org.springframework.security.saml.serviceprovider.filters.SamlProcessAuthenticationResponseFilter;
import org.springframework.security.saml.serviceprovider.filters.SamlServiceProviderMetadataFilter;
import org.springframework.security.saml.serviceprovider.spi.SamlTemplateProcessor;
import org.springframework.security.saml.serviceprovider.filters.SelectIdentityProviderUIFilter;
import org.springframework.security.saml.serviceprovider.spi.ServiceProviderMetadataResolver;
import org.springframework.security.saml.serviceprovider.spi.ServiceProviderSamlValidator;
import org.springframework.security.saml.serviceprovider.spi.SingletonServiceProviderConfigurationResolver;
import org.springframework.security.saml.spi.VelocityTemplateEngine;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;
import static org.springframework.util.Assert.notNull;
import static org.springframework.util.StringUtils.hasText;

public class SamlServiceProviderConfigurer extends AbstractHttpConfigurer<SamlServiceProviderConfigurer, HttpSecurity> {

	public static SamlServiceProviderConfigurer serviceProvider() {
		SamlServiceProviderConfigurer configurer = new SamlServiceProviderConfigurer();
		//TODO - do we need to post process setters?
		//		configurer.postProcess(configurer);
		return configurer;
	}

	/*
	 * User required fields
	 */
	private SamlTransformer samlTransformer = null;

	/*
	 * Fields with implementation defaults
	 */
	private String prefix = "/saml/sp";
	private HostedServiceProviderConfiguration configuration;
	private SamlValidator samlValidator = null;
	private SamlTemplateEngine samlTemplateEngine = null;
	private AuthenticationManager authenticationManager = null;
	private ServiceProviderResolver resolver = null;
	private ServiceProviderConfigurationResolver configurationResolver;

	@Override
	public void init(HttpSecurity builder) throws Exception {
		notNull(prefix, "SAML path prefix must not be null.");
		notNull(samlTransformer, "SAML Core Transformer Implementation must not be null.");

		if (samlValidator == null) {
			samlValidator = new ServiceProviderSamlValidator(samlTransformer);
		}
		if (samlTemplateEngine == null) {
			samlTemplateEngine = new VelocityTemplateEngine(true);
		}

		if (configurationResolver == null) {
			notNull(configuration, "SAML Service Provider Configuration must not be null.");
			if (!hasText(configuration.getPrefix())) {
				configuration = HostedServiceProviderConfiguration.Builder.builder(configuration)
					.withPrefix(prefix)
					.build();
			}
			configurationResolver = new SingletonServiceProviderConfigurationResolver(configuration);
		}

		if (resolver == null) {
			ServiceProviderMetadataResolver serviceProviderMetadataResolver =
				new ServiceProviderMetadataResolver(samlTransformer);
			resolver = new DefaultServiceProviderResolver(serviceProviderMetadataResolver, configurationResolver);
		}

		String matchPrefix = "/" + stripSlashes(prefix);
		String samlPattern = matchPrefix + "/**";
		builder
			.csrf().ignoringAntMatchers(samlPattern)
			.and()
			.authorizeRequests()
			.antMatchers(samlPattern).permitAll()
		;

	}

	@Override
	public void configure(HttpSecurity builder) throws Exception {
		SamlTemplateProcessor template = new SamlTemplateProcessor(samlTemplateEngine);
		String matchPrefix = "/" + stripSlashes(prefix);

		SamlServiceProviderMetadataFilter metadataFilter = new SamlServiceProviderMetadataFilter(
			new AntPathRequestMatcher(matchPrefix + "/metadata/**"),
			samlTransformer,
			resolver
		);

		SelectIdentityProviderUIFilter selectFilter = new SelectIdentityProviderUIFilter(
			new AntPathRequestMatcher(matchPrefix + "/select/**"),
			resolver, template
		)
			.setRedirectOnSingleProvider(false); //avoid redirect loop upon logout

		SamlAuthenticationRequestFilter authnFilter = new SamlAuthenticationRequestFilter(
			new AntPathRequestMatcher(matchPrefix + "/discovery/**"),
			samlTransformer,
			resolver,
			template
		);

		SamlProcessAuthenticationResponseFilter authenticationFilter = new SamlProcessAuthenticationResponseFilter(
			new AntPathRequestMatcher(matchPrefix + "/SSO/**"),
			samlTransformer,
			samlValidator,
			resolver
		);
		if (authenticationManager != null) {
			authenticationFilter.setAuthenticationManager(authenticationManager);
		}

		builder.addFilterAfter(metadataFilter, BasicAuthenticationFilter.class);
		builder.addFilterAfter(selectFilter, metadataFilter.getClass());
		builder.addFilterAfter(authnFilter, selectFilter.getClass());
		builder.addFilterAfter(authenticationFilter, authnFilter.getClass());

	}


	public SamlServiceProviderConfigurer samlTransformer(SamlTransformer samlTransformer) {
		this.samlTransformer = samlTransformer;
		return this;
	}


	public SamlServiceProviderConfigurer samlValidator(SamlValidator samlValidator) {
		this.samlValidator = samlValidator;
		return this;
	}

	public SamlServiceProviderConfigurer prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}


	public SamlServiceProviderConfigurer serviceProviderResolver(ServiceProviderResolver resolver) {
		this.resolver = resolver;
		return this;
	}


	public SamlServiceProviderConfigurer samlTemplateEngine(SamlTemplateEngine samlTemplateEngine) {
		this.samlTemplateEngine = samlTemplateEngine;
		return this;
	}

	public SamlServiceProviderConfigurer authenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
		return this;
	}


	public SamlServiceProviderConfigurer configuration(HostedServiceProviderConfiguration configuration) {
		this.configuration = configuration;
		return this;
	}


	public SamlServiceProviderConfigurer providerResolver(ServiceProviderResolver resolver) {
		this.resolver = resolver;
		return this;
	}


	public SamlServiceProviderConfigurer configurationResolver(
		ServiceProviderConfigurationResolver configurationResolver
	) {
		this.configurationResolver = configurationResolver;
		return this;
	}
}