<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/order.php');
include_once('models/order_item.php');
include_once('models/receipt.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();
  $tableUid = $json->{"table_uid"};
  $orderAt = $json->{"order_at"};
  $orderingMode = $json->{"ordering_mode"};
  $orderItems = $json->{"order_items"};

  $orders = Order::create_order($database, $tableUid, $orderAt, $orderingMode, $orderItems);
  if ($orders != false) {
    $response = ["status" => 1, "message" => "successful", "orders" => $orders];
    echo json_encode($response, JSON_UNESCAPED_SLASHES);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $orders = [];
  if(isset($_GET['state'])) {
    if ($_GET['state'] === 'open') {
      $orders = Order::get_open_orders($database);
    }
    elseif ($_GET['state'] === 'closed') {
      $orders = Order::get_closed_orders($database);
    }
    elseif ($_GET['state'] === 'undelivered') {
      $orders = Order::get_undelivered_orders($database);
    }
    elseif ($_GET['state'] === 'unpaid') {
      $orders = Order::get_unpaid_orders($database);
    }
  }
  else {
    $orders = Order::get_all_orders($database);
  }

  header('Content-Type: application/json');
  echo json_encode($orders, JSON_UNESCAPED_SLASHES);
  //$response = ["orders" => $orders];
  //success_response($response);
}
else {
  invalid_response();
}

?>
