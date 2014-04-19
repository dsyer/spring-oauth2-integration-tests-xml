/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package sparklr.common;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.test.OAuth2ContextSetup;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.InMemoryApprovalStore;
import org.springframework.security.oauth2.provider.approval.JdbcApprovalStore;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import sparklr.common.AbstractIntegrationTests.TestConfiguration;

@SpringApplicationConfiguration(classes = TestConfiguration.class, inheritLocations = true)
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@IntegrationTest
public abstract class AbstractIntegrationTests implements PortHolder {

	private static String globalTokenPath;

	private static String globalAuthorizePath;

	@Rule
	public HttpTestUtils http = HttpTestUtils.standard().setPortHolder(this);

	@Rule
	public OAuth2ContextSetup context = OAuth2ContextSetup.standard(http);

	@Autowired
	private EmbeddedWebApplicationContext server;

	@Autowired(required=false)
	private TokenStore tokenStore;
	
	@Autowired(required=false)
	private ApprovalStore approvalStore;
	
	@Autowired(required=false)
	private DataSource dataSource;
	
	@Override
	public int getPort() {
		return server == null ? 8080 : server.getEmbeddedServletContainer().getPort();
	}

	@Before
	public void init() throws Exception {
		clear(tokenStore);
		clear(approvalStore);
	}

	private void clear(ApprovalStore approvalStore) throws Exception {
		if (approvalStore instanceof Advised) {
			Advised advised = (Advised) tokenStore;
			ApprovalStore target = (ApprovalStore) advised.getTargetSource().getTarget();
			clear(target);
			return;
		}
		if (approvalStore instanceof InMemoryApprovalStore) {
			((InMemoryApprovalStore) approvalStore).clear();
		}
		if (approvalStore instanceof JdbcApprovalStore) {
			JdbcTemplate template = new JdbcTemplate(dataSource);
			template.execute("delete from oauth_approvals");
		}
	}

	private void clear(TokenStore tokenStore) throws Exception {
		if (tokenStore instanceof Advised) {
			Advised advised = (Advised) tokenStore;
			TokenStore target = (TokenStore) advised.getTargetSource().getTarget();
			clear(target);
			return;
		}
		if (tokenStore instanceof InMemoryTokenStore) {
			((InMemoryTokenStore) tokenStore).clear();
		}
		if (tokenStore instanceof JdbcTokenStore) {
			JdbcTemplate template = new JdbcTemplate(dataSource);
			template.execute("delete from oauth_access_token");
			template.execute("delete from oauth_refresh_token");
			template.execute("delete from oauth_client_token");
			template.execute("delete from oauth_code");
		}
	}

	@Value("${oauth.paths.token:/oauth/token}")
	public void setTokenPath(String tokenPath) {
		globalTokenPath = tokenPath;
	}

	@Value("${oauth.paths.authorize:/oauth/authorize}")
	public void setAuthorizePath(String authorizePath) {
		globalAuthorizePath = authorizePath;
	}

	public static String tokenPath() {
		return globalTokenPath;
	}

	public static String authorizePath() {
		return globalAuthorizePath;
	}

	@Configuration
	@PropertySource(value = "classpath:test.properties", ignoreResourceNotFound = true)
	protected static class TestConfiguration {

	}

}