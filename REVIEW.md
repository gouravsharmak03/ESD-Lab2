# REVIEW.md — AI Output Review Log

Complete **one review entry per slice** before moving to the next.  


---

## Slice 1 — Signup (S1)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `AuthController.signup()` | Controller | Yes |
| `AuthService.signup()` | Service | Yes |
| `AuthService.toResponse()` | Service | Yes |
| `SignupRequest` | DTO | Yes |
| `UserResponse` | DTO | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/signup` | PASS |
| Returns **201** | PASS |
| Response shape: `{ id, email, fullName, role, createdAt }` | PASS |
| No `passwordHash` field in response | PASS |
| Role in response is always `STUDENT` | PASS |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| `@Valid` on controller parameter | PASS |
| `@Email`, `@NotBlank`, `@Size` on DTO fields | PASS |
| Duplicate email throws `DuplicateResourceException` | PASS |
| No new error/response wrapper class invented | Verify against your changes |
| No stack trace or exception message leaked to client | Verify error handling |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Password hashed via injected `PasswordEncoder` (not `new BCryptPasswordEncoder()`) | PASS |
| `SignupRequest` has no `role` field, or it is ignored | PASS |
| Duplicate check uses normalised (lowercased/trimmed) email | PASS |
| AI did **not** touch `SecurityConfig`, add JWT class, or add `UserDetailsService` | PASS, if you did not introduce these during S1 |

### E. Understanding Questions

1. Where exactly is the password hashed, and why is the same password stored as a different string for two users?

   > The password is hashed in `AuthService` using `passwordEncoder.encode(request.password())`. BCrypt uses a random salt, so the same password can produce different stored hashes.

2. What would happen if `SignupRequest` accepted a `role` field and the AI used it?

   > A user could submit `ADMIN` as their role and gain unauthorized privileges if the server trusted that value. Signup should always assign `Role.STUDENT` on the server.

3. Which class turns `DuplicateResourceException` into a 409 response?

   > `GlobalExceptionHandler` handles `DuplicateResourceException` and returns HTTP 409 Conflict. Verify that this is the exact handler class name in your project.

### F. Decision

| File | Decision | Reason (if modified/rejected) |
|---|---|---|
| `SignupRequest.java` | Accepted / Modified | Added request validation annotations |
| `UserResponse.java` | Accepted / Modified | Confirmed response excludes password hash |
| `AuthService.java` | Accepted / Modified | Normalizes email, checks duplicates, hashes password, assigns STUDENT |
| `AuthController.java` | Accepted / Modified | Signup endpoint returns HTTP 201 |



---

#
# Slice 2 — Login & JWT (S2)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `LoginRequest.java` | DTO | Yes |
| `LoginResponse.java` | DTO | Yes |
| `JwtService.generateToken()` | Security / token generation | Yes |
| `AuthService.login()` | Service | Yes |
| `AuthService.signup()` | Service / preserved S1 | Yes |
| `AuthController.login()` | Controller | Yes |
| `AuthController.signup()` | Controller / preserved S1 | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/login` | PASS |
| Returns **200** | PASS |
| Response: `{ accessToken, tokenType: "Bearer", expiresIn }` | PASS |
| `expiresIn` matches configured value (not hardcoded) | PASS |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Same 401 message for unknown email and wrong password | PASS |
| No `try/catch` swallowing exceptions silently | PASS |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| JWT secret comes from `@Value`, no string literal secret | PASS |
| Uses jjwt 0.12 API: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, `.signWith(key)` | PASS |
| Rejects deprecated APIs: `setSubject`, `setExpiration`, `signWith(SignatureAlgorithm.HS256, secret)`, `Jwts.parserBuilder()` | PASS |
| `jti` is a random UUID (not the user id) | PASS |
| No password or hash in token claims | PASS |
| Parse/validate method not written yet (that is S3) | PASS |

### E. Understanding Questions

**1. Why must the error message be identical for "no such email" and "wrong password"?**

> To prevent user enumeration. If the error messages differed, an attacker could discover which email addresses are registered by attempting to log in.

**2. Who can read the claims inside your JWT? Who can change them?**

> Anyone who obtains the JWT can decode and read its claims because a standard signed JWT is encoded, not encrypted. Changing the claims invalidates the signature unless the attacker can generate a valid signature using the secret key.

**3. What does `jti` give us that `sub` doesn't? (Think about S5.)**

> The `sub` claim identifies the token's subject, which is the user's email. The `jti` claim uniquely identifies an individual token using a random UUID. This lets us track or revoke a particular token, for example by adding its ID to a denylist during logout in S5.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| `LoginRequest.java` | Accepted | Defines the email and password fields required for login. |
| `LoginResponse.java` | Accepted | Returns the access token, token type, and configured expiry. |
| `JwtService.java` | Accepted | Generates signed JWTs using the configured secret and expiry. |
| `AuthService.java` | Modified | Preserves signup and adds email normalization, password verification, and token generation. |
| `AuthController.java` | Modified | Preserves signup and adds `POST /api/auth/login`. |


