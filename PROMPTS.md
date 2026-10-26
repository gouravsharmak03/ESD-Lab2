# PROMPTS.md — AI Prompt Log

Record every prompt you send to the AI **starting from Slice 2**.  
Slice 1 is done manually — no AI. See `README.md` for the prompt template structure.

> **Rules:**
> - One prompt (+ follow-ups) per slice
> - Use the structured template from the README — free-form prompts are not allowed
> - Do not back-fill this file after the lab

---

## Example — How a good S1 prompt looks (for reference only, S1 is done manually)

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout:
  com.esd.lab2
  ├── config/       SecurityConfig.java, AdminSeeder.java
  ├── controller/   CourseController.java
  ├── service/      CourseService.java
  ├── dto/          CourseRequest.java, CourseResponse.java
  ├── entity/       User.java, Role.java (enum), Course.java
  ├── repository/   UserRepository.java, CourseRepository.java
  ├── exception/    ApiError.java, GlobalExceptionHandler.java,
  │                 DuplicateResourceException.java, InvalidCredentialsException.java,
  │                 ResourceNotFoundException.java
  └── security/     JsonAuthenticationEntryPoint.java, JsonAccessDeniedHandler.java


TASK
Implement only POST /api/auth/signup.
Request: SignupRequest { email (valid email, required), password (required, 8–64 chars),
         fullName (required, not blank, max 100) }.
Response: 201 with UserResponse { id, email, fullName, role, createdAt }.
The new user's role is always STUDENT, even if the request contains a role field.
Store email trimmed and lowercased. Hash the password with the existing PasswordEncoder bean.
If the email already exists, throw DuplicateResourceException.

ALLOWED TO CREATE OR CHANGE
dto/SignupRequest.java, dto/UserResponse.java,
service/AuthService.java (signup method only),
controller/AuthController.java (signup endpoint only)

RULES
- Do not create or modify any file not listed above.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Use DTOs for request and response. Never return an entity from a controller.
- Use jakarta.validation annotations for input validation.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

---



## Slice 2 — Login & JWT (S2)

**Prompt 1:**

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout:
com.esd.lab2
├── config/       SecurityConfig.java, AdminSeeder.java
├── controller/   AuthController.java
├── service/      AuthService.java
├── dto/          SignupRequest.java, UserResponse.java
├── entity/       User.java, Role.java (enum), Course.java
├── repository/   UserRepository.java, CourseRepository.java
├── exception/    ApiError.java, GlobalExceptionHandler.java,
│                 DuplicateResourceException.java, InvalidCredentialsException.java,
│                 ResourceNotFoundException.java
└── security/     JsonAuthenticationEntryPoint.java, JsonAccessDeniedHandler.java

Existing code you must use (do not rewrite it):
- AuthService.java: existing signup implementation, injected PasswordEncoder and UserRepository.
- AuthController.java: existing POST /api/auth/signup endpoint.
- User.java and Role.java: existing user entity and role enum.
- UserRepository.java: existing user lookup methods.
- Existing InvalidCredentialsException and GlobalExceptionHandler.
- Existing JWT dependencies and configuration in the project.

TASK
Implement only S2: user login and JWT issuing.

Contract:
- POST /api/auth/login is public.
- Request: LoginRequest { email: required, password: required }.
- Return HTTP 200 with LoginResponse { accessToken, tokenType: "Bearer", expiresIn }.
- Normalize email by trimming and lowercasing before lookup.
- Verify the password using passwordEncoder.matches().
- For unknown email or incorrect password, throw InvalidCredentialsException with exactly the same message: "Invalid email or password".
- JWT claims: sub = user's email, role = user's role, jti = random UUID, iat = issued-at time, exp = expiration time.
- Sign the token using HS256.
- expiresIn must use the configured expiration value, not a hardcoded value.

ALLOWED TO CREATE OR CHANGE
- dto/LoginRequest.java
- dto/LoginResponse.java
- security/JwtService.java — token generation only; do not implement parsing/validation yet.
- service/AuthService.java — add login(LoginRequest); preserve signup.
- controller/AuthController.java — add POST /api/auth/login; preserve signup.

