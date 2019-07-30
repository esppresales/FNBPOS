<?php
include_once('../../conf/constant.php');
include_once('db_helpers.php');
include_once('request_helpers.php');
include_once('response_helpers.php');
include_once('models/surcharge.php');

$database = DB::get_database();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
  $json = get_json_request_body();
  $name = $json->{"name"};
  $percentage = $json->{"percentage"};
  $priority = $json->{"priority"};

  $surcharges = Surcharge::create_surcharge($database, $name, $percentage, $priority);
  if ($surcharges != false) {
    $surcharges = Surcharge::get_all_surcharges($database);

    header('Content-Type: application/json');
    echo json_encode($surcharges, JSON_UNESCAPED_SLASHES);
  }
  else {
    invalid_response();
  }
}
elseif ($_SERVER['REQUEST_METHOD'] === 'DELETE') {
  Surcharge::delete_all_surcharges($database);
  header('Content-Type: application/json');
  echo json_encode(["status" => 1], JSON_UNESCAPED_SLASHES);
}
elseif ($_SERVER['REQUEST_METHOD'] === 'GET') {
  $surcharges = Surcharge::get_all_surcharges($database);

  header('Content-Type: application/json');
  echo json_encode($surcharges, JSON_UNESCAPED_SLASHES);
}
else {
  invalid_response();
}

?>
