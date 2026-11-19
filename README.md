# Hirfa — Handmade Delivery Platform

Hirfa is a Java web application that connects customers with Jordanian artisans selling handmade goods. It includes a SQLite catalog, customer and artisan accounts, shopping cart and checkout flows, wallet payments, order management, and artisan product tools.

**GitHub:** [elafkhateeb/se430-hirfa](https://github.com/elafkhateeb/se430-hirfa)

## Features

- Browse products and filter by craft or maximum price.
- Register and sign in as a customer or artisan.
- Add products to a customer cart, review the cart, and check out.
- Pay with cash on delivery, card, or wallet; apply supported promotional codes.
- Review order history, wallet balance, delivery estimates, and artisan profiles.
- Let artisans add products and restock only products belonging to their account.
- Rate artisans and generate artisan sales reports.
- Store application data in SQLite.

## Requirements

- JDK 17 or later
- The SQLite JDBC, JUnit, SLF4J, and JaCoCo libraries included in `lib/`

No external build tool is required.

## Run the website

From the repository root:

```bash
mkdir -p build
javac -cp lib/sqlite-jdbc.jar -d build $(find src -name '*.java')
java -cp "build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" com.hirfa.web.WebServer handmade.db 8080
```

Open [http://localhost:8080](http://localhost:8080). The database and initial product catalog are created automatically on first run.

### Main pages

| Page | Purpose |
|---|---|
| `/products` | Browse, filter, and add available products to a cart |
| `/register` | Create a customer or artisan account |
| `/login` | Sign in using the selected account type |
| `/cart` | Review the cart and continue shopping |
| `/order` | Choose delivery and payment details and place an order |
| `/wallet` | View or top up a customer wallet |
| `/orders` | View the signed-in customer's order history |
| `/artisan/dashboard` | Add or restock products owned by the signed-in artisan |
| `/artisan` | Browse artisan profiles and ratings |
| `/admin` | View artisan sales reports |

Artisan registration requires a name, Jordanian mobile number, email, password, and craft. After registration, the artisan is signed in and can manage products from the dashboard. Customers and artisans use separate account types at sign-in.

## Run the tests

```bash
mkdir -p build
javac -cp "lib/sqlite-jdbc.jar:lib/junit-console.jar" -d build $(find src test -name '*.java')
java -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" \
  org.junit.platform.console.ConsoleLauncher --scan-classpath --details=tree
```

The JUnit 5 suite covers customer and artisan accounts, product and cart rules, checkout and payment, order lifecycle, delivery fees, discounts, reports, wallet, and loyalty behavior.

## Generate coverage

```bash
java -javaagent:lib/jacocoagent.jar=destfile=coverage/jacoco.exec \
  -cp "lib/junit-console.jar:build:lib/sqlite-jdbc.jar:lib/slf4j-api.jar" \
  org.junit.platform.console.ConsoleLauncher --scan-classpath

java -jar lib/jacococli.jar report coverage/jacoco.exec \
  --classfiles build/com/hirfa/service --classfiles build/com/hirfa/model \
  --classfiles build/com/hirfa/repo --classfiles build/com/hirfa/web \
  --sourcefiles src --html coverage/html --csv coverage/summary.csv
```

The HTML report is written to `coverage/html/index.html`.

## Project layout

```text
src/com/hirfa/model/       Domain models
src/com/hirfa/repo/        SQLite persistence
src/com/hirfa/service/     Business rules
src/com/hirfa/web/         HTTP server and web pages
test/com/hirfa/            JUnit 5 tests
features/                  Gherkin requirements (R01–R20)
requirements-and-tests.html
requirements-and-tests.pdf English requirements and test-design report
lib/                       Bundled runtime and test libraries
```

## Requirements and testing documentation

The project documents 20 functional requirements using Gherkin scenarios, black-box equivalence partitioning and boundary-value cases, and JUnit tests for the service layer. The requirements and test report is available as [HTML](requirements-and-tests.html) and [PDF](requirements-and-tests.pdf).

Regenerate the HTML report and standalone Gherkin files after editing the requirement or test-case data:

```bash
python3 gen_doc.py
```

## Team

| Member | Focus areas |
|---|---|
| Elaf Alkhatib | Artisan requirements, delivery fees, wallet and loyalty, requirements documentation |
| Dana Haddad | Persistence and models, cart, payments, order lifecycle, artisan reports |
| Raghed Ananbeh | Products, discounts, delivery estimates, service setup |
| Alaa Alomari | Customer registration and login, checkout, web interface, test and coverage documentation |

## Traceability

| Requirements | Main implementation | JUnit tests |
|---|---|---|
| 1–2 | `CustomerService` | `CustomerServiceTest` |
| 3, 14 | `ArtisanService` | `ArtisanServiceTest` |
| 4–6 | `ProductService` | `ProductServiceTest` |
| 7, 17 | `CartService` | `CartServiceTest` |
| 8, 16 | `OrderService` | `OrderServiceTest` |
| 9 | `DeliveryFeeCalculator` | `DeliveryFeeCalculatorTest` |
| 10 | `DiscountCalculator` | `DiscountCalculatorTest` |
| 11 | `PaymentService` | `PaymentServiceTest` |
| 12–13 | `OrderLifecycleService` | `OrderLifecycleServiceTest` |
| 15, 20 | `WalletService`, `LoyaltyService` | `WalletAndLoyaltyTest` |
| 18 | `EtaCalculator` | `EtaCalculatorTest` |
| 19 | `ReportService` | `ReportServiceTest` |
