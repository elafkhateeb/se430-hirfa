CASES = {
    1: [
        ("Valid partition", "Name, valid Jordanian phone, unused email, strong password", "Account is created"),
        ("Duplicate email", "Email already belongs to an account", "Registration is rejected"),
        ("Invalid phone", "Phone number does not match the Jordanian mobile format", "Registration is rejected"),
    ],
    2: [
        ("Valid partition", "Registered email and matching password", "Customer is signed in"),
        ("Invalid password", "Registered email and incorrect password", "Sign-in is rejected"),
        ("Unknown account", "Email address is not registered", "Sign-in is rejected"),
    ],
    3: [
        ("Valid partition", "Name, phone, unique email, strong password, approved craft", "Artisan account is created"),
        ("Invalid craft", "Craft is not in the approved list", "Registration is rejected"),
        ("Weak password", "Password is shorter than 8 characters or has no digit", "Registration is rejected"),
    ],
    4: [
        ("Lower price boundary", "Price = 0.01 JOD; stock = 0", "Product is accepted"),
        ("Upper limits", "Price = 1000 JOD; stock = 500", "Product is accepted"),
        ("Out of range", "Price = 0 or stock = 501", "Product is rejected"),
    ],
    5: [
        ("Valid partition", "Stock = 10; restock by 5", "Stock becomes 15"),
        ("Zero quantity boundary", "Restock by 0", "Request is rejected"),
        ("Upper stock boundary", "Stock = 499; restock by 2", "Request is rejected"),
    ],
    6: [
        ("Craft filter", "Select one approved craft", "Only matching products are returned"),
        ("Maximum price boundary", "Maximum = 10 JOD", "Products costing 10 JOD or less are returned"),
        ("No matches", "Select a craft and price with no matching products", "An empty result is returned"),
    ],
    7: [
        ("Lower quantity boundary", "Add 1 item when stock is available", "Item is added"),
        ("Upper quantity boundary", "Add 5 items when stock is at least 5", "Item is added"),
        ("Invalid quantity", "Add 0 or 6 items", "Cart is unchanged"),
    ],
    8: [
        ("Valid checkout", "Signed-in customer, non-empty cart, supported payment", "Order is created and stock is reserved"),
        ("Empty cart", "Signed-in customer has no cart items", "Order is not created"),
        ("Unsupported payment", "Payment method is not supported", "Order is not created"),
    ],
    9: [
        ("Distance band", "Subtotal below 100 JOD; distance = 7 km", "Fee for the 5–15 km band is charged"),
        ("Free-delivery boundary", "Subtotal = 100 JOD", "Fee is 0 JOD"),
        ("Maximum distance", "Distance beyond the supported maximum", "Fee is capped at 10 JOD"),
    ],
    10: [
        ("Eligible HIRFA10", "Subtotal = 10 JOD; code = HIRFA10", "10% discount is applied"),
        ("Below minimum", "Subtotal = 9.99 JOD; code = HIRFA10", "Code is rejected"),
        ("Unknown code", "Enter an unsupported promotion code", "No discount is applied"),
    ],
    11: [
        ("Cash payment", "Cash order total = 500 JOD", "Payment is accepted"),
        ("Card boundary", "Card number contains exactly 16 digits", "Card payment is accepted"),
        ("Insufficient wallet", "Wallet balance is less than the order total", "Payment is rejected"),
    ],
    12: [
        ("Within cancellation window", "New order age = 119 minutes", "Order is cancelled"),
        ("Cancellation boundary", "New order age = 120 minutes", "Order may still be cancelled"),
        ("Outside cancellation window", "New order age = 121 minutes", "Cancellation is rejected"),
    ],
    13: [
        ("Valid transition", "New order advances once", "Status becomes Preparing"),
        ("Terminal state", "Delivered order advances", "Status remains Delivered"),
        ("Cancelled state", "Cancelled order advances", "Status remains Cancelled"),
    ],
    14: [
        ("Lower rating boundary", "Submit 1 star", "Rating is recorded"),
        ("Upper rating boundary", "Submit 5 stars", "Rating is recorded"),
        ("Invalid rating", "Submit 0 or 6 stars", "Rating is rejected"),
    ],
    15: [
        ("Lower top-up boundary", "Top up by 1 JOD", "Balance increases by 1 JOD"),
        ("Upper top-up boundary", "Top up by 1000 JOD", "Balance increases by 1000 JOD"),
        ("Out of range", "Top up by 0 or 1001 JOD", "Balance is unchanged"),
    ],
    16: [
        ("Minimum boundary", "Cart subtotal = 5 JOD", "Checkout is allowed"),
        ("Below minimum", "Cart subtotal = 4.99 JOD", "Order is rejected"),
        ("Empty cart", "Cart has no items", "Order is rejected"),
    ],
    17: [
        ("Valid quantity", "Change quantity to 3; stock = 5", "Cart quantity becomes 3"),
        ("Per-item limit", "Change quantity to 6", "Change is rejected"),
        ("Stock limit", "Change quantity to 4; stock = 3", "Change is rejected"),
    ],
    18: [
        ("Standard estimate", "Distance = 5 km; peak time = false", "Base preparation and travel time are included"),
        ("High-load estimate", "Distance = 5 km; current load = 16", "45-minute high-load allowance is added"),
        ("Long-distance estimate", "Distance = 31 km", "60-minute long-distance allowance is added"),
    ],
    19: [
        ("Valid report", "Existing artisan and start date before end date", "Order count and revenue are returned"),
        ("Empty period", "Valid date range with no matching orders", "Report indicates no matching orders"),
        ("Invalid period", "Start date is after end date", "Report request is rejected"),
    ],
    20: [
        ("Base-tier boundary", "Completed orders = 0", "Base tier is returned"),
        ("Tier threshold", "Completed orders equal the next-tier threshold", "Next tier and discount are returned"),
        ("Negative count", "Completed-order count is negative", "Base tier is returned"),
    ],
}
