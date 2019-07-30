<?php
include_once('db_helpers.php');

class Table {
  public static function get_all_tables($database) {
    return DB::get_all_records($database, TABLES_TABLE_NAME);
  }

  public static function get_tables_by_uid($database, $uid) {
    return DB::get_records($database, TABLES_TABLE_NAME, ["uid" => $uid]);
  }

  public static function create_table($database, $uid, $name, $status, $disabled) {
    $fields = [
      "uid" => $uid,
      "name" => $name,
      "status" => $status,
      "disabled" => $disabled
    ];

    if (DB::create_new_record($database, TABLES_TABLE_NAME, $fields)) {
      return Table::get_tables_by_uid($database, $uid);
    }

    return false;
  }

  public static function delete_all_tables($database) {
    return DB::delete_all_records($database, TABLES_TABLE_NAME);
  }
}

?>
