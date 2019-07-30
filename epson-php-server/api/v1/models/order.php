<?php
include_once('db_helpers.php');
include_once('order_item.php');

class Order {
  public static function get_all_orders($database) {
    $orders = DB::get_all_records($database, ORDERS_TABLE_NAME);
    $new_orders = [];
    foreach ($orders as $order) {
      $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
      $order["order_items"] = $order_items;
      array_push($new_orders, $order);
    }
    return $new_orders;
  }

  public static function get_orders_by_id($database, $id) {
    $orders = DB::get_records($database, ORDERS_TABLE_NAME, ["id" => $id]);

    $new_orders = [];
    foreach ($orders as $order) {
      $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
      $order["order_items"] = $order_items;
      array_push($new_orders, $order);
    }
    return $new_orders;
  }

  public static function get_open_orders($database) {
    $orders = DB::get_records_where($database, ORDERS_TABLE_NAME, "receipt_id IS NULL");

    $new_orders = [];
    foreach ($orders as $order) {
      $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
      $order["order_items"] = $order_items;
      array_push($new_orders, $order);
    }
    return $new_orders;
  }

  public static function get_closed_orders($database) {
    $orders = DB::get_records_where($database, ORDERS_TABLE_NAME, "receipt_id IS NOT NULL");

    $new_orders = [];
    foreach ($orders as $order) {
      $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
      $order["order_items"] = $order_items;
      array_push($new_orders, $order);
    }
    return $new_orders;
  }

  public static function get_undelivered_orders($database) {
    $undelivered_order_items = [];
    foreach (OrderItem::get_undelivered_order_items($database) as $undelivered_order_item) {
      if (!array_key_exists($undelivered_order_item["order_id"], $undelivered_order_items)) {
        $undelivered_order_items[$undelivered_order_item["order_id"]] = [];
      }

      array_push($undelivered_order_items[$undelivered_order_item["order_id"]], $undelivered_order_item);
    }

    $uniq_undelivered_order_ids = array_keys($undelivered_order_items);

    $new_orders = [];

    if (count($uniq_undelivered_order_ids) > 0) {
      $orders = DB::get_records_where($database, ORDERS_TABLE_NAME, "receipt_id IS NULL AND id in (" . join(",", $uniq_undelivered_order_ids) . ")");
      foreach ($orders as $order) {
        $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
        $order["order_items"] = $order_items;
        array_push($new_orders, $order);
      }
    }
    return $new_orders;
  }

  public static function get_unpaid_orders($database) {
    $orders = DB::get_records_where($database, ORDERS_TABLE_NAME, "receipt_id IS NULL");

    $new_orders = [];
    foreach ($orders as $order) {
      $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
      $order["order_items"] = $order_items;

      $has_undelivered_order_items = false;
      foreach ($order_items as $order_item) {
        if ($order_item["quantity_ordered"] != $order_item["quantity_served"]) {
          $has_undelivered_order_items = true;
        }
      }

      if (!$has_undelivered_order_items) {
        array_push($new_orders, $order);
      }
    }
    return $new_orders;
  }

  public static function get_orders_by_time_range($database, $start_time, $end_time) {
    error_log("order_at >= '" . $start_time . "' AND order_at <= '" . $end_time . "'");
    $orders = DB::get_records_where($database, ORDERS_TABLE_NAME, "order_at >= '" . $start_time . "' AND order_at <= '" . $end_time . "'");

    $new_orders = [];
    foreach ($orders as $order) {
      $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
      $order["order_items"] = $order_items;
      array_push($new_orders, $order);
    }
    return $new_orders;
  }

  public static function create_order($database, $table_uid, $order_at, $ordering_mode, $order_items) {
    $fields = [
      "table_uid" => $table_uid,
      //"receipt_uid" => $receipt_uid,
      "order_at" => $order_at,
      "ordering_mode" => $ordering_mode
    ];

    if (DB::create_new_record($database, ORDERS_TABLE_NAME, $fields)) {
      $order_id = $database->lastInsertRowid();

      $order_items_create_success = true;

      error_log("order_id " . $order_id);
      error_log("order_item " . var_export($order_items, true));

      foreach ($order_items as $order_item) {
          error_log("order_id " . $order_id);
        $product_uid = $order_item->{"product_uid"};
        $quantity_ordered = $order_item->{"quantity_ordered"};
        $quantity_served = $order_item->{"quantity_served"};
        $remark = $order_item->{"remark"};
        if (OrderItem::create_order_item($database, $order_id, $product_uid, $quantity_ordered, $quantity_served, $remark) == false) {
          $order_items_create_success = false;
        }
      }

      return Order::get_orders_by_id($database, $order_id);
    }

    return false;
  }

  public static function get_orders_for_receipt_id($database, $receipt_id) {
    $orders = DB::get_records($database, ORDERS_TABLE_NAME, ["receipt_id" => $receipt_id]);

    $new_orders = [];
    foreach ($orders as $order) {
      $order_items = OrderItem::get_order_items_by_order_id($database, $order["id"]);
      $order["order_items"] = $order_items;
      array_push($new_orders, $order);
    }
    return $new_orders;
  }

  public static function update_receipt_id_for_order_id($database, $receipt_id, $order_id) {
    $whereFields = [
      "id" => $order_id
    ];
    $updateFields = [
      "receipt_id" => $receipt_id
    ];

    return DB::update_record($database, ORDERS_TABLE_NAME, $whereFields, $updateFields);
  }
}

?>
