<?php
include_once('db_helpers.php');

class ReceiptHeader {
  public static function get_all_receipt_headers($database) {
    return DB::get_all_records($database, RECEIPT_HEADERS_TABLE_NAME);
  }

  public static function create_receipt_header($database, $content, $priority) {
    $fields = [
      "content" => $content,
      "priority" => $priority
    ];

    if (DB::create_new_record($database, RECEIPT_HEADERS_TABLE_NAME, $fields)) {
      $receipt_header_id = $database->lastInsertRowid();
      return ReceiptHeader::get_all_receipt_headers($database);
    }

    return false;
  }

  public static function delete_all_receipt_headers($database) {
    return DB::delete_all_records($database, RECEIPT_HEADERS_TABLE_NAME);
  }
}

?>
