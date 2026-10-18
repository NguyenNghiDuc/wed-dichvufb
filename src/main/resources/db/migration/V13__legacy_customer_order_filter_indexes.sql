-- Owner-scoped indexes for legacy customer order pagination and exact service/date filters.
CREATE INDEX idx_orders_customer_owner_created
    ON orders(owner_username, created_at DESC, id DESC);
CREATE INDEX idx_orders_customer_owner_service_created
    ON orders(owner_username, server_name, created_at DESC, id DESC);
