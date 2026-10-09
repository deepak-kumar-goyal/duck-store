CREATE TABLE duck (
    id       INT            NOT NULL AUTO_INCREMENT,
    color    VARCHAR(10)    NOT NULL,
    size     VARCHAR(10)    NOT NULL,
    price    DECIMAL(10, 2) NOT NULL,
    quantity INT            NOT NULL,
    deleted  BOOLEAN        NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    -- Enforces the merge invariant at the database level, even for concurrent adds.
    CONSTRAINT uk_duck_color_size_price UNIQUE (color, size, price),
    CONSTRAINT ck_duck_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_duck_price CHECK (price > 0)
);
