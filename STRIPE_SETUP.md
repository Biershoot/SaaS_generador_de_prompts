# Stripe Integration Setup Guide

This guide will help you set up Stripe payments for your Prompt Generator SaaS application.

## Prerequisites

1. A Stripe account (sign up at https://stripe.com)
2. Your Spring Boot application running
3. MySQL database configured

## Step 1: Stripe Dashboard Setup

### 1.1 Get Your API Keys

1. Log in to your Stripe Dashboard
2. Go to **Developers > API keys**
3. Copy your **Publishable key** and **Secret key**
4. Make sure you're in **Test mode** for development

### 1.2 Create Products and Prices

1. Go to **Products** in your Stripe Dashboard
2. Create the following products:

#### Free Plan
- Name: Free Plan
- Price: $0/month
- Note: This doesn't need a Stripe price ID

#### Premium Plan
- Name: Premium Plan
- Price: $9.99/month
- Billing: Recurring
- Copy the **Price ID** (starts with `price_`)

#### Pro Plan
- Name: Pro Plan
- Price: $19.99/month
- Billing: Recurring
- Copy the **Price ID** (starts with `price_`)

### 1.3 Set Up Webhooks

1. Go to **Developers > Webhooks**
2. Click **Add endpoint**
3. Set the endpoint URL to: `https://your-domain.com/api/webhooks/stripe`
4. Select the following events:
   - `checkout.session.completed`
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
5. Copy the **Webhook signing secret** (starts with `whsec_`)

## Step 2: Environment Configuration

### 2.1 Update Environment Variables

Add the following environment variables to your system or `.env` file:

```bash
STRIPE_SECRET_KEY=sk_test_your_secret_key_here
STRIPE_PUBLISHABLE_KEY=pk_test_your_publishable_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here
```

### 2.2 Update Price IDs

Update the price IDs in `SubscriptionService.java` with your actual Stripe price IDs:

```java
createPlan("premium", "Premium", "Enhanced features with more prompts", 
           "price_your_premium_price_id", 9.99, "USD", "monthly", 100, true, false),
createPlan("pro", "Pro", "Unlimited access with priority support", 
           "price_your_pro_price_id", 19.99, "USD", "monthly", -1, true, true)
```

## Step 3: Database Migration

Run the database migration to add Stripe fields:

```bash
mvn flyway:migrate
```

This will add the following columns to the `subscriptions` table:
- `stripe_subscription_id`
- `stripe_customer_id`
- `stripe_price_id`
- `created_at`
- `updated_at`

## Step 4: Testing the Integration

### 4.1 Test Checkout Session Creation

```bash
curl -X POST http://localhost:8080/api/payments/create-checkout-session \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "priceId": "price_your_premium_price_id",
    "successUrl": "http://localhost:3000/success",
    "cancelUrl": "http://localhost:3000/cancel"
  }'
```

### 4.2 Test Available Plans

```bash
curl -X GET http://localhost:8080/api/payments/plans \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4.3 Test Subscription Status

```bash
curl -X GET http://localhost:8080/api/subscriptions/my-subscription \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Step 5: Frontend Integration

### 5.1 Install Stripe.js

```html
<script src="https://js.stripe.com/v3/"></script>
```

### 5.2 Create Checkout Session

```javascript
// Create checkout session
const response = await fetch('/api/payments/create-checkout-session', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    priceId: 'price_your_premium_price_id',
    successUrl: 'http://localhost:3000/success',
    cancelUrl: 'http://localhost:3000/cancel'
  })
});

const { sessionUrl } = await response.json();

// Redirect to Stripe Checkout
window.location.href = sessionUrl;
```

### 5.3 Handle Success/Cancel

```javascript
// Success page
const urlParams = new URLSearchParams(window.location.search);
const sessionId = urlParams.get('session_id');

if (sessionId) {
  // Payment successful, update UI
  console.log('Payment successful!');
}

// Cancel page
// Handle cancellation
console.log('Payment cancelled');
```

## Step 6: Production Deployment

### 6.1 Update to Live Keys

1. Switch to **Live mode** in Stripe Dashboard
2. Update environment variables with live keys:
   - `STRIPE_SECRET_KEY=sk_live_...`
   - `STRIPE_PUBLISHABLE_KEY=pk_live_...`
   - `STRIPE_WEBHOOK_SECRET=whsec_...`

### 6.2 Update Webhook URL

Update the webhook endpoint URL to your production domain:
`https://your-production-domain.com/api/webhooks/stripe`

### 6.3 SSL Certificate

Ensure your production server has a valid SSL certificate, as Stripe requires HTTPS for webhooks.

## API Endpoints

### Payment Endpoints

- `POST /api/payments/create-checkout-session` - Create Stripe checkout session
- `GET /api/payments/plans` - Get available subscription plans

### Subscription Endpoints

- `GET /api/subscriptions/my-subscription` - Get current user's subscription
- `GET /api/subscriptions/can-create-prompt` - Check if user can create prompts
- `GET /api/subscriptions/features` - Get subscription features

### Webhook Endpoint

- `POST /api/webhooks/stripe` - Handle Stripe webhook events

## Subscription Plans

### Free Plan
- Price: $0/month
- Prompt limit: 10 prompts
- Features: Basic access

### Premium Plan
- Price: $9.99/month
- Prompt limit: 100 prompts
- Features: Custom prompts, enhanced access

### Pro Plan
- Price: $19.99/month
- Prompt limit: Unlimited
- Features: Custom prompts, priority support

## Troubleshooting

### Common Issues

1. **Webhook signature verification failed**
   - Check that the webhook secret is correct
   - Ensure the webhook URL is accessible

2. **Checkout session creation fails**
   - Verify the price ID exists in Stripe
   - Check that the Stripe secret key is correct

3. **Subscription not updating**
   - Check webhook endpoint is receiving events
   - Verify database connection and migration

### Testing with Stripe CLI

Install Stripe CLI for local testing:

```bash
# Install Stripe CLI
stripe login

# Forward webhooks to local server
stripe listen --forward-to localhost:8080/api/webhooks/stripe
```

## Security Considerations

1. **Never expose secret keys** in client-side code
2. **Always verify webhook signatures** to prevent replay attacks
3. **Use HTTPS** in production
4. **Implement proper error handling** for failed payments
5. **Log all payment events** for debugging and compliance

## Support

For Stripe-specific issues, refer to:
- [Stripe Documentation](https://stripe.com/docs)
- [Stripe Support](https://support.stripe.com)
- [Stripe API Reference](https://stripe.com/docs/api)
