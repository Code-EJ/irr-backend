CREATE TABLE donations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_name VARCHAR(255) NOT NULL,
    donor_document VARCHAR(14) NOT NULL,
    donor_type VARCHAR(2) NOT NULL,
    donor_address VARCHAR(255) NOT NULL,
    material_type_id BIGINT NOT NULL,
    material_subtype_id BIGINT NOT NULL,
    material_sub_subtype_id BIGINT NOT NULL,
    weight DOUBLE NOT NULL
);
