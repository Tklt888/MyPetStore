-- Apply once to the shared MyPetStore database before deploying both apps.
ALTER TABLE product ADD COLUMN archived_at DATETIME NULL DEFAULT NULL;
ALTER TABLE sales_order ADD COLUMN archived_at DATETIME NULL DEFAULT NULL;
