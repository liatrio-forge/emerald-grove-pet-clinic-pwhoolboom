/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * Shared test security configuration for {@code @WebMvcTest} slices.
 *
 * <p>
 * Activating {@code @EnableWebSecurity} registers the
 * {@code SecurityExpressionHandler<FilterInvocation>} bean that the Thymeleaf Spring
 * Security dialect ({@code sec:authorize}) requires.
 *
 * <p>
 * A {@link MockMvcBuilderCustomizer} applies {@code springSecurity()} so that
 * {@code @WithMockUser} and {@code @WithAnonymousUser} annotations propagate correctly
 * through the real security filter chain.
 *
 * <p>
 * The filter chain allows all non-API requests and requires authentication for
 * {@code /api/**}, returning 401 (not 302) for unauthenticated API calls. CSRF is
 * enabled; tests that submit forms must use {@code .with(csrf())}.
 *
 * <p>
 * Usage: annotate the test class with {@code @Import(WebMvcTestSecurityConfig.class)}.
 */
@TestConfiguration
@EnableWebSecurity
public class WebMvcTestSecurityConfig {

	@Bean
	SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/**").authenticated().anyRequest().permitAll())
			.exceptionHandling(ex -> ex.authenticationEntryPoint(
					(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)));
		return http.build();
	}

	@Bean
	MockMvcBuilderCustomizer securityMockMvcCustomizer() {
		return new MockMvcBuilderCustomizer() {
			@Override
			public void customize(ConfigurableMockMvcBuilder<?> builder) {
				builder.apply(SecurityMockMvcConfigurers.springSecurity());
			}
		};
	}

}
