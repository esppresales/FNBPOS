<?php
include_once('db_helpers.php');
include_once('order.php');

class Receipt {
  public static function get_all_receipts($database) {
    $receipts = DB::get_all_records($database, RECEIPTS_TABLE_NAME);
    $new_receipts = [];
    foreach ($receipts as $receipt) {
      $receipt_id = $receipt["id"];
      $receipt["orders"] = Order::get_orders_for_receipt_id($database, $receipt_id);
      array_push($new_receipts, $receipt);
    }
    return $new_receipts;
  }

  public static function get_receipts_by_id($database, $id) {
    return DB::get_records($database, RECEIPTS_TABLE_NAME, ["id" => $id]);
  }

  public static function create_receipt($database, $paid_amount, $paid_at, $discount_amount, $discount_description, $final_amount) {
    $fields = [
      "paid_amount" => $paid_amount,
      "paid_at" => $paid_at,
      "discount_amount" => $discount_amount,
      "discount_description" => $discount_description,
      "final_amount" => $final_amount
    ];

    if (DB::create_new_record($database, RECEIPTS_TABLE_NAME, $fields)) {
      $receipt_id = $database->lastInsertRowid();
      return Receipt::get_receipts_by_id($database, $receipt_id);
    }

    return false;
  }

  public static function create_receipt_for_order_ids($database, $paid_amount, $paid_at, $discount_amount, $discount_description, $final_amount, $order_ids) {
    $receipts = Receipt::create_receipt($database, $paid_amount, $paid_at, $discount_amount, $discount_description, $final_amount);
    if (count($receipts) > 0) {
      $receipt = $receipts[0];
      $receipt_id = $receipt["id"];
      foreach ($order_ids as $order_id) {
        Order::update_receipt_id_for_order_id($database, $receipt_id, $order_id);
      }
    }

    $new_receipts = [];
    foreach ($receipts as $receipt) {
      $receipt_id = $receipt["id"];
      $receipt["orders"] = Order::get_orders_for_receipt_id($database, $receipt_id);
      array_push($new_receipts, $receipt);
    }
    return $new_receipts;
  }

  public static function get_receipts_by_time_range($database, $start_time, $end_time) {
    $receipts = DB::get_records_where($database, RECEIPTS_TABLE_NAME, "paid_at >= '" . $start_time . "' AND paid_at <= '" . $end_time . "'");

    $new_receipts = [];
    foreach ($receipts as $receipt) {
      $receipt_id = $receipt["id"];
      $receipt["orders"] = Order::get_orders_for_receipt_id($database, $receipt_id);
      array_push($new_receipts, $receipt);
    }

    return $new_receipts;
  }
}

?>
