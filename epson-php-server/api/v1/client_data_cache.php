<?php
include('db_helpers.php');
include('models/product.php');
include('models/category.php');
include('models/printer.php');
include('models/table.php');

$database = get_database();

$products = Product::get_all_categories($database);

$categories = Category::get_all_categories($database);

$printers = Printer::get_all_printers($database);

$tables = Table::get_all_tables($database);

$response = [
  "products" => $products,
  "categories" => $categories,
  "printers" => $printers,
  "tables" => $tables
];
success_response($response);

?>
