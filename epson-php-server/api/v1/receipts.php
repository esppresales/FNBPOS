<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/receipt.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();
  $paid_amount = $json->{"paid_amount"};
  $paid_at = $json->{"paid_at"};
  $discount_amount = $json->{"discount_amount"};
  $discount_description = $json->{"discount_description"};
  $final_amount = $json->{"final_amount"};
  $order_ids = $json->{"order_ids"};

  $receipts = Receipt::create_receipt_for_order_ids($database, $paid_amount, $paid_at, $discount_amount, $discount_description, $final_amount, $order_ids);
  if ($receipts != false) {
    $response = ["receipts" => $receipts];
    success_response($response);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $receipts = Receipt::get_all_receipts($database);

  header('Content-Type: application/json');
  echo json_encode($receipts, JSON_UNESCAPED_SLASHES);
  //$response = ["receipts" => $receipts];
  //success_response($response);
}
else {
  invalid_response();
}

?>
