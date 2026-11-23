-- Apply the 54 downloaded catalogue images to an existing MyPetStore database.
-- Safe to run repeatedly: only the 54 ON_SALE teaching products are changed.
START TRANSACTION;

UPDATE product
SET cover_image=CONCAT('images/catalog/', sku, '.jpg')
WHERE status='ON_SALE';

-- 后台练习用的下架加热垫也已有独立商品图，避免购物车历史条目回退占位图。
UPDATE product
SET cover_image='images/catalog/REP-HEAT-26.jpg'
WHERE sku='REP-HEAT-26';

UPDATE product_image pi
JOIN product p ON p.id=pi.product_id
SET pi.image_path=p.cover_image,
    pi.alt_text=CONCAT(p.name, '·商品图')
WHERE p.status='ON_SALE';

COMMIT;

SELECT COUNT(*) AS updated_on_sale_products
FROM product
WHERE status='ON_SALE'
  AND cover_image=CONCAT('images/catalog/', sku, '.jpg');
