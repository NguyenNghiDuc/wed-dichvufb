# Issue #26 — legacy order form and mobile regression

The legacy request form binds four allowed UI fields back to the existing `order` model on validation errors. HTML escaping prevents attacker-controlled notes or names from becoming active markup.

The creation service remains the authority for owner and fixed price. Spoofed `pricePerUnit`, `totalPrice`, `ownerUsername` and `id` input do not influence the newly created order because it constructs an independent entity.

The six-column customer-only legacy list is horizontally scrollable on mobile. MockMvc regression covers invalid quantity, form preservation, XSS encoding, forged owner/price, authentication and CSRF. No external posting or payment.
