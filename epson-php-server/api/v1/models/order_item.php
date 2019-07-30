<?php
include_once('db_helpers.php');

class OrderItem {
  public static function get_all_order_items($database) {
    return DB::get_all_records($database, ORDER_ITEMS_TABLE_NAME);
  }

  public static function get_order_items_by_order_id($database, $order_id) {
    return DB::get_records($database, ORDER_ITEMS_TABLE_NAME, ["order_id" => $order_id]);
  }

  public static function get_order_items_by_order_id_and_product_uid($database, $order_id, $product_uid) {
    return DB::get_records($database, ORDER_ITEMS_TABLE_NAME, ["order_id" => $order_id, "product_uid" => $product_uid]);
  }

  public static function get_undelivered_order_items($database) {
    return DB::get_records_where($database, ORDER_ITEMS_TABLE_NAME, "quantity_ordered <> quantity_served");
  }

  public static function create_order_item($database, $order_id, $product_uid, $quantity_ordered, $quantity_served, $remark) {
    $fields = [
      "order_id" => $order_id,
      "product_uid" => $product_uid,
      "quantity_ordered" => $quantity_ordered,
      "quantity_served" => $quantity_served,
      "remark" => $remark
    ];

    if (DB::create_new_record($database, ORDER_ITEMS_TABLE_NAME, $fields)) {
      return OrderItem::get_order_items_by_order_id($database, $order_id);
    }

    return false;
  }

  public static function update_order_item_for_order_id_and_product_uid($database, $order_id, $product_uid, $quantity_ordered, $quantity_served, $remark) {
    $whereFields = ["order_id" => $order_id, "product_uid" => $product_uid];
    $updateFields = [
      "quantity_ordered" => $quantity_ordered,
      "quantity_served" => $quantity_served,
      "remark" => $remark
    ];
    if (DB::update_record($database, ORDER_ITEMS_TABLE_NAME, $whereFields, $updateFields)) {
        return OrderItem::get_order_items_by_order_id_and_product_uid($database, $order_id, $product_uid);
    }
    return false;
  }

  public static function delete_order_item_for_order_id_and_product_uid($database, $order_id, $product_uid) {
    $whereFields = ["order_id" => $order_id, "product_uid" => '"' . $product_uid . '"'];
    if (DB::delete_records($database, ORDER_ITEMS_TABLE_NAME, $whereFields)) {
        return OrderItem::get_order_items_by_order_id_and_product_uid($database, $order_id, $product_uid);
    }
    return false;
  }
}

?>
