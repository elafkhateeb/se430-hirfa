REQS = [
    dict(n=1, title="Customer registration", fn="CustomerService.register", test="CustomerServiceTest",
         desc="A customer can register with a name, Jordanian mobile number, unique email address, and strong password.",
         bdd="""Feature: Customer registration

Scenario: Register with valid details
  Given a visitor enters "Nour Ayada", "0771112223", "nour@example.com", and "NourPass2026"
  When the customer registration form is submitted
  Then a customer account is created
  And the new customer is signed in

Scenario: Reject an email address already in use
  Given an account already uses "amal@example.com"
  When a visitor registers with the same email address
  Then registration is rejected with a duplicate-email message
"""),
    dict(n=2, title="Customer login", fn="CustomerService.login", test="CustomerServiceTest",
         desc="A registered customer can sign in with the email address and password used at registration.",
         bdd="""Feature: Customer login

Scenario: Sign in with valid customer credentials
  Given a customer account exists for "amal@example.com"
  When the customer submits the correct password
  Then the customer is signed in
  And customer navigation is displayed

Scenario: Reject an incorrect password
  Given a customer account exists for "amal@example.com"
  When the customer submits an incorrect password
  Then sign-in is rejected
"""),
    dict(n=3, title="Artisan registration and account", fn="ArtisanService.register", test="ArtisanServiceTest",
         desc="An artisan creates an account with contact details, a unique email address, a strong password, and an approved craft.",
         bdd="""Feature: Artisan registration and account

Scenario: Register a complete artisan account
  Given an artisan enters valid contact details and selects "Leather"
  When the artisan registration form is submitted
  Then an artisan account is created
  And the artisan is signed in to the product dashboard

Scenario: Reject an unapproved craft
  Given "Printing" is not an approved craft
  When an artisan submits that craft
  Then the account is not created
"""),
    dict(n=4, title="Add a product", fn="ProductService.add", test="ProductServiceTest",
         desc="An artisan adds a named product in an approved craft with a price from 0.01 to 1000 JOD and stock from 0 to 500.",
         bdd="""Feature: Add a handmade product

Scenario: Add a valid product
  Given an artisan enters "Copper Ring", a price of 12.50 JOD, and stock of 10
  When the artisan submits the product
  Then the product is saved in the artisan's craft
  And the product appears in the storefront

Scenario: Reject a product with a zero price
  Given an artisan enters a price of 0 JOD
  When the artisan submits the product
  Then the product is not saved
"""),
    dict(n=5, title="Restock a product", fn="ProductService.restock", test="ProductServiceTest",
         desc="An artisan can add a positive quantity to a product's stock without exceeding the 500-item limit.",
         bdd="""Feature: Restock a product

Scenario: Restock an available product
  Given an artisan owns a product with 4 items in stock
  When the artisan adds 6 items
  Then the stock becomes 10

Scenario: Reject restocking above the stock limit
  Given a product has 499 items in stock
  When the artisan adds 2 items
  Then the stock remains unchanged
"""),
    dict(n=6, title="Search the catalog", fn="ProductService.search", test="ProductServiceTest",
         desc="A visitor can filter products by an approved craft, a maximum price, or both.",
         bdd="""Feature: Search the product catalog

Scenario: Filter products by craft
  Given the catalog contains products in several crafts
  When a visitor selects "Soap"
  Then only soap products are displayed

Scenario: Filter products by maximum price
  Given the catalog contains products at different prices
  When a visitor sets the maximum price to 10 JOD
  Then products above 10 JOD are excluded
"""),
    dict(n=7, title="Add a product to the cart", fn="CartService.addItem", test="CartServiceTest",
         desc="A customer can add an in-stock product in a quantity from 1 to 5, subject to available stock.",
         bdd="""Feature: Add a product to the cart

Scenario: Add an in-stock item
  Given a product has 12 items in stock
  When a customer adds 2 items to the cart
  Then the cart contains 2 of that product

Scenario: Reject a quantity above the per-product limit
  Given a product has 12 items in stock
  When a customer tries to add 6 items
  Then the cart is unchanged
"""),
    dict(n=8, title="Place an order", fn="OrderService.place", test="OrderServiceTest",
         desc="A signed-in customer can place a non-empty order using a supported payment method and valid delivery details.",
         bdd="""Feature: Place an order

Scenario: Place an order with valid details
  Given a signed-in customer has a valid cart and delivery distance
  When the customer confirms the order with cash on delivery
  Then the order is stored with its items and calculated total
  And the purchased stock is reserved

Scenario: Reject an empty cart
  Given a signed-in customer has an empty cart
  When the customer confirms checkout
  Then no order is created
"""),
    dict(n=9, title="Calculate delivery fees", fn="DeliveryFeeCalculator.fee", test="DeliveryFeeCalculatorTest",
         desc="Delivery fees depend on distance, with free delivery for qualifying orders and a maximum fee.",
         bdd="""Feature: Calculate delivery fees

Scenario: Apply a distance-based fee
  Given an order subtotal is below the free-delivery threshold
  When the delivery distance is 7 kilometers
  Then the fee for the matching distance band is charged

Scenario: Waive the fee for a qualifying order
  Given an order subtotal is at least 100 JOD
  When delivery fees are calculated
  Then the delivery fee is 0 JOD
"""),
    dict(n=10, title="Apply a promotion", fn="DiscountCalculator.apply", test="DiscountCalculatorTest",
         desc="Supported promotion codes apply their documented percentage discounts and minimum subtotal rules.",
         bdd="""Feature: Apply a promotion

Scenario: Apply HIRFA10 to an eligible subtotal
  Given the subtotal is at least 10 JOD
  When the customer enters "HIRFA10"
  Then a 10 percent discount is applied

Scenario: Reject HIRFA10 below its minimum
  Given the subtotal is below 10 JOD
  When the customer enters "HIRFA10"
  Then no discount is applied and an eligibility message is shown
"""),
    dict(n=11, title="Process payment", fn="PaymentService.pay", test="PaymentServiceTest",
         desc="Cash, a valid 16-digit card, or a customer wallet with enough funds can pay for an order.",
         bdd="""Feature: Process payment

Scenario: Pay with a valid card
  Given a customer has a valid card number
  When the customer submits a card payment
  Then the payment is confirmed

Scenario: Reject an invalid card number
  Given a customer submits a card number with fewer than 16 digits
  When the payment is processed
  Then the payment is rejected
"""),
    dict(n=12, title="Cancel an order", fn="OrderLifecycleService.cancel", test="OrderLifecycleServiceTest",
         desc="A new order can be cancelled within two hours; wallet-funded amounts are refunded.",
         bdd="""Feature: Cancel an order

Scenario: Cancel a new order within two hours
  Given an order is new and was placed 30 minutes ago
  When the customer cancels the order
  Then the order is cancelled
  And any wallet-funded amount is refunded

Scenario: Reject cancellation after two hours
  Given an order was placed more than two hours ago
  When the customer cancels the order
  Then the order remains unchanged
"""),
    dict(n=13, title="Advance an order status", fn="OrderLifecycleService.advance", test="OrderLifecycleServiceTest",
         desc="Order states follow the supported lifecycle and terminal states cannot advance.",
         bdd="""Feature: Advance an order status

Scenario: Advance to the next valid state
  Given an order is in the new state
  When the order advances
  Then its state becomes preparing

Scenario: Do not advance a terminal order
  Given an order is delivered
  When the order advances
  Then its state remains delivered
"""),
    dict(n=14, title="Rate an artisan", fn="ArtisanService.rateArtisan", test="ArtisanServiceTest",
         desc="A customer can submit a rating from 1 to 5 stars and the artisan's average is updated.",
         bdd="""Feature: Rate an artisan

Scenario: Submit a valid rating
  Given an artisan profile is available
  When a customer submits 5 stars
  Then the rating count and average are updated

Scenario: Reject a rating outside the supported range
  Given an artisan profile is available
  When a customer submits 6 stars
  Then the rating is rejected
"""),
    dict(n=15, title="Top up a wallet", fn="WalletService.topUp", test="WalletAndLoyaltyTest",
         desc="A customer can top up a wallet by an amount from 1 to 1000 JOD.",
         bdd="""Feature: Top up a wallet

Scenario: Add a valid top-up
  Given a customer wallet has a balance of 20 JOD
  When the customer adds 25 JOD
  Then the wallet balance becomes 45 JOD

Scenario: Reject an amount above the limit
  Given a customer requests a top-up of 1001 JOD
  When the top-up is processed
  Then the wallet balance is unchanged
"""),
    dict(n=16, title="Enforce the minimum order", fn="OrderService.place", test="OrderServiceTest",
         desc="An order subtotal must meet the 5 JOD minimum before fees and discounts.",
         bdd="""Feature: Enforce the minimum order

Scenario: Accept an order at the minimum
  Given a customer cart has a subtotal of 5 JOD
  When the customer confirms the order
  Then checkout may continue

Scenario: Reject an order below the minimum
  Given a customer cart has a subtotal of 4.99 JOD
  When the customer confirms the order
  Then no order is created
"""),
    dict(n=17, title="Change a cart quantity", fn="CartService.changeQty", test="CartServiceTest",
         desc="A customer can change a cart quantity within the supported per-product limit and current stock.",
         bdd="""Feature: Change a cart quantity

Scenario: Update a quantity within stock
  Given a cart contains 1 item and 8 are in stock
  When the customer changes the quantity to 4
  Then the cart contains 4 items

Scenario: Reject a quantity above current stock
  Given a cart contains 1 item and 3 are in stock
  When the customer changes the quantity to 4
  Then the cart remains unchanged
"""),
    dict(n=18, title="Estimate delivery time", fn="EtaCalculator.estimate", test="EtaCalculatorTest",
         desc="The estimated delivery time is based on a fixed preparation time, distance, and applicable long-distance or high-load additions.",
         bdd="""Feature: Estimate delivery time

Scenario: Estimate a standard delivery
  Given the delivery distance is 5 kilometers and the current load is 10 orders
  When the estimate is calculated
  Then the estimate includes preparation and travel time

Scenario: Add the high-load allowance
  Given the delivery distance is 5 kilometers and the current load is 16 orders
  When the estimate is calculated
  Then the high-load allowance is included
"""),
    dict(n=19, title="Report artisan sales", fn="ReportService.artisanReport", test="ReportServiceTest",
         desc="A report summarizes orders and revenue for an artisan during a valid date range.",
         bdd="""Feature: Report artisan sales

Scenario: Report sales for a valid period
  Given an artisan has completed orders in the selected period
  When a report is requested for that artisan and period
  Then the report includes order count and revenue

Scenario: Reject an invalid date range
  Given the report start date is after its end date
  When the report is requested
  Then an invalid-period result is returned
"""),
    dict(n=20, title="Calculate customer loyalty tier", fn="LoyaltyService.tierFor", test="WalletAndLoyaltyTest",
         desc="A customer's loyalty tier and associated discount are determined by the number of completed orders.",
         bdd="""Feature: Calculate customer loyalty tier

Scenario: Qualify for a higher tier
  Given a customer has completed enough orders for the next tier
  When the loyalty tier is calculated
  Then the customer receives the matching tier and discount

Scenario: Keep the base tier below the threshold
  Given a customer has fewer completed orders than the first threshold
  When the loyalty tier is calculated
  Then the customer remains in the base tier
"""),
]
