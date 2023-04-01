# FP Shopping Cart

Example project using FP best practices following [Practical FP in Scala book](https://leanpub.com/pfp-scala)

#### Run locally
- Publish docker img locally: `sbt docker:publishLocal` - You can check it was published with `docker images | grep fp-cart`
- Run docker compose: `docker compose up`
- App should be running :)

#### Tests
`sbt test`

#### Configuration and env
This is a didactic example, test secrets are included in [docker-compose.yml](docker-compose.yml)

_In a real app, these secrets must NOT be published_

For Admin users, the following environment variables are needed:
- `FP_CART_JWT_SECRET_KEY`
- `FP_CART_JWT_CLAIM`
- `FP_CART_ADMIN_USER_TOKEN`

For access token (manipulation of the shopping cart):
- `FP_CART_ACCESS_TOKEN_SECRET_KEY`

For password encryption:
- `FP_CART_PASSWORD_SALT`

#### Payments client
The [payment request](src/main/scala/algebras/PaymentClient.scala) is just a GET to this [UUID generator API](https://www.uuidgenerator.net/api/version4)
that returns a random UUID simulating the real payment processing

#### HTTP endpoints
- `GET /brands`
- `POST /brands`
- `GET /categories`
- `POST /categories`
- `GET /items`
- `GET /items?brand=gibson`
- `POST /items`
- `PUT /items`
- `GET /cart`
- `POST /cart`
- `PUT /cart`
- `DELETE /cart/{itemId}`
- `GET /orders`
- `GET /orders/{orderId}`
- `POST /checkout`
- `POST /auth/users`
- `POST /auth/login`
- `POST /auth/logout`