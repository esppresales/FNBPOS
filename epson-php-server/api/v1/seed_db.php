<?php
include('db_helpers.php');

$database = get_database();

$create_orders_table_statement =
  "CREATE TABLE IF NOT EXISTS orders (
id INTEGER PRIMARY KEY AUTOINCREMENT,
table_uid TEXT,
receipt_uid TEXT,
order_at DATETIME,
order_mode TEXT
)";
$database->query($create_orders_table_statement);

$create_order_items_table_statement =
  "CREATE TABLE IF NOT EXISTS order_items (
order_id INTEGER,
product_uid TEXT,
quantity_ordered INTEGER,
quantity_served INTEGER,
remark TEXT
)";
$database->query($create_order_items_table_statement);

$create_products_table_statement =
  "CREATE TABLE IF NOT EXISTS products (
uid TEXT PRIMARY KEY,
name TEXT,
description TEXT,
price REAL,
disabled INTEGER
)";
$database->query($create_products_table_statement);

$create_categories_table_statement =
  "CREATE TABLE IF NOT EXISTS categories (
uid TEXT PRIMARY KEY,
name TEXT,
priority INTEGER,
disabled INTEGER
)";
$database->query($create_categories_table_statement);

$create_product_categories_table_statement =
  "CREATE TABLE IF NOT EXISTS product_categories (
product_uid TEXT,
category_uid TEXT
)";
$database->query($create_product_categories_table_statement);

$create_printers_table_statement =
  "CREATE TABLE IF NOT EXISTS printers (
uid TEXT PRIMARY KEY,
name TEXT,
priority INTEGER,
disabled INTEGER
)";
$database->query($create_printers_table_statement);

$create_category_printers_table_statement =
  "CREATE TABLE IF NOT EXISTS category_printers (
category_uid TEXT,
printer_uid TEXT
)";
$database->query($create_category_printers_table_statement);

$create_tables_table_statement =
  "CREATE TABLE IF NOT EXISTS tables (
uid TEXT PRIMARY KEY,
name TEXT,
status TEXT,
disabled INTEGER
)";
$database->query($create_tables_table_statement);

$create_receipts_table_statement =
  "CREATE TABLE IF NOT EXISTS receipts (
uid TEXT PRIMARY KEY,
paid_amount REAL,
paid_at DATETIME,
discount_amount REAL,
discount_description TEXT
)";
$database->query($create_receipts_table_statement);

header('Content-Type: application/json');
$response = [
  "status" => "1",
  "message" => "successful"
];
echo json_encode($response);

?>
