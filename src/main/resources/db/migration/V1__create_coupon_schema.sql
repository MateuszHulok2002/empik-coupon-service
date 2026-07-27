CREATE TABLE coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    max_uses INTEGER NOT NULL,
    current_uses INTEGER NOT NULL DEFAULT 0,
    country_code VARCHAR(2) NOT NULL,

    CONSTRAINT ck_coupons_code_canonical
        CHECK (
            code = upper(btrim(code))
            AND char_length(code) BETWEEN 1 AND 64
            AND code !~ '[[:cntrl:]]'
        ),
    CONSTRAINT ck_coupons_max_uses
        CHECK (max_uses > 0),
    CONSTRAINT ck_coupons_current_uses
        CHECK (current_uses >= 0 AND current_uses <= max_uses),
    CONSTRAINT ck_coupons_country_code
        CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT uk_coupons_code
        UNIQUE (code)
);

CREATE TABLE coupon_redemptions (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    redeemed_at TIMESTAMPTZ NOT NULL,
    resolved_country_code VARCHAR(2) NOT NULL,

    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT,
    CONSTRAINT uk_coupon_redemptions_coupon_user
        UNIQUE (coupon_id, user_id),
    CONSTRAINT ck_coupon_redemptions_user_id
        CHECK (
            user_id = btrim(user_id)
            AND char_length(user_id) BETWEEN 1 AND 128
            AND user_id !~ '[[:cntrl:]]'
        ),
    CONSTRAINT ck_coupon_redemptions_country
        CHECK (resolved_country_code ~ '^[A-Z]{2}$')
);

CREATE INDEX idx_coupon_redemptions_coupon_redeemed_at
    ON coupon_redemptions (coupon_id, redeemed_at);
