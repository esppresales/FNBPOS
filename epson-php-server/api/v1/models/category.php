<?php
include_once('db_helpers.php');

class Category {
  public static function get_all_categories($database) {
    return DB::get_all_records($database, CATEGORIES_TABLE_NAME);
  }

  public static function get_categories_by_uid($database, $uid) {
    $categories = DB::get_records($database, CATEGORIES_TABLE_NAME, ["uid" => $uid]);

    foreach ($categories as $category) {
      $product_uids = [];
      $product_categories = DB::get_records($database, PRODUCT_CATEGORIES_TABLE_NAME, ["category_uid" => $category["uid"]]);
      foreach ($product_categories as $product_category) {
        array_push($product_uids, $product_category["product_uid"]);
      }
      $category["product_uids"] = $product_uids;

      $printer_uids = [];
      $category_printers = DB::get_records($database, CATEGORY_PRINTERS_TABLE_NAME, ["category_uid" => $category["uid"]]);
      foreach ($category_printers as $category_printer) {
        array_push($printer_uids, $category_printer["printer_uid"]);
      }

      $category["printer_uids"] = $printer_uids;
    }

    return $categories;
  }

  public static function create_category($database, $uid, $name, $priority, $disabled) {
    $fields = [
      "uid" => $uid,
      "name" => $name,
      "priority" => $priority,
      "disabled" => $disabled
    ];

    if (DB::create_new_record($database, CATEGORIES_TABLE_NAME, $fields)) {
      return Category::get_categories_by_uid($database, $uid);
    }
    return false;
  }

  public static function delete_all_categories($database) {
    return DB::delete_all_records($database, CATEGORIES_TABLE_NAME);
  }

  public static function update_category_products($database, $category_uid, $product_uids) {
    DB::delete_records($database, PRODUCT_CATEGORIES_TABLE_NAME, ["category_uid" => $category_uid]);
    foreach ($product_uids as $product_uid) {
      DB::create_new_record($database, PRODUCT_CATEGORIES_TABLE_NAME, ["category_uid" => $category_uid, "product_uid" => $product_uid]);
    }
  }

  public static function update_category_printers($database, $category_uid, $printer_uids) {
    DB::delete_records($database, CATEGORY_PRINTERS_TABLE_NAME, ["category_uid" => $category_uid]);
    foreach ($printer_uids as $printer_uid) {
      DB::create_new_record($database, CATEGORY_PRINTERS_TABLE_NAME, ["category_uid" => $category_uid, "printer_uid" => $printer_uid]);
    }
  }
}

?>
