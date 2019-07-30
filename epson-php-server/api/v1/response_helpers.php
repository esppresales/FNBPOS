<?php

function success_response($response) {
  header('Content-Type: application/json');
  $response = array_merge($response, ["status" => 1, "message" => "successful"]);
  echo json_encode($response, JSON_UNESCAPED_SLASHES);
}

function invalid_response() {
  header('Content-Type: application/json');
  $response = ["status" => "0", "message" => "invalid request!", "request" => $_REQUEST];
  echo json_encode($response, JSON_UNESCAPED_SLASHES);
}

?>
