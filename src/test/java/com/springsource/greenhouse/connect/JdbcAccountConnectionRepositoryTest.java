package com.springsource.greenhouse.connect;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.security.encrypt.SearchableStringEncryptor;
import org.springframework.security.encrypt.StringEncryptor;
import org.springframework.social.twitter.TwitterOperations;

import com.springsource.greenhouse.account.Account;
import com.springsource.greenhouse.account.AccountMapper;
import com.springsource.greenhouse.account.StubFileStorage;
import com.springsource.greenhouse.database.GreenhouseTestDatabaseBuilder;

public class JdbcAccountConnectionRepositoryTest {
	
	private EmbeddedDatabase db;

	private JdbcTemplate jdbcTemplate;

	private AccountProvider<TwitterOperations> accountProvider;

	private JdbcAccountProviderFactory providerFactory;

	@Before
	public void setup() {
		db = new GreenhouseTestDatabaseBuilder().member().connectedAccount().testData(getClass()).getDatabase();
		jdbcTemplate = new JdbcTemplate(db);
		StringEncryptor encryptor = new SearchableStringEncryptor("secret", "5b8bd7612cdab5ed");
		AccountMapper accountMapper = new AccountMapper(new StubFileStorage(), "http://localhost:8080/members/{profileKey}");
		providerFactory = new JdbcAccountProviderFactory(jdbcTemplate, encryptor, accountMapper);
		accountProvider = providerFactory.getAccountProvider(TwitterOperations.class);
	}

	@After
	public void destroy() {
		if (db != null) {
			db.shutdown();
		}
	}

	@Test
	public void connect() {
		assertFalse(accountProvider.isConnected(2L));
		accountProvider.addConnection(2L, "accessToken", "kdonald");
		assertTrue(accountProvider.isConnected(2L));
	}
	
	@Test
	public void connected() {
		assertTrue(accountProvider.isConnected(1L));
	}

	@Test
	public void notConnected() {
		assertFalse(accountProvider.isConnected(2L));
	}

	@Test
	public void getApi() {
		TwitterOperations api = accountProvider.getApi(1L);
		assertNotNull(api);
	}

	@Test
	public void getApiNotConnected() {
		TwitterOperations api = accountProvider.getApi(2L);
		assertNotNull(api);
	}

	@Test
	public void getProviderAccountId() {
		assertEquals("habuma", accountProvider.getProviderAccountId(1L));
	}

	@Test
	public void getProviderAccountIdNotConnected() {
		assertNull(accountProvider.getProviderAccountId(2L));
	}
	
	@Test
	public void findConnectedAccount() throws Exception {
		assertNotNull(accountProvider.findAccountByConnection("345678901"));
	}

	@Test(expected = NoSuchAccountConnectionException.class)
	public void connectedAccountNotFound() throws Exception {
		accountProvider.findAccountByConnection("badtoken");
	}
	
	@Test
	public void findFriendAccounts() throws Exception {
		List<Account> accounts = accountProvider.findAccountsWithProviderAccountIds(asList("habuma", "rclarkson", "BarakObama"));
		assertEquals(2, accounts.size());
	}

	@Test
	public void disconnect() {
		assertTrue(accountProvider.isConnected(1L));
		accountProvider.disconnect(1L);
		assertFalse(accountProvider.isConnected(1L));
	}
	
}