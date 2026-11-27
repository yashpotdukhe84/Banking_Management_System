package com.sprint.bank;

import com.sprint.bank.dto.LoginRequest;
import com.sprint.bank.dto.RegisterRequest;
import com.sprint.bank.security.JwtFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankingManagementSystemApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contextLoads() {
	}

	@Test
	void registerLoginAndLogoutFlowWorks() throws Exception {
		// 1. Register a new customer via REST API
		RegisterRequest register = new RegisterRequest();
		register.setEmail("rahul@example.com");
		register.setPassword("Password@123");
		register.setFullName("Rahul Sharma");
		register.setPhone("9876543210");
		register.setAddress("Mumbai, India");
		register.setPanNumber("ABCDE1234F");

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(register)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(201))
				.andExpect(jsonPath("$.message").value("Registration successful"))
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andExpect(jsonPath("$.data.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.data.fullName").value("Rahul Sharma"));

		// 2. Duplicate registration returns 409 Conflict
		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(register)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));

		// 3. Login with valid credentials
		LoginRequest login = new LoginRequest();
		login.setEmail("rahul@example.com");
		login.setPassword("Password@123");

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(login)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("Login successful"))
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andReturn();

		JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String token = loginJson.path("data").path("token").asText();

		// 4. Customer trying to access admin URL gets 403 Forbidden
		mockMvc.perform(get("/api/v1/admin/dashboard")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));

		// 5. Logout without token gets 401 Unauthorized
		mockMvc.perform(post("/api/v1/auth/logout"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		// 6. Logout with valid token succeeds
		mockMvc.perform(post("/api/v1/auth/logout")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("Logout successful"));

		// 7. Reusing logged-out token fails with 401 Unauthorized
		mockMvc.perform(post("/api/v1/auth/logout")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		// 8. Re-login issues a fresh working token
		MvcResult reloginResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(login)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andReturn();

		String newToken = objectMapper.readTree(reloginResult.getResponse().getContentAsString())
				.path("data").path("token").asText();

		mockMvc.perform(post("/api/v1/auth/logout")
						.header("Authorization", "Bearer " + newToken))
				.andExpect(status().isOk());
	}

	@Test
	void validationAndBadCredentialsWorkCleanly() throws Exception {
		// Invalid registration payload -> 400 Bad Request
		RegisterRequest invalid = new RegisterRequest();
		invalid.setEmail("bad-email");
		invalid.setPassword("123");

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalid)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));

		// Wrong password for seeded admin -> 401 Unauthorized
		LoginRequest wrongLogin = new LoginRequest();
		wrongLogin.setEmail("admin@bank.com");
		wrongLogin.setPassword("WrongPass@123");

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(wrongLogin)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		// Correct password for seeded admin -> 200 OK with ADMIN role
		LoginRequest adminLogin = new LoginRequest();
		adminLogin.setEmail("admin@bank.com");
		adminLogin.setPassword("Admin@123");

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(adminLogin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("ADMIN"));
	}

	@Test
	void thymeleafAuthPagesWorkEndToEnd() throws Exception {
		// 1. Login and Register pages render cleanly
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Bank Login")));

		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Customer Registration")));

		// 2. Unauthenticated dashboard request redirects to /login
		mockMvc.perform(get("/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		// 3. Register via Thymeleaf form (leaving optional PAN blank) -> sets AUTH_TOKEN cookie & redirects to /dashboard?registered=true
		MvcResult registerResult = mockMvc.perform(post("/register")
						.param("fullName", "Priya Verma")
						.param("email", "priya@example.com")
						.param("password", "Password@123")
						.param("phone", "9123456789")
						.param("address", "Delhi, India")
						.param("panNumber", ""))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dashboard?registered=true"))
				.andExpect(cookie().exists(JwtFilter.TOKEN_COOKIE_NAME))
				.andReturn();

		Cookie authCookie = registerResult.getResponse().getCookie(JwtFilter.TOKEN_COOKIE_NAME);
		assertNotNull(authCookie);

		// 4. Dashboard opens with valid cookie and shows Toastify script + user details
		mockMvc.perform(get("/dashboard?registered=true").cookie(authCookie))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Priya Verma")))
				.andExpect(content().string(containsString("priya@example.com")))
				.andExpect(content().string(containsString("Toastify")))
				.andExpect(content().string(containsString("Registration successful!")));

		// 5. Logout via Thymeleaf form clears cookie & redirects to /login?logout=true
		mockMvc.perform(post("/logout").cookie(authCookie))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login?logout=true"))
				.andExpect(cookie().maxAge(JwtFilter.TOKEN_COOKIE_NAME, 0));

		// 6. Login page with ?logout=true triggers logout toast notification
		mockMvc.perform(get("/login?logout=true"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Logged out successfully!")));

		// 7. Reusing the old cookie no longer accesses /dashboard
		mockMvc.perform(get("/dashboard").cookie(authCookie))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));

		// 8. Invalid login via Thymeleaf form stays on login page with error toast message
		mockMvc.perform(post("/login")
						.param("email", "priya@example.com")
						.param("password", "WrongPass@123"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Invalid email or password")));

		// 9. Valid login via Thymeleaf form succeeds and redirects to /dashboard?login=true
		MvcResult loginResult = mockMvc.perform(post("/login")
						.param("email", "priya@example.com")
						.param("password", "Password@123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/dashboard?login=true"))
				.andExpect(cookie().exists(JwtFilter.TOKEN_COOKIE_NAME))
				.andReturn();

		Cookie freshCookie = loginResult.getResponse().getCookie(JwtFilter.TOKEN_COOKIE_NAME);
		assertNotNull(freshCookie);

		mockMvc.perform(get("/dashboard?login=true").cookie(freshCookie))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Priya Verma")))
				.andExpect(content().string(containsString("Login successful!")));
	}
}