---

## Slice 3 — JWT Filter & Protected API (S3)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `JwtService.validateToken()` | Security / token validation | Verify against S3 prompt |
| `JwtAuthenticationFilter.doFilterInternal()` | Security / JWT filter | Verify against S3 prompt |
| `SecurityConfig.filterChain()` | Security configuration | Verify against S3 prompt |
| `UserController.getCurrentUser()` | Controller / protected API | Verify against S3 prompt |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `GET /api/courses` works **without** token | PASS — returned HTTP 200 |
| `GET /api/users/me` returns **401** with no token | PASS — returned HTTP 401 with JSON error |
| `GET /api/users/me` returns **200** with valid token | PASS — returned HTTP 200 with user profile |
| 401 body is an `ApiError` JSON object | PASS — `JsonAuthenticationEntryPoint` creates and serializes an `ApiError` |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Filter does not write a response on error — continues chain | PASS — code inspection confirms the filter continues the chain after invalid-token handling |
| Filter clears SecurityContext on bad token | PASS — code inspection confirms `SecurityContextHolder.clearContext()` is called |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Spring Security 6 style: lambda DSL, `requestMatchers`, `authorizeHttpRequests` | PASS — verified in `SecurityConfig` |
| Rejected: `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()` | PASS — none used in the inspected `SecurityConfig` |
| `permitAll` covers only the three specific public endpoints | PASS — signup POST, login POST, and courses GET |
| `GET /api/courses` is public but `POST /api/courses` is NOT (watch for broad matcher) | PASS — GET returned 200 without token; POST returned 401 without token |
| Signature + expiry verified with `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | PASS — verified in `JwtService.validateToken()` |
| Authorities use `ROLE_` prefix | PASS — filter constructs `SimpleGrantedAuthority("ROLE_" + role)` |
| Entry point and access-denied handler wired via `exceptionHandling(...)` | PASS — verified in `SecurityConfig` |
| No `UserDetailsService`, `AuthenticationManager`, sessions or CORS added | PASS — source search found none; stateless session policy is configured |
| `GET /api/users/me` returns `UserResponse`, not the entity | PASS — verified in `UserController`; response contains no password hash |

### E. Understanding Questions

1. Why is CSRF protection disabled here, and when would disabling it be wrong?

   > CSRF protection is disabled because this API uses stateless Bearer-token authentication instead of relying on a server-side login session. Disabling CSRF can be unsafe if authentication relies on cookies that browsers automatically send, such as in a session-based web application.

2. Walk through what happens to a request with an expired token, step by step, until the client sees 401.

   > The JWT filter extracts the Bearer token and calls `JwtService.validateToken()`. The JWT parser verifies the signature and expiration. If the token has expired, parsing throws an exception. The filter catches the exception, clears the security context, and continues the filter chain. Since the request is not authenticated, Spring Security rejects access to the protected endpoint and invokes the configured `JsonAuthenticationEntryPoint`, which returns HTTP 401 with a JSON error.

3. Why don't 401/403 errors from the filter chain reach `GlobalExceptionHandler`?

   > Spring Security processes authentication and authorization failures in its filter chain, before controller-level exception handling. The configured `JsonAuthenticationEntryPoint` handles authentication failures (401), and `JsonAccessDeniedHandler` handles access-denied failures (403). These security handlers generate the responses directly instead of relying on `GlobalExceptionHandler`.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| `JwtService.java` | Modified | Adds JWT signature and expiration validation while preserving token generation. |
| `JwtAuthenticationFilter.java` | Added | Extracts Bearer tokens, validates claims, creates the authenticated security context, and clears the context for invalid tokens. |
| `SecurityConfig.java` | Modified | Configures stateless security, explicit public routes, JSON security handlers, and JWT filter placement. |
| `UserController.java` | Added | Provides the authenticated user's profile through `UserResponse` without exposing the entity or password hash. |

---

## Slice 4 — Role Restriction (S4)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `config/SecurityConfig.java` — `filterChain()` | S4 prompt not recorded in `PROMPTS.md` | Unverified — confirm against the original S4 prompt |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/courses` with no token → **401** | PASS — HTTP 401 verified |
| `POST /api/courses` with STUDENT token → **403** | PASS — HTTP 403 verified |
| `POST /api/courses` with ADMIN token → **201** | PASS — HTTP 201 verified; course created |
| 403 body is an `ApiError` JSON object | PASS — JSON body contains status 403, error Forbidden, message, path, and fieldErrors |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Role naming consistent: `hasRole("ADMIN")` ↔ authority `ROLE_ADMIN` | PASS — `JwtAuthenticationFilter` prefixes the token role with `ROLE_` |
| If `@PreAuthorize` used: `@EnableMethodSecurity` is present | N/A — restriction is configured in `SecurityConfig`, not with `@PreAuthorize` |
| Rule placed **before** `anyRequest().authenticated()` if in SecurityConfig | PASS — POST `/api/courses` ADMIN rule precedes `anyRequest()` |
| Diff is small — AI did not rewrite the whole config | Unverified — Git diff includes S3 changes as well as the S4 rule |