RULES
- Do not create or modify any file not listed above.
- Do not modify SecurityConfig.java or create a JWT filter; those belong to S3.
- Use existing exceptions. Do not create new error classes.
- Use DTOs for request and response. Never return an entity from a controller.
- Use jakarta.validation annotations for request validation.
- Read the JWT secret from @Value("${jwt.secret}"); do not hardcode secrets.
- Use JJWT 0.12 APIs: subject(), claim(), id(), issuedAt(), expiration(), signWith(key).
- Do not use deprecated JWT APIs, including setSubject, setExpiration, parserBuilder(), or signWith(SignatureAlgorithm.HS256, secret).
- Generate jti using a random UUID, not the user ID.
- Never include the password or password hash in JWT claims.
- If anything is unclear, add a TODO instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

**Follow-up (if any):**

```
(paste here)
```


---


---


## Slice 3 — JWT Filter & Protected API (S3)

**Prompt 1:**

```text
CONTEXT

This is the ESD Lab 2 Spring Boot authentication project.

Technology:
- Spring Boot 3.2.5
- Java 17
- Spring Security 6
- PostgreSQL
- JJWT 0.12.6
- Maven

Follow the existing project structure, naming conventions, exception
handling, and DTOs. Do not add any dependencies.

S1 and S2 have already been implemented.

Existing relevant components include:
- config/SecurityConfig.java
- controller/AuthController.java
- controller/CourseController.java, if present
- service/AuthService.java
- dto/LoginRequest.java
- dto/LoginResponse.java
- dto/SignupRequest.java
- dto/UserResponse.java
- entity/User.java
- entity/Role.java
- security/JwtService.java
- existing authentication entry point and access-denied handler
- GlobalExceptionHandler and ApiError, if present

Inspect the actual project files before making changes. Do not assume
that every listed class exists or invent duplicate classes.

TASK

Implement only Slice 3: JWT validation, authentication filtering,
and the protected current-user API.

PRESERVE S1 AND S2

- Keep signup working at POST /api/auth/signup.
- Keep login working at POST /api/auth/login.
- Preserve the existing request and response DTOs.
- Preserve the existing signup and login behavior.
- Do not rewrite unrelated application code.

REQUIRED BEHAVIOR

1. Public endpoints:
   - POST /api/auth/signup
   - POST /api/auth/login
   - GET /api/courses

2. Protected endpoint:
   - GET /api/users/me

3. GET /api/users/me must:
   - Require a valid JWT in the Authorization header.
   - Accept the header format: Authorization: Bearer <token>.
   - Return HTTP 200 for a valid token.
   - Return a UserResponse DTO for the authenticated user.
   - Never expose the User entity or password hash.

4. Missing, malformed, invalid-signature, or expired tokens must
   not authenticate the request.

5. Unauthenticated access to GET /api/users/me must return HTTP 401
   using the project's existing ApiError JSON response format.

6. Keep GET /api/courses public without making POST /api/courses public.
   Do not accidentally allow all methods on /api/courses.

JWT VALIDATION

- Extend the existing JwtService rather than creating a duplicate
  JWT service.
- Use the same configured Base64 secret used by token generation.
- Read the secret through @Value("${jwt.secret}").
- Use JJWT 0.12.x APIs.
- Verify the token signature and expiry with the appropriate API:
  Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
- Read the subject and role from verified claims only.
- Handle malformed, expired, or invalid tokens safely.
- Never trust claims from a token whose signature has not been verified.
- Do not expose exception details or stack traces to clients.

AUTHENTICATION FILTER

- Implement a OncePerRequestFilter for JWT authentication.
- Extract the Bearer token from the Authorization header.
- Validate the token using JwtService.
- Build an authenticated Spring Security Authentication object.
- Set it in the SecurityContext only after successful validation.
- Map the role claim to a Spring Security authority with the ROLE_
  prefix, for example ROLE_STUDENT or ROLE_ADMIN.
- Do not write an HTTP response directly from the filter when token
  validation fails; clear the SecurityContext and continue the filter
  chain so Spring Security can handle the unauthenticated request.
- Ensure authentication is not retained from an invalid token.
- Register the filter in the appropriate position before
  UsernamePasswordAuthenticationFilter.

SECURITY CONFIGURATION

- Use Spring Security 6 lambda DSL.
- Use authorizeHttpRequests and requestMatchers.
- Permit only the three specified public endpoints:
  POST /api/auth/signup
  POST /api/auth/login
  GET /api/courses
- Require authentication for all other requests, including
  GET /api/users/me and POST /api/courses.
- Configure the existing authentication entry point and access-denied
  handler through exceptionHandling(...).
- Disable CSRF only in accordance with the existing assignment's
  stateless Bearer-token API design.
- Keep the application stateless.
- Do not introduce sessions or form login.

STRICT RESTRICTIONS

- Do not use WebSecurityConfigurerAdapter.
- Do not use antMatchers, authorizeRequests, or the old .and() style.
- Do not use Jwts.parserBuilder().
- Do not add a UserDetailsService.
- Do not add an AuthenticationManager.
- Do not add dependencies.
- Do not add or modify CORS configuration.
- Do not implement role restrictions for POST /api/courses in this
  slice; that belongs to S4.
- Do not implement logout or a JWT denylist; that belongs to S5.
- Do not modify unrelated files.
- Do not replace existing error handlers with new competing handlers.
- Do not change the signup or login API contracts.

ALLOWED FILES

First inspect the existing repository and identify the minimum files
needed for S3. Expected files may include:
- security/JwtService.java
- a JWT authentication filter under security/
- config/SecurityConfig.java
- a controller for GET /api/users/me
- any existing authentication handler that must be wired correctly

Do not modify files outside the S3 scope. If an additional file appears
necessary, explain why before changing it.

OUTPUT

1. List every file to be created or modified and explain why.
2. Provide the complete contents of each changed or created file.
3. Explain how token validation and SecurityContext authentication work.
4. Identify any assumptions based on the existing project.
5. Do not claim tests passed unless they were actually run.
```

