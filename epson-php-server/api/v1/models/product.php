<?php
include_once('db_helpers.php');

class Product {
  public static function get_all_products($database) {
    $products = DB::get_all_records($database, PRODUCTS_TABLE_NAME);
	  $new_products = [];
    foreach ($products as $product) {
  	  $product_uid = $product["uid"];
  	  $product_images = Product::get_product_images_by_uid($product_uid);
  	  $product["images"] = $product_images;
  	  $product_descriptions = explode("\\n", $product["description"]);
  	  unset($product["description"]);
  	  $product["descriptions"] = $product_descriptions;
      $product["category_uids"] = Product::get_category_uids_by_uid($database, $product_uid);
  	  array_push($new_products, $product);
  	}
  	return $new_products;
  }

  public static function get_products_by_uid($database, $uid) {
    $products = DB::get_records($database, PRODUCTS_TABLE_NAME, ["uid" => $uid]);
    $new_products = [];
    foreach ($products as $product) {
      $product_uid = $product["uid"];
      $product_images = Product::get_product_images_by_uid($product_uid);
      $product["images"] = $product_images;
      $product_descriptions = explode("\\n", $product["description"]);
      unset($product["description"]);
      $product["descriptions"] = $product_descriptions;
      $product["category_uids"] = Product::get_category_uids_by_uid($database, $product_uid);
      array_push($new_products, $product);
    }
    return $new_products;
  }

  public static function create_product($database, $uid, $name, $description, $price, $disabled) {
    $fields = [
      "uid" => $uid,
      "name" => $name,
      "description" => $description,
      "price" => $price,
      "disabled" => $disabled
    ];

    if (DB::create_new_record($database, PRODUCTS_TABLE_NAME, $fields)) {
      return Product::get_products_by_uid($database, $uid);
    }

    return false;
  }

  public static function delete_all_products($database) {
    return DB::delete_all_records($database, PRODUCTS_TABLE_NAME);
  }

  public static function get_product_images_by_uid($uid) {
    return ['/db/product_images/' . $uid . '/1.jpg'];
  }

  public static function get_category_uids_by_uid($database, $uid) {
    $product_categories = DB::get_records($database, PRODUCT_CATEGORIES_TABLE_NAME, ["product_uid" => $uid]);
    $category_uids = [];
    foreach($product_categories as $product_category) {
      array_push($category_uids, $product_category["category_uid"]);
    }
    return $category_uids;
  }
}

?>