### E. Understanding Questions

1. A STUDENT calls `POST /api/courses`. Why does the server return 403 and not 401?

   > The STUDENT has already authenticated successfully using a valid JWT, so the server knows the user's identity. However, the user's `ROLE_STUDENT` authority does not satisfy `hasRole("ADMIN")`. The request is therefore forbidden (403), rather than unauthenticated (401).

2. Where does the role in the JWT come from, and can the client change it?

   > The role claim is added by `JwtService` when the server generates the JWT, using the user's role. A client cannot change the role claim and keep the token valid because the JWT signature would no longer match. The server verifies the signature when validating the token.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| `src/main/java/com/esd/lab2/config/SecurityConfig.java` | Keep | Adds an ADMIN-only rule for POST `/api/courses` before the catch-all authenticated rule; the three authorization outcomes were verified |

---

## Slice 5 — Logout (S5) *(stretch)*

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `TokenDenylistService.java` — `deny()`, `isDenied()` | S5 | Unverified — original allowed-file list unavailable |
| `JwtAuthenticationFilter.java` — `doFilterInternal()` | S5 | Unverified — original allowed-file list unavailable |
| `AuthController.java` — `logout()` | S5 | Unverified — original allowed-file list unavailable |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/auth/logout` returns **204** | PASS — HTTP 204 observed |
| Same token returns **401** after logout | PASS — HTTP 401 observed |
| Fresh login after logout returns a new working token | PASS — fresh login succeeded; protected endpoint returned HTTP 200 |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Logout adds entry to server-side denylist (not just tells client to delete token) | PASS — logout calls `tokenDenylistService.deny()` |
| Denylist keyed by **jti**, not email | PASS — uses `claims.getId()` as the map key |
| Map is `ConcurrentHashMap` (thread-safe) | PASS — implemented in `TokenDenylistService` |
| Logout endpoint is NOT under `permitAll` | PASS — existing security configuration requires authentication |
| Filter checks the denylist and treats denylisted jti as invalid | PASS — filter checks `tokenDenylistService.isDenied(tokenId)` |
| (Bonus) Expired entries are removed from the map | PASS — `isDenied()` removes expired entries |

### E. Understanding Questions

1. Why does logout need server-side state if JWT is "stateless"?

   > JWTs normally remain valid until they expire. A server-side denylist lets the application reject a specific token immediately after logout, even before its expiration time.

2. What happens to the denylist when the server restarts? What if there are two server instances?

   > The denylist is stored in memory, so its entries disappear when the server restarts. With two server instances, each instance has its own map, so a token denied on one instance might still be accepted by the other. A shared store would be needed to synchronize the denylist.

3. Why store the expiry time with each jti?

   > The token only needs to remain denied until it expires. Storing the expiry time allows expired entries to be removed from the map instead of keeping them indefinitely.

### F. Decision

| File | Decision | Reason |
|---|---|---|
| `TokenDenylistService.java` | Keep | Stores denied JWT IDs and their expiry times in a thread-safe map |
| `JwtAuthenticationFilter.java` | Keep | Checks the denylist and rejects logged-out tokens |
| `AuthController.java` | Keep | Adds the logout endpoint and records the current token in the denylist |

---

## No-AI Change Tasks



**Files changed:**

| File | What changed and why |
|---|---|
| `src/main/java/com/esd/lab2/security/TokenDenylistService.java` | Added an in-memory, thread-safe denylist keyed by JWT ID, with expiry-based cleanup |
| `src/main/java/com/esd/lab2/security/JwtAuthenticationFilter.java` | Added a denylist check so logged-out tokens cannot authenticate |
| `src/main/java/com/esd/lab2/controller/AuthController.java` | Added `POST /api/auth/logout` to deny the current token until it expires |

**Explanation (your own words):**
> I added a service to store logged-out JWT IDs and their expiration times. The authentication filter checks the denylist after validating a token. The logout endpoint adds the current token ID to the denylist. I tested logout and received HTTP 204, reused the same token and received HTTP 401, then logged in again and verified that the fresh token accessed the protected endpoint with HTTP 200. The denylist is stored in memory, so entries do not survive a server restart.``