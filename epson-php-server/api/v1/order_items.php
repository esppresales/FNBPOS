<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/order_item.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();

  $order_id = $json->{"order_id"};
  $product_uid = $json->{"product_uid"};
  $quantity_ordered = $json->{"quantity_ordered"};
  $quantity_served = $json->{"quantity_served"};
  $remark = $json->{"remark"};

  $order_items = OrderItem::create_order_item($database, $order_id, $product_uid, $quantity_ordered, $quantity_served, $remark);
  if ($order_items != false) {
    $response = ["order_items" => $order_items];
    success_response($response);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $order_items = [];
  if(isset($_GET['order_id'])) {
    $order_items = OrderItem::get_order_items_by_order_id($database, $_GET['order_id']);
  }
  else {
    $order_items = OrderItem::get_all_order_items($database);
  }

  header('Content-Type: application/json');
  echo json_encode($order_items, JSON_UNESCAPED_SLASHES);
}
elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
  if(isset($_GET['order_id']) && isset($_GET['product_uid'])) {
    $order_id = $_GET['order_id'];
    $product_uid = $_GET['product_uid'];

    $order_items = OrderItem::delete_order_item_for_order_id_and_product_uid($database, $order_id, $product_uid);
    header('Content-Type: application/json');
    echo json_encode($order_items, JSON_UNESCAPED_SLASHES);
  }
  else {
    invalid_response();
  }
}

elseif ($_SERVER['REQUEST_METHOD'] === 'PATCH') {
  $json = get_json_request_body();

  $order_id = $json->{"order_id"};
  $product_uid = $json->{"product_uid"};
  $quantity_ordered = $json->{"quantity_ordered"};
  $quantity_served = $json->{"quantity_served"};
  $remark = $json->{"remark"};

  $order_items = OrderItem::get_order_items_by_order_id_and_product_uid($database, $order_id, $product_uid);

  if ($order_items != false && count($order_items) > 0) {
      $result_order_items = OrderItem::update_order_item_for_order_id_and_product_uid($database, $order_id, $product_uid, $quantity_ordered, $quantity_served, $remark);
    if ($result_order_items) {
        header('Content-Type: application/json');
        echo json_encode($result_order_items, JSON_UNESCAPED_SLASHES);
    }
    else {
        invalid_response();
    }
  }
  else {
    invalid_response();
  }
}
else {
  invalid_response();
}

?>
