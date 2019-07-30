<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/printer.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();
  $uid = $json->{"uid"};
  $name = $json->{"name"};
  $priority = $json->{"priority"};
  $disabled = $json->{"disabled"};

  $printers = Printer::create_printer($database, $uid, $name, $priority, $disabled);
  if ($printers != false) {
    $response = ["printers" => $printers];
    success_response($response);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
  Printer::delete_all_printers($database);
  header('Content-Type: application/json');
  echo json_encode(["status" => 1], JSON_UNESCAPED_SLASHES);
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $printers = Printer::get_all_printers($database);

  header('Content-Type: application/json');
  echo json_encode($printers, JSON_UNESCAPED_SLASHES);
  //$response = ["printers" => $printers];
  //success_response($response);
}
else {
  invalid_response();
}

?>
