-- Add Stripe fields to subscriptions table
ALTER TABLE subscriptions 
ADD COLUMN stripe_subscription_id VARCHAR(255) UNIQUE,
ADD COLUMN stripe_customer_id VARCHAR(255),
ADD COLUMN stripe_price_id VARCHAR(255),
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add indexes for better performance
CREATE INDEX idx_subscriptions_stripe_customer_id ON subscriptions(stripe_customer_id);
CREATE INDEX idx_subscriptions_stripe_subscription_id ON subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);
