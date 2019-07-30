<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/table.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();
  $uid = $json->{"uid"};
  $name = $json->{"name"};
  $status = $json->{"status"};
  $disabled = $json->{"disabled"};

  $tables = Table::create_table($database, $uid, $name, $status, $disabled);
  if ($tables != false) {
    $response = ["tables" => $tables];
    success_response($response);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
  Table::delete_all_tables($database);
  header('Content-Type: application/json');
  echo json_encode(["status" => 1], JSON_UNESCAPED_SLASHES);
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $tables = Table::get_all_tables($database);

  header('Content-Type: application/json');
  echo json_encode($tables, JSON_UNESCAPED_SLASHES);
  //$response = ["tables" => $tables];
  //success_response($response);
}
else {
  invalid_response();
}

?>
