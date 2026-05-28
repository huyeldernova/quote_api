# QuoteSend API — Spring Boot Backend

## Tech Stack
- **Spring Boot 3.3** · **Java 21**
- **PostgreSQL** — main database
- **Redis** — JWT blacklist (logout/token revocation)
- **Spring Security + OAuth2 Resource Server** — JWT auth (HS512)
- **AWS S3** — image uploads & PDF storage
- **OpenHTMLtoPDF** — server-side PDF generation
- **Spring Mail + Thymeleaf** — HTML email with PDF attachment
- **Nimbus JOSE JWT** — token signing/verification

---

## Setup

### 1. Prerequisites
- Java 21
- PostgreSQL 15+
- Redis 7+
- (Optional) AWS S3 bucket for image uploads

### 2. Database
```sql
CREATE DATABASE quotesend;
```
JPA `ddl-auto=update` will create all tables on first startup.

### 3. Configure `application.properties`
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/quotesend
spring.datasource.username=postgres
spring.datasource.password=your_password

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT — must be >= 64 characters for HS512
jwt.secret-key=your-super-secret-key-at-least-64-characters-long-for-hs512!!

# Email (Gmail example — use App Password)
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# AWS S3 (optional — image upload feature)
aws.s3.bucket-name=your-bucket
aws.s3.region=ap-southeast-1
aws.s3.access-key=YOUR_KEY
aws.s3.secret-key=YOUR_SECRET

# App base URL (used in tracking pixel links)
app.base-url=http://localhost:8080
```

### 4. Run
```bash
mvn spring-boot:run
```

---

## API Endpoints

### Auth — `/api/v1/auth/**` (public)
| Method | Path              | Description                          |
|--------|-------------------|--------------------------------------|
| POST   | `/auth/login`     | Login → `{ accessToken, refreshToken, user }` |
| POST   | `/auth/register`  | Register → same response             |
| POST   | `/auth/refresh`   | Refresh tokens → `{ accessToken, refreshToken }` |
| GET    | `/auth/me`        | Get current user profile             |
| PUT    | `/auth/me`        | Update name / company                |
| POST   | `/auth/logout`    | Blacklist access token in Redis      |

### Quotes — `/api/v1/quotes` (authenticated)
| Method | Path                      | Description                        |
|--------|---------------------------|------------------------------------|
| GET    | `/quotes`                 | List all quotes for current user   |
| POST   | `/quotes`                 | Create new quote                   |
| GET    | `/quotes/{id}`            | Get quote detail                   |
| PUT    | `/quotes/{id}`            | Update quote                       |
| DELETE | `/quotes/{id}`            | Delete quote                       |
| POST   | `/quotes/{id}/duplicate`  | Duplicate quote                    |
| GET    | `/quotes/{id}/pdf`        | Download PDF (blob)                |
| POST   | `/quotes/{id}/send`       | Send email with PDF attachment     |

### Emails — `/api/v1/emails` (authenticated)
| Method | Path             | Description           |
|--------|------------------|-----------------------|
| GET    | `/emails`        | All email logs        |
| GET    | `/emails/stats`  | Open/click stats      |
| GET    | `/emails/{id}`   | Single email log      |

### Images — `/api/v1/images` (authenticated)
| Method | Path              | Description                    |
|--------|-------------------|--------------------------------|
| POST   | `/images/upload`  | Upload image → `{ imageUrl }` |
| DELETE | `/images?url=...` | Delete image from S3           |

### Dashboard — `/api/v1/dashboard` (authenticated)
| Method | Path               | Description                        |
|--------|--------------------|------------------------------------|
| GET    | `/dashboard/stats` | Aggregate stats + recent data      |

### Tracking — `/track/**` (public, no auth)
| Method | Path                   | Description                              |
|--------|------------------------|------------------------------------------|
| GET    | `/track/open/{token}`  | Returns 1×1 GIF, marks email as opened  |
| GET    | `/track/click/{token}` | Records click, redirects to website     |

---

## Architecture Notes

### Price Calculation
Formula (matches FE exactly):
```
pricePerPerson = ceil((totalCost / (1 - margin/100)) / 50) * 50
totalAmount    = pricePerPerson × paxCount
```

### Quote Ownership
All quote endpoints verify ownership via JWT subject (userId).  
`findByIdAndUserId()` ensures users can only access their own quotes.

### Email Tracking Flow
1. `POST /quotes/{id}/send` → creates `EmailLog` with a UUID `trackingToken`
2. Backend sends HTML email with `<img src="/track/open/{token}"/>` pixel embedded
3. When client opens email → email client loads pixel → `GET /track/open/{token}`
4. Backend marks `opened=true`, updates `quote.status = VIEWED`

### Token Flow
- `accessToken` → HS512 JWT, 2h TTL, contains userId as `sub` + `authorities`
- `refreshToken` → HS512 JWT, 14d TTL, `type=REFRESH_TOKEN` claim
- Logout → access token JTI stored in Redis until expiry
- Refresh → old refresh token blacklisted, new pair issued

### DB Schema (auto-created by JPA)
```
users           → id(UUID), name, email, password, company
roles           → id(UUID), name
user_has_role   → user_id, role_id
quotes          → id(BIGINT), user_id, quote_number, ...all fields..., status
quote_costs     → id(BIGINT), quote_id, label, amount, sort_order
quote_days      → id(BIGINT), quote_id, day_number, location, ..., image_url
email_logs      → id(BIGINT), quote_id, to_email, tracking_token, opened, clicked, ...
```

---

## Development Tips

### Disable S3 in local dev
If you don't have AWS credentials, `ImageService` will throw on upload.  
You can comment out the `S3Config` bean or mock S3 with LocalStack.

### Disable email in local dev
Set `spring.mail.host=localhost` and run a local SMTP mock like MailHog:
```bash
docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog
```
Then `spring.mail.port=1025` (no auth needed).

### Seed initial role
On first run, the `USER` role is auto-created when the first user registers.

