<?php

date_default_timezone_set("Singapore");

define('SQLITE_FILENAME', '../../db/epson.sqlite3');
define('PRODUCT_IMAGE_FOLDER', '../../db/product_images/');

// table names
define('CATEGORIES_TABLE_NAME', 'categories');
define('PRODUCT_CATEGORIES_TABLE_NAME', 'category_products');
define('CATEGORY_PRINTERS_TABLE_NAME', 'category_printers');
define('ORDERS_TABLE_NAME', 'orders');
define('ORDER_ITEMS_TABLE_NAME', 'order_items');
define('PRINTERS_TABLE_NAME', 'printers');
define('PRODUCTS_TABLE_NAME', 'products');
define('TABLES_TABLE_NAME', 'tables');
define('RECEIPTS_TABLE_NAME', 'receipts');
define('RECEIPT_HEADERS_TABLE_NAME', 'receipt_headers');
define('SURCHARGES_TABLE_NAME', 'surcharges');

?>