**Follow-up (if any):**

```text
Review your implementation against every S3 requirement.

Check that:
- GET /api/courses works without a token.
- GET /api/users/me returns 401 without a token.
- GET /api/users/me returns 200 with a valid token.
- Invalid and expired tokens cannot authenticate.
- The filter clears the SecurityContext on token-validation failure
  and continues the filter chain.
- The existing authentication entry point produces the required
  ApiError response.
- Only the specified three endpoints are public.
- POST /api/courses remains protected.
- The code uses Spring Security 6 and JJWT 0.12.x APIs.
- No UserDetailsService, AuthenticationManager, new dependencies,
  CORS changes, or logout/denylist functionality was introduced.
- S1 and S2 behavior remains intact.

List any failures or unverified requirements honestly. Do not silently
rewrite unrelated files to fix them.
```

---

## Slice 4 — Role Restriction (S4)

**Prompt 1:**

```text
Implement S4: restrict POST /api/courses to ADMIN users only.

CONTEXT
- This project uses Spring Boot 3, Spring Security 6, and JWT bearer-token authentication.
- S3 JWT authentication and the public GET /api/courses endpoint are already implemented.
- The JWT authentication filter sets authorities using the ROLE_ prefix.
- The Role enum includes STUDENT and ADMIN.
- An AdminSeeder creates an ADMIN user using credentials configured through application.yml/environment variables.

REQUIREMENTS
- Only users with the ADMIN role may create courses through POST /api/courses.
- Unauthenticated POST /api/courses requests must return HTTP 401.
- Authenticated STUDENT requests to POST /api/courses must return HTTP 403.
- Authenticated ADMIN requests with a valid course payload must be able to create a course and receive HTTP 201.
- GET /api/courses must remain public.
- Keep the existing signup and login contracts unchanged.
- Preserve the existing JSON authentication and access-denied error responses.
- Use the existing role naming convention consistently: hasRole("ADMIN") matches ROLE_ADMIN.
- Keep the change minimal and do not modify unrelated functionality.
- Do not add dependencies, a UserDetailsService, or an AuthenticationManager.
- Do not implement logout or a JWT denylist; those belong to S5.
- Do not claim tests passed unless they were actually run.

ALLOWED FILES
- Expected implementation file: src/main/java/com/esd/lab2/config/SecurityConfig.java
- Inspect the existing project before editing.
- Do not modify other files unless necessary; explain any additional changes.

OUTPUT
1. List every file created or modified and explain why.
2. Explain how the role restriction works.
3. Test POST /api/courses without a token, with a STUDENT token, and with an ADMIN token.
4. Report test results honestly.
```

**Follow-up (if any):**

```text
Review the S4 implementation against every requirement.
Verify the 401, 403, and 201 outcomes; ensure GET /api/courses remains public; confirm that hasRole("ADMIN") matches ROLE_ADMIN and the rule appears before anyRequest().authenticated() if configured in SecurityConfig.
Check the actual diff and list any requirements that remain unverified. Do not claim the reconstructed prompt is the original prompt.
```

---

## Slice 5 — Logout *(stretch, S5)*

**Prompt 1:**

```
(paste your prompt here)
```

**Follow-up (if any):**

```
(paste here)
```
