<?php
include_once('db_helpers.php');

class Printer {
  public static function get_all_printers($database) {
    $printers = DB::get_all_records($database, PRINTERS_TABLE_NAME);
    $new_printers = [];
    foreach ($printers as $printer) {
      $printer_uid = $printer["uid"];
      $printer["category_uids"] = Printer::get_category_uids_by_uid($database, $printer_uid);
      array_push($new_printers, $printer);
    }
    return $new_printers;
  }

  public static function get_printers_by_uid($database, $uid) {
    $printers = DB::get_records($database, PRINTERS_TABLE_NAME, ["uid" => $uid]);
    $new_printers = [];
    foreach ($printers as $printer) {
      $printer_uid = $printer["uid"];
      $printer["category_uids"] = Printer::get_category_uids_by_uid($database, $printer_uid);
      array_push($new_printers, $printer);
    }
    return $new_printers;
  }

  public static function create_printer($database, $uid, $name, $priority, $disabled) {
    $fields = [
      "uid" => $uid,
      "name" => $name,
      "priority" => $priority
    ];

    if (DB::create_new_record($database, PRINTERS_TABLE_NAME, $fields)) {
      return Printer::get_printers_by_uid($database, $uid);
    }

    return false;
  }

  public static function delete_all_printers($database) {
    return DB::delete_all_records($database, PRINTERS_TABLE_NAME);
  }

  public static function get_category_uids_by_uid($database, $uid) {
    $category_printers = DB::get_records($database, CATEGORY_PRINTERS_TABLE_NAME, ["printer_uid" => $uid]);
    $category_uids = [];
    foreach($category_printers as $category_printer) {
      array_push($category_uids, $category_printer["category_uid"]);
    }
    return $category_uids;
  }
}

?>
