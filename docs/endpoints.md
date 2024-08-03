# User Routes

## Create User

**Endpoint**: `POST /auth/users`

**Authentication**: Not required

**Description**: This endpoint creates a new user with a given username and password.

### Request Body
```json
{
  "username": "string",
  "password": "string"
}
```

# Order Routes

## Get All Orders

**Endpoint**: `GET /orders`

**Description**: This endpoint retrieves all orders for the authenticated user.

**Authentication**: (Common user) Required HTTP Header `Authorization: Bearer <token>`

### Response
- **200 OK**: A list of orders for the authenticated user.
- **403 Forbidden**: The user is not authenticated properly.

### Example Response
```json
[
  {
    "id": "c56a4180-65aa-42ec-a945-5fd21dec0538",
    "pid": "d84bba2e-7f6a-4a3b-93aa-3a69d5c1c798",
    "items": {
      "item1-id": 2,
      "item2-id": 1
    },
    "total": 150.00
  },
  {
    "id": "f87a6541-3b34-4c2e-95a8-34567c5a2e1a",
    "pid": "e75bba2f-9e7a-4c3b-93aa-3a69d5c1c798",
    "items": {
      "item3-id": 3
    },
    "total": 90.00
  }
]
```

## Get Order by ID

**Endpoint:** GET `/orders/{orderId}`

**Description:** This endpoint retrieves a specific order by its ID for the authenticated user.

**Authentication**: (Common user)Required HTTP Header `Authorization: Bearer <token>`

### Response
- **200 OK**: The details of the requested order.
- **403 Forbidden**: The user is not authenticated or authorized to access this endpoint.
- **404 Not Found**: The order with the specified ID was not found.

### Example Response
```json
{
  "id": "c56a4180-65aa-42ec-a945-5fd21dec0538",
  "pid": "d84bba2e-7f6a-4a3b-93aa-3a69d5c1c798",
  "items": {
    "item1-id": 2,
    "item2-id": 1
  },
  "total": 150.00
}
```

# Item Routes

## Get All Items

**Endpoint**: `GET /items`

**Description**: This endpoint retrieves all items or filters items by brand if the `brand` query parameter is provided.

### Query Parameters
- `brand` (optional): The brand to filter items by.

### Response
- **200 OK**: A list of items, optionally filtered by brand.

### Example Response
```json
[
  {
    "id": "c56a4180-65aa-42ec-a945-5fd21dec0538",
    "name": "Item Name",
    "description": "Item Description",
    "price": 50.00,
    "brand": {
      "id": "b45c4d1f-31f7-46a9-b5b9-23b1d8e8a82e",
      "name": "LUC Brand"
    },
    "category": {
      "id": "d1b1c3e1-13f4-4cfa-8e6f-32f5f8a9c1e9",
      "name": "Shoes"
    }
  },
  {
    "id": "f87a6541-3b34-4c2e-95a8-34567c5a2e1a",
    "name": "Another Item",
    "description": "Another Description",
    "price": 30.00,
    "brand": {
      "id": "b45c4d1f-31f7-46a9-b5b9-23b1d8e8a82e",
      "name": "LUC Brand"
    },
    "category": {
      "id": "d1b1c3e1-13f4-4cfa-8e6f-32f5f8a9c1e9",
      "name": "Shoes"
    }
  }
]
```

## Get Item by ID

**Endpoint**: GET `/items/{itemId}`

**Description**: This endpoint retrieves a specific item by its ID.

### Response
- **200 OK**: The details of the requested item.
- **404 Not Found**: The item with the specified ID was not found.

### Example Response
```json
{
  "id": "c56a4180-65aa-42ec-a945-5fd21dec0538",
  "name": "Item Name",
  "description": "Item Description",
  "price": 50.00,
  "brand": {
    "id": "b45c4d1f-31f7-46a9-b5b9-23b1d8e8a82e",
    "name": "LUC Brand"
  },
  "category": {
    "id": "d1b1c3e1-13f4-4cfa-8e6f-32f5f8a9c1e9",
    "name": "Shoes"
  }
}
```

# Checkout Routes

## Checkout

**Endpoint**: `POST /checkout`

**Description**: This endpoint processes the checkout for the authenticated user's cart using the provided card details.

**Authentication**: (Common user) Required HTTP Header `Authorization: Bearer <token>`

### Request Body
```json
{
  "name": "Cardholder Name",
  "number": 1234567812345678,
  "expiration": 1225,
  "cvv": 123
}
```

### Response
- **201 Created**: The checkout was successfully processed.
- **400 Bad Request**: Various errors related to empty cart, payment issues, or order issues.
- **404 Not Found**: The cart for the user was not found.
- **403 Forbidden**: The user is not authenticated or authorized to access this endpoint.

