<?php
function get_json_request_body() {
  return json_decode(file_get_contents('php://input'));
}
?>
