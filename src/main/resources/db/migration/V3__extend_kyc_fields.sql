alter table kyc_requests
    add column if not exists occupation varchar(255),
    add column if not exists monthly_income numeric(18,2),
    add column if not exists citizen_front_image_url text,
    add column if not exists citizen_back_image_url text,
    add column if not exists portrait_image_url text;
