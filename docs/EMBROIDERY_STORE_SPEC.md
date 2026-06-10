# Embroidery Store Specification

## Business Model

The store sells embroidery designs, not garments.

A design is the primary product.

Example:

Brand Of Sacrifice

Customers then choose:
- Garment Type
- Color
- Size

## Catalog Structure

CatalogGroup
→ Collection
→ Design
→ GarmentType
→ DesignGarment
→ DesignGarmentPrice
→ Color
→ Size
→ Inventory

## Catalog Groups

Examples:
- Anime
- Games
- Gym
- Kazakh
- Cars
- Minimal

## Collections

Anime
- Berserk
- Naruto

Games
- Cyberpunk
- Elden Ring

Cars
- JDM

## Garment Types

- T_SHIRT
- OVERSIZE_TSHIRT
- SWEATSHIRT
- HOODIE
- ZIP_HOODIE

## Prices

Currencies:
- KZT
- RUB
- USD
- EUR

No automatic conversion.

Prices are managed manually by admins.

## Inventory

Inventory must be stored by:

Design + GarmentType + Color + Size

## Reviews

Only customers who purchased a design may leave reviews.

## User Addresses

Users can store:
- First Name
- Last Name
- Phone
- Email
- Delivery Addresses

Orders must store address snapshots.

## Custom Design

Keep existing Custom Design module.

## Admin Requirements

Admin can:

- Create catalog groups
- Create collections
- Create designs
- Upload images
- Configure garment types
- Configure prices
- Configure colors
- Configure sizes
- Configure inventory
- Manage orders
- Manage custom embroidery requests