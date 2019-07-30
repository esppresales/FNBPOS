<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('file_helpers.php');
include_once('models/product.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  //error_log(file_get_contents('php://input'));
  $product = json_decode($_POST['product']);
  //error_log('$_FILES = ' . json_encode($_FILES, JSON_UNESCAPED_SLASHES) . ', $_POST["product"] = ' . json_encode($product, JSON_UNESCAPED_SLASHES));

  if (empty($_FILES['image'])) {
    invalid_response();
  }
  else {
    $image_file = $_FILES['image'];

    $uid = $product->{"uid"};
    $name = $product->{"name"};
    //$description = implode("\n",$product->{"descriptions"});
    $description = "description";
    $price = $product->{"price"};
    $disabled = $product->{"disabled"};

    $products = Product::create_product($database, $uid, $name, $description, $price, $disabled);
    if ($products != false) {
      $target_path = PRODUCT_IMAGE_FOLDER . $uid . "/1.jpg";
      copy_uploaded_file($image_file, $target_path, function() use ($products) {
        $response = ["products" => $products];
        success_response($response);
      }, function() {
        invalid_response();
      });
    }
    else {
      invalid_response();
    }
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
  Product::delete_all_products($database);
  header('Content-Type: application/json');
  echo json_encode(["status" => 1], JSON_UNESCAPED_SLASHES);
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $products = Product::get_all_products($database);

  header('Content-Type: application/json');
  echo json_encode($products, JSON_UNESCAPED_SLASHES);
  //$response = ["products" => $products];
  //success_response($response);
}
else {
  invalid_response();
}

?>
