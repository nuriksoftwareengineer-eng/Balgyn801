# Project Overview

This project is an embroidery design e-commerce platform built with Spring Boot.

## Critical Business Rule

This is NOT a traditional clothing store.

We sell embroidery designs.

The design is the product.

Customers select:

- garment type
- color
- size

inside a design page.

Never model garments as standalone products.

Wrong:

- Brand Of Sacrifice Hoodie
- Brand Of Sacrifice T-Shirt

Correct:

Design:
- Brand Of Sacrifice

Variants:
- Hoodie
- T-Shirt
- Sweatshirt

## Existing Modules

The following modules already exist and should be preserved whenever possible:

- JWT Authentication
- Security Configuration
- Users
- Roles
- Refresh Tokens
- Orders
- Order History
- Payments
- Delivery
- Custom Design
- Admin API

Do not rewrite working modules without strong justification.

Prefer extending existing code instead of replacing it.

## Development Rules

1. Analyze existing entities first.
2. Analyze relationships before coding.
3. Reuse existing code whenever possible.
4. Show implementation plan before major changes.
5. Minimize breaking changes.
6. Generate Flyway migrations for DB changes.

Never perform large architectural rewrites without approval.