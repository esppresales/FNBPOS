<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/category.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();
  $uid = $json->{"uid"};
  $name = $json->{"name"};
  $priority = $json->{"priority"};
  $disabled = $json->{"disabled"};
  Category::create_category($database, $uid, $name, $priority, $disabled);

  if (isset($json->{'product_categories'})) {
    $product_categories = $json->{"product_categories"};
	Category::update_category_products($database, $name, $product_categories);
  }
  if (isset($json->{'category_printers'})) {
    $category_printers = $json->{"category_printers"};
	Category::update_category_printers($database, $name, $category_printers);
  }

  $categories = Category::get_categories_by_uid($database, $name);
  if ($categories != false) {
    $response = ["categories" => $categories];
    success_response($response);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
  Category::delete_all_categories($database);
  header('Content-Type: application/json');
  echo json_encode(["status" => 1], JSON_UNESCAPED_SLASHES);
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $categories = Category::get_all_categories($database);

  header('Content-Type: application/json');
  echo json_encode($categories, JSON_UNESCAPED_SLASHES);
  //$response = ["categories" => $categories];
  //success_response($response);
}
else {
  invalid_response();
}

?>
