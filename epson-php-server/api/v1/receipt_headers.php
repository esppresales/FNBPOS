<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/receipt_header.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();
  $content = $json->{"content"};
  $priority = $json->{"priority"};

  $receipt_headers = ReceiptHeader::create_receipt_header($database, $content, $priority);
  if ($receipt_headers != false) {
    $receipt_headers = ReceiptHeader::get_all_receipt_headers($database);

    header('Content-Type: application/json');
    echo json_encode($receipt_headers, JSON_UNESCAPED_SLASHES);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
  ReceiptHeader::delete_all_receipt_headers($database);
  header('Content-Type: application/json');
  echo json_encode(["status" => 1], JSON_UNESCAPED_SLASHES);
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $receipt_headers = ReceiptHeader::get_all_receipt_headers($database);

  header('Content-Type: application/json');
  echo json_encode($receipt_headers, JSON_UNESCAPED_SLASHES);
}
else {
  invalid_response();
}

?>