# Category Routes

## Get All Categories

**Endpoint**: `GET /categories`

**Description**: This endpoint retrieves all categories.

### Response
- **200 OK**: A list of categories.

### Example Response
```json
[
  {
    "uuid": "c56a4180-65aa-42ec-a945-5fd21dec0538",
    "name": "Electronics"
  },
  {
    "uuid": "d84bba2e-7f6a-4a3b-93aa-3a69d5c1c798",
    "name": "Books"
  }
]
```

# Cart Routes

## Get Shopping Cart

**Endpoint**: `GET /cart`

**Description**: This endpoint retrieves the authenticated user's shopping cart.

**Authentication**: (Common user) Required HTTP Header `Authorization: Bearer <token>`

### Response
- **200 OK**: The user's shopping cart.
- **403 Forbidden**: The user is not authenticated or authorized to access this endpoint.

### Example Response
```json
{
  "items": {
    "c56a4180-65aa-42ec-a945-5fd21dec0538": 2,
    "d84bba2e-7f6a-4a3b-93aa-3a69d5c1c798": 1
  }
}
```

## Add Items to the Cart

**Endpoint**: `POST /cart`

**Description**: This endpoint adds items to the authenticated user's shopping cart.

**Authentication**: (Common user) Required HTTP Header `Authorization: Bearer <token>`

### Request Body
```json
{
  "items": {
    "c56a4180-65aa-42ec-a945-5fd21dec0538": 2,
    "d84bba2e-7f6a-4a3b-93aa-3a69d5c1c798": 1
  }
}
```
## Modify Items in the Cart

**Endpoint**: `PUT /cart`

**Description**: This endpoint modifies items in the authenticated user's shopping cart.

**Authentication**: (Common user) Required HTTP Header `Authorization: Bearer <token>`

### Request Body
```json
{
  "items": {
    "c56a4180-65aa-42ec-a945-5fd21dec0538": 3,
    "d84bba2e-7f6a-4a3b-93aa-3a69d5c1c798": 2
  }
}
```

## Remove Item from the Cart

**Endpoint**: `DELETE /cart/{itemId}`

**Description**: This endpoint removes an item from the authenticated user's shopping cart.

**Authentication**: (Common user) Required HTTP Header `Authorization: Bearer <token>`

### Path Parameters
- `itemId`: UUID of the item to remove.

### Response
- **204 No Content**: The item was successfully removed from the cart.
- **403 Forbidden**: The user is not authenticated or authorized to access this endpoint.

# Brand Routes

## Get All Brands

**Endpoint**: `GET /brands`

**Description**: This endpoint retrieves all brands.

### Response
- **200 OK**: A list of brands.

# Login Routes

## Login

**Endpoint**: `POST /auth/login`

**Description**: This endpoint authenticates a user and returns a token if the login is successful.

### Request Body
```json
{
  "username": "string",
  "password": "string"
}
```

# Logout Routes

## Logout

**Endpoint**: `POST /auth/logout`

**Description**: This endpoint logs out the authenticated user by invalidating their token.

**Authentication**: (Common user) Required HTTP Header `Authorization: Bearer <token>`

### Response
- **204 No Content**: The user was successfully logged out.
- **403 Forbidden**: The user is not authenticated or authorized to access this endpoint.


# Admin Brand Routes

## Create Brand

**Endpoint**: `POST /brands`

**Description**: This endpoint allows an admin user to create a new brand.

**Authentication**: (Admin user) Required HTTP Header `Authorization: Bearer <token>`

### Request Body
```json
"LUC Brand"
```

# Admin - Category Routes

## Create Category

**Endpoint**: `POST /categories`

**Description**: This endpoint allows an admin user to create a new category.

**Authentication**: (Admin user) Required HTTP Header `Authorization: Bearer <token>`

### Request Body
```json
"string"
```

# Admin Item Routes

## Create Item

**Endpoint**: `POST /items`

**Description**: This endpoint allows an admin user to create a new item.

**Authentication**: (Admin user) Required HTTP Header `Authorization: Bearer <token>`

### Request Body
```json
{
  "name": "Item Name",
  "description": "Item Description",
  "price": "10.50",
  "brandId": "b1a1c3e1-13f4-4cfa-8e6f-32f5f8a9c1e9",
  "categoryId": "b1a1c3e1-13f4-4cfa-8e6f-32f5f8a9c1e9"
}
```

## Update Item

**Endpoint**: `PUT /items`

**Description**: This endpoint allows an admin user to update the price of an existing item.

**Authentication**: (Admin user) Required HTTP Header `Authorization: Bearer <token>`

### Request Body
```json
{
  "id": "d84bba2e-7f6a-4a3b-93aa-3a69d5c1c798",
  "price": "15.00"
}
```
